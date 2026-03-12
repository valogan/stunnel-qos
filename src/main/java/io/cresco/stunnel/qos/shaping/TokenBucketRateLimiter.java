package io.cresco.stunnel.qos.shaping;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * High-performance token bucket rate limiter.
 * Thread-safe and designed for low contention.
 * All rates are in bytes per second.
 */
public class TokenBucketRateLimiter {
    // Configuration
    private final long capacity; // Maximum tokens (burst size in bytes)
    private final double refillRate; // Tokens per nanosecond (bytes/ns)
    private final long refillIntervalNs; // Refill interval in nanoseconds

    // State
    private final AtomicLong availableTokens;
    private final AtomicLong lastRefillTimeNs;
    private final Lock refillLock;

    // Metrics
    private final AtomicLong totalConsumed;
    private final AtomicLong totalRejected;
    private final AtomicLong totalWaitedNs;
    private final AtomicLong totalRefills;

    /**
     * Creates a token bucket rate limiter.
     * 
     * @param ratePerSecond Rate in bytes per second
     * @param burstSize     Burst size in bytes
     */
    public TokenBucketRateLimiter(long ratePerSecond, long burstSize) {
        if (ratePerSecond <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        if (burstSize <= 0) {
            throw new IllegalArgumentException("Burst size must be positive");
        }

        this.capacity = burstSize;
        this.refillRate = ratePerSecond / 1_000_000_000.0; // Bytes per nanosecond
        this.refillIntervalNs = 10_000_000L; // 10ms refill interval

        this.availableTokens = new AtomicLong(burstSize);
        this.lastRefillTimeNs = new AtomicLong(System.nanoTime());
        this.refillLock = new ReentrantLock();

        this.totalConsumed = new AtomicLong(0);
        this.totalRejected = new AtomicLong(0);
        this.totalWaitedNs = new AtomicLong(0);
        this.totalRefills = new AtomicLong(0);
    }

    /**
     * Try to consume tokens immediately.
     * Returns true if successful, false if insufficient tokens.
     * 
     * @param tokens Number of tokens (bytes) to consume
     * @return true if tokens were consumed, false otherwise
     */
    public boolean tryConsume(long tokens) {
        if (tokens <= 0) {
            return true; // Consuming zero or negative tokens always succeeds
        }

        // Fast path: try without lock if we have enough tokens
        long current = availableTokens.get();
        if (current >= tokens) {
            if (availableTokens.compareAndSet(current, current - tokens)) {
                totalConsumed.addAndGet(tokens);
                return true;
            }
        }

        // Need to refill and retry
        return tryConsumeWithRefill(tokens);
    }

    private boolean tryConsumeWithRefill(long tokens) {
        refillLock.lock();
        try {
            refill();
            long current = availableTokens.get();
            if (current >= tokens) {
                availableTokens.set(current - tokens);
                totalConsumed.addAndGet(tokens);
                return true;
            }
            totalRejected.incrementAndGet();
            return false;
        } finally {
            refillLock.unlock();
        }
    }

    /**
     * Consume tokens, waiting up to the specified time if necessary.
     * Returns the wait time in nanoseconds, or -1 if timed out.
     * 
     * @param tokens    Number of tokens (bytes) to consume
     * @param maxWaitNs Maximum time to wait in nanoseconds
     * @return Wait time in nanoseconds, or -1 if timed out
     */
    public long tryConsumeWithWait(long tokens, long maxWaitNs) {
        if (tokens <= 0) {
            return 0; // No wait needed for zero tokens
        }

        long startTime = System.nanoTime();
        long deadline = startTime + maxWaitNs;

        while (System.nanoTime() < deadline) {
            if (tryConsume(tokens)) {
                long waited = System.nanoTime() - startTime;
                totalWaitedNs.addAndGet(waited);
                return waited;
            }

            // Get accurate deficit under lock to avoid race conditions
            long deficit;
            refillLock.lock();
            try {
                refill();
                long currentTokens = availableTokens.get();
                deficit = tokens - currentTokens;
                if (deficit <= 0) {
                    // Tokens became available while we were waiting for lock
                    // Try consuming again in next iteration
                    continue;
                }
            } finally {
                refillLock.unlock();
            }

            // Calculate time needed to accumulate enough tokens
            // Guard against division by zero or extremely small refillRate
            if (refillRate <= 1e-12) { // Less than 1 byte per microsecond
                // Rate is too small to wait reasonably, reject immediately
                break;
            }

            long nsNeeded = (long) (deficit / refillRate);
            if (nsNeeded <= 0) {
                nsNeeded = 1; // At least 1 ns
            }

            // Don't wait longer than 1ms or until deadline
            long waitNs = Math.min(nsNeeded, 1_000_000L); // Max 1ms sleep
            waitNs = Math.min(waitNs, deadline - System.nanoTime());

            if (waitNs <= 0) {
                break; // No time left
            }

            try {
                TimeUnit.NANOSECONDS.sleep(waitNs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }

        totalRejected.incrementAndGet();
        return -1;
    }

    /**
     * Refill tokens based on elapsed time.
     */
    private void refill() {
        long now = System.nanoTime();
        long last = lastRefillTimeNs.get();
        long elapsed = now - last;

        if (elapsed <= 0) {
            return;
        }

        // Calculate tokens to add
        double tokensToAdd = elapsed * refillRate;
        if (tokensToAdd <= 0) {
            return;
        }

        // Update available tokens
        long current = availableTokens.get();
        long newValue = Math.min(capacity, current + (long) tokensToAdd);

        if (availableTokens.compareAndSet(current, newValue)) {
            // Successfully added tokens, update timestamp
            lastRefillTimeNs.compareAndSet(last, now);
            totalRefills.incrementAndGet();
        } else {
            // Another thread updated tokens, but we should still update timestamp
            // if our elapsed time calculation is still valid (not stale)
            // Use compareAndSet to ensure we don't move timestamp backwards
            lastRefillTimeNs.compareAndSet(last, now);
        }
    }

    /**
     * Get the current number of available tokens.
     */
    public long getAvailableTokens() {
        refillLock.lock();
        try {
            refill();
            return availableTokens.get();
        } finally {
            refillLock.unlock();
        }
    }

    /**
     * Get the total number of tokens consumed.
     */
    public long getTotalConsumed() {
        return totalConsumed.get();
    }

    /**
     * Get the total number of consumption requests rejected.
     */
    public long getTotalRejected() {
        return totalRejected.get();
    }

    /**
     * Get the total time waited for tokens in nanoseconds.
     */
    public long getTotalWaitedNs() {
        return totalWaitedNs.get();
    }

    /**
     * Get the total number of refill operations.
     */
    public long getTotalRefills() {
        return totalRefills.get();
    }

    /**
     * Get the average wait time per successful consumption in nanoseconds.
     */
    public double getAverageWaitNs() {
        long waited = totalWaitedNs.get();
        long consumed = totalConsumed.get();
        return consumed > 0 ? (double) waited / consumed : 0.0;
    }

    /**
     * Get the rejection rate (rejections per consumption attempt).
     */
    public double getRejectionRate() {
        long rejected = totalRejected.get();
        long consumed = totalConsumed.get();
        long totalAttempts = rejected + consumed;
        return totalAttempts > 0 ? (double) rejected / totalAttempts : 0.0;
    }

    /**
     * Reset all metrics counters.
     */
    public void resetMetrics() {
        totalConsumed.set(0);
        totalRejected.set(0);
        totalWaitedNs.set(0);
        totalRefills.set(0);
    }

    /**
     * Get a string representation of the rate limiter state.
     */
    @Override
    public String toString() {
        return String.format(
                "TokenBucketRateLimiter{rate=%.1f B/s, capacity=%d, available=%d, consumed=%d, rejected=%d}",
                refillRate * 1_000_000_000.0, capacity, getAvailableTokens(),
                totalConsumed.get(), totalRejected.get());
    }
}
