package io.cresco.stunnel.qos.config;

/**
 * Defines the QoS class/priority for a tunnel.
 * Higher priority values indicate more important traffic.
 */
public enum QoSClass {
    /**
     * Real-time traffic: VoIP, video conferencing, gaming
     * Requires low latency and jitter
     */
    REAL_TIME(9, "Real-time", 0.95, 0.99, 50, 10),
    
    /**
     * Interactive traffic: SSH, remote desktop, web browsing
     * Requires responsive interaction
     */
    INTERACTIVE(7, "Interactive", 0.90, 0.95, 100, 20),
    
    /**
     * Bulk traffic: File transfers, backups, sync
     * Requires high throughput, tolerant of latency
     */
    BULK(5, "Bulk", 0.80, 0.90, 200, 50),
    
    /**
     * Background traffic: Non-urgent background tasks
     * Lowest priority, can be delayed
     */
    BACKGROUND(3, "Background", 0.70, 0.80, 500, 100);
    
    private final int priority;           // Priority value (higher = more important)
    private final String displayName;     // Human-readable name
    private final double minBandwidthUtilization;  // Minimum bandwidth utilization target
    private final double maxBandwidthUtilization;  // Maximum bandwidth utilization target
    private final int defaultMaxLatencyMs;         // Default maximum latency
    private final int defaultMaxJitterMs;          // Default maximum jitter
    
    QoSClass(int priority, String displayName, 
             double minBandwidthUtilization, double maxBandwidthUtilization,
             int defaultMaxLatencyMs, int defaultMaxJitterMs) {
        this.priority = priority;
        this.displayName = displayName;
        this.minBandwidthUtilization = minBandwidthUtilization;
        this.maxBandwidthUtilization = maxBandwidthUtilization;
        this.defaultMaxLatencyMs = defaultMaxLatencyMs;
        this.defaultMaxJitterMs = defaultMaxJitterMs;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getMinBandwidthUtilization() {
        return minBandwidthUtilization;
    }
    
    public double getMaxBandwidthUtilization() {
        return maxBandwidthUtilization;
    }
    
    public int getDefaultMaxLatencyMs() {
        return defaultMaxLatencyMs;
    }
    
    public int getDefaultMaxJitterMs() {
        return defaultMaxJitterMs;
    }
    
    /**
     * Returns whether this QoS class is higher priority than another.
     */
    public boolean isHigherPriorityThan(QoSClass other) {
        return this.priority > other.priority;
    }
    
    /**
     * Returns whether this QoS class is lower priority than another.
     */
    public boolean isLowerPriorityThan(QoSClass other) {
        return this.priority < other.priority;
    }
    
    /**
     * Returns the recommended buffer size for this QoS class.
     */
    public int getRecommendedBufferSize() {
        switch (this) {
            case REAL_TIME: return 4096;      // Small buffers for low latency
            case INTERACTIVE: return 8192;    // Medium buffers
            case BULK: return 16384;          // Large buffers for throughput
            case BACKGROUND: return 32768;    // Very large buffers
            default: return 8192;
        }
    }
    
    /**
     * Returns the recommended queue depth for this QoS class.
     */
    public int getRecommendedQueueDepth() {
        switch (this) {
            case REAL_TIME: return 20;        // Small queue for low latency
            case INTERACTIVE: return 50;      // Medium queue
            case BULK: return 200;            // Large queue for throughput
            case BACKGROUND: return 500;      // Very large queue
            default: return 100;
        }
    }
    
    /**
     * Returns the recommended sampling interval for metrics.
     */
    public int getRecommendedSamplingIntervalMs() {
        switch (this) {
            case REAL_TIME: return 100;       // Frequent sampling
            case INTERACTIVE: return 500;     // Moderate sampling
            case BULK: return 2000;           // Infrequent sampling
            case BACKGROUND: return 5000;     // Very infrequent sampling
            default: return 1000;
        }
    }
    
    @Override
    public String toString() {
        return displayName + " (priority: " + priority + ")";
    }
}
