package io.cresco.stunnel.qos.config;

import java.util.Objects;

/**
 * Defines latency requirements for a tunnel.
 * All values are in milliseconds.
 */
public class LatencyRequirements {
    private Integer maxLatencyMs;    // Maximum one-way latency
    private Integer maxJitterMs;     // Maximum jitter (variation in latency)
    private Integer maxReorder;      // Maximum packet reordering (packets)
    
    /**
     * Default constructor with no specific requirements.
     */
    public LatencyRequirements() {
        // No requirements by default
    }
    
    /**
     * Constructor with maximum latency requirement.
     */
    public LatencyRequirements(int maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
    }
    
    /**
     * Constructor with latency and jitter requirements.
     */
    public LatencyRequirements(int maxLatencyMs, int maxJitterMs) {
        this.maxLatencyMs = maxLatencyMs;
        this.maxJitterMs = maxJitterMs;
    }
    
    public Integer getMaxLatencyMs() {
        return maxLatencyMs;
    }
    
    public LatencyRequirements setMaxLatencyMs(Integer maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
        return this;
    }
    
    public Integer getMaxJitterMs() {
        return maxJitterMs;
    }
    
    public LatencyRequirements setMaxJitterMs(Integer maxJitterMs) {
        this.maxJitterMs = maxJitterMs;
        return this;
    }
    
    public Integer getMaxReorder() {
        return maxReorder;
    }
    
    public LatencyRequirements setMaxReorder(Integer maxReorder) {
        this.maxReorder = maxReorder;
        return this;
    }
    
    /**
     * Validates the latency requirements.
     * @return true if the requirements are valid, false otherwise
     */
    public boolean isValid() {
        // Max latency must be positive if specified
        if (maxLatencyMs != null && maxLatencyMs <= 0) return false;
        
        // Max jitter must be positive if specified
        if (maxJitterMs != null && maxJitterMs < 0) return false;
        
        // Max reorder must be positive if specified
        if (maxReorder != null && maxReorder < 0) return false;
        
        // Jitter should be less than latency
        if (maxLatencyMs != null && maxJitterMs != null && 
            maxJitterMs >= maxLatencyMs) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns whether latency limiting should be applied.
     */
    public boolean shouldLimitLatency() {
        return maxLatencyMs != null && maxLatencyMs > 0;
    }
    
    /**
     * Returns whether jitter limiting should be applied.
     */
    public boolean shouldLimitJitter() {
        return maxJitterMs != null && maxJitterMs > 0;
    }
    
    /**
     * Creates default latency requirements.
     */
    public static LatencyRequirements defaultRequirements() {
        return new LatencyRequirements();
    }
    
    /**
     * Creates latency requirements for real-time traffic.
     */
    public static LatencyRequirements forRealTime() {
        return new LatencyRequirements(50, 10); // 50ms max latency, 10ms max jitter
    }
    
    /**
     * Creates latency requirements for interactive traffic.
     */
    public static LatencyRequirements forInteractive() {
        return new LatencyRequirements(100, 20); // 100ms max latency, 20ms max jitter
    }
    
    /**
     * Creates latency requirements for bulk traffic.
     */
    public static LatencyRequirements forBulk() {
        return new LatencyRequirements(200, 50); // 200ms max latency, 50ms max jitter
    }
    
    /**
     * Creates latency requirements for background traffic.
     */
    public static LatencyRequirements forBackground() {
        return new LatencyRequirements(500, 100); // 500ms max latency, 100ms max jitter
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatencyRequirements that = (LatencyRequirements) o;
        return Objects.equals(maxLatencyMs, that.maxLatencyMs) &&
               Objects.equals(maxJitterMs, that.maxJitterMs) &&
               Objects.equals(maxReorder, that.maxReorder);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(maxLatencyMs, maxJitterMs, maxReorder);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LatencyRequirements{");
        if (maxLatencyMs != null) {
            sb.append("maxLatency=").append(maxLatencyMs).append("ms");
        }
        if (maxJitterMs != null) {
            if (sb.length() > 20) sb.append(", ");
            sb.append("maxJitter=").append(maxJitterMs).append("ms");
        }
        if (maxReorder != null) {
            if (sb.length() > 20) sb.append(", ");
            sb.append("maxReorder=").append(maxReorder);
        }
        sb.append("}");
        return sb.toString();
    }
}