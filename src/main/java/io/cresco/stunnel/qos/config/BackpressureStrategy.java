package io.cresco.stunnel.qos.config;

/**
 * Defines strategies for handling backpressure when queues are full.
 */
public enum BackpressureStrategy {
    /**
     * Drop the lowest priority packet to make room for new packets.
     * Best for real-time traffic where latency is critical.
     */
    DROP_LOWEST("drop_lowest", "Drop lowest priority packet"),
    
    /**
     * Drop the newest packet (the one that would cause overflow).
     * Simple and predictable, good for bulk traffic.
     */
    DROP_NEWEST("drop_newest", "Drop newest packet"),
    
    /**
     * Pause the reader to apply backpressure up the stack.
     * Preserves all packets but may cause buffer bloat.
     */
    PAUSE_READER("pause_reader", "Pause reader to apply backpressure"),
    
    /**
     * Block the writer until space becomes available.
     * Preserves all packets but may cause thread blocking.
     */
    BLOCK_WRITER("block_writer", "Block writer until space available"),
    
    /**
     * Apply adaptive backpressure based on queue conditions.
     * Uses different strategies based on current conditions.
     */
    ADAPTIVE("adaptive", "Adaptive strategy based on conditions");
    
    private final String key;
    private final String description;
    
    BackpressureStrategy(String key, String description) {
        this.key = key;
        this.description = description;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the recommended strategy for a given QoS class.
     */
    public static BackpressureStrategy recommendedFor(QoSClass qosClass) {
        switch (qosClass) {
            case REAL_TIME:
                return DROP_LOWEST;      // Preserve latency at all costs
            case INTERACTIVE:
                return PAUSE_READER;     // Balance latency and reliability
            case BULK:
                return DROP_NEWEST;      // Simple and efficient
            case BACKGROUND:
                return BLOCK_WRITER;     // Don't drop if we can wait
            default:
                return ADAPTIVE;
        }
    }
    
    /**
     * Returns whether this strategy involves dropping packets.
     */
    public boolean involvesDropping() {
        return this == DROP_LOWEST || this == DROP_NEWEST;
    }
    
    /**
     * Returns whether this strategy involves applying backpressure.
     */
    public boolean involvesBackpressure() {
        return this == PAUSE_READER || this == BLOCK_WRITER || this == ADAPTIVE;
    }
    
    @Override
    public String toString() {
        return description + " (" + key + ")";
    }
}