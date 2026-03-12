package io.cresco.stunnel.qos.config;

import java.util.Objects;

/**
 * Defines reliability requirements for a tunnel.
 */
public class ReliabilityRequirements {
    private Double maxPacketLoss;        // Maximum packet loss rate (0.0-1.0)
    private Boolean enableFec;           // Enable forward error correction
    private Integer fecRedundancy;       // FEC redundancy percentage (0-100)
    private Boolean enableRetransmission; // Enable packet retransmission
    private Integer maxRetransmissions;  // Maximum retransmission attempts
    
    /**
     * Default constructor with no specific requirements.
     */
    public ReliabilityRequirements() {
        // No requirements by default
    }
    
    /**
     * Constructor with maximum packet loss requirement.
     */
    public ReliabilityRequirements(double maxPacketLoss) {
        this.maxPacketLoss = maxPacketLoss;
    }
    
    public Double getMaxPacketLoss() {
        return maxPacketLoss;
    }
    
    public ReliabilityRequirements setMaxPacketLoss(Double maxPacketLoss) {
        this.maxPacketLoss = maxPacketLoss;
        return this;
    }
    
    public Boolean getEnableFec() {
        return enableFec;
    }
    
    public ReliabilityRequirements setEnableFec(Boolean enableFec) {
        this.enableFec = enableFec;
        return this;
    }
    
    public Integer getFecRedundancy() {
        return fecRedundancy;
    }
    
    public ReliabilityRequirements setFecRedundancy(Integer fecRedundancy) {
        this.fecRedundancy = fecRedundancy;
        return this;
    }
    
    public Boolean getEnableRetransmission() {
        return enableRetransmission;
    }
    
    public ReliabilityRequirements setEnableRetransmission(Boolean enableRetransmission) {
        this.enableRetransmission = enableRetransmission;
        return this;
    }
    
    public Integer getMaxRetransmissions() {
        return maxRetransmissions;
    }
    
    public ReliabilityRequirements setMaxRetransmissions(Integer maxRetransmissions) {
        this.maxRetransmissions = maxRetransmissions;
        return this;
    }
    
    /**
     * Validates the reliability requirements.
     * @return true if the requirements are valid, false otherwise
     */
    public boolean isValid() {
        // Max packet loss must be between 0 and 1 if specified
        if (maxPacketLoss != null && (maxPacketLoss < 0.0 || maxPacketLoss > 1.0)) {
            return false;
        }
        
        // FEC redundancy must be between 0 and 100 if specified
        if (fecRedundancy != null && (fecRedundancy < 0 || fecRedundancy > 100)) {
            return false;
        }
        
        // Max retransmissions must be positive if specified
        if (maxRetransmissions != null && maxRetransmissions < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns whether packet loss monitoring should be applied.
     */
    public boolean shouldMonitorPacketLoss() {
        return maxPacketLoss != null && maxPacketLoss < 1.0;
    }
    
    /**
     * Returns whether forward error correction should be enabled.
     */
    public boolean shouldEnableFec() {
        return enableFec != null && enableFec;
    }
    
    /**
     * Returns whether retransmission should be enabled.
     */
    public boolean shouldEnableRetransmission() {
        return enableRetransmission != null && enableRetransmission;
    }
    
    /**
     * Creates default reliability requirements.
     */
    public static ReliabilityRequirements defaultRequirements() {
        return new ReliabilityRequirements();
    }
    
    /**
     * Creates reliability requirements for real-time traffic.
     */
    public static ReliabilityRequirements forRealTime() {
        return new ReliabilityRequirements()
            .setMaxPacketLoss(0.001)          // 0.1% max packet loss
            .setEnableFec(true)               // Enable FEC
            .setFecRedundancy(20)             // 20% redundancy
            .setEnableRetransmission(false);  // No retransmission (adds latency)
    }
    
    /**
     * Creates reliability requirements for interactive traffic.
     */
    public static ReliabilityRequirements forInteractive() {
        return new ReliabilityRequirements()
            .setMaxPacketLoss(0.01)           // 1% max packet loss
            .setEnableFec(true)               // Enable FEC
            .setFecRedundancy(10)             // 10% redundancy
            .setEnableRetransmission(true)    // Enable retransmission
            .setMaxRetransmissions(3);        // Max 3 retransmissions
    }
    
    /**
     * Creates reliability requirements for bulk traffic.
     */
    public static ReliabilityRequirements forBulk() {
        return new ReliabilityRequirements()
            .setMaxPacketLoss(0.05)           // 5% max packet loss
            .setEnableFec(false)              // Disable FEC (overhead not worth it)
            .setEnableRetransmission(true)    // Enable retransmission
            .setMaxRetransmissions(5);        // Max 5 retransmissions
    }
    
    /**
     * Creates reliability requirements for background traffic.
     */
    public static ReliabilityRequirements forBackground() {
        return new ReliabilityRequirements()
            .setMaxPacketLoss(0.1)            // 10% max packet loss
            .setEnableFec(false)              // Disable FEC
            .setEnableRetransmission(true)    // Enable retransmission
            .setMaxRetransmissions(2);        // Max 2 retransmissions
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReliabilityRequirements that = (ReliabilityRequirements) o;
        return Objects.equals(maxPacketLoss, that.maxPacketLoss) &&
               Objects.equals(enableFec, that.enableFec) &&
               Objects.equals(fecRedundancy, that.fecRedundancy) &&
               Objects.equals(enableRetransmission, that.enableRetransmission) &&
               Objects.equals(maxRetransmissions, that.maxRetransmissions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(maxPacketLoss, enableFec, fecRedundancy, 
                           enableRetransmission, maxRetransmissions);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ReliabilityRequirements{");
        if (maxPacketLoss != null) {
            sb.append("maxPacketLoss=").append(String.format("%.3f", maxPacketLoss * 100)).append("%");
        }
        if (enableFec != null) {
            if (sb.length() > 27) sb.append(", ");
            sb.append("fec=").append(enableFec);
            if (fecRedundancy != null) {
                sb.append("(").append(fecRedundancy).append("%)");
            }
        }
        if (enableRetransmission != null) {
            if (sb.length() > 27) sb.append(", ");
            sb.append("retrans=").append(enableRetransmission);
            if (maxRetransmissions != null) {
                sb.append("(").append(maxRetransmissions).append(")");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}