package io.cresco.stunnel.qos.config;

import java.util.Objects;

/**
 * Defines bandwidth requirements and limits for a tunnel.
 * All values are in bits per second (bps).
 */
public class BandwidthProfile {
    private Long guaranteedBps;     // Guaranteed bandwidth (bits/sec), null = no guarantee
    private Long maximumBps;        // Maximum bandwidth (bits/sec), null = no limit
    private Long burstBps;          // Burst bandwidth (bits/sec), null = calculated
    private Integer burstDurationMs; // Burst duration in milliseconds
    
    /**
     * Default constructor with no limits.
     */
    public BandwidthProfile() {
        // No limits by default
    }
    
    /**
     * Constructor with maximum bandwidth limit.
     */
    public BandwidthProfile(long maximumBps) {
        this.maximumBps = maximumBps;
    }
    
    /**
     * Constructor with guaranteed and maximum bandwidth.
     */
    public BandwidthProfile(long guaranteedBps, long maximumBps) {
        this.guaranteedBps = guaranteedBps;
        this.maximumBps = maximumBps;
    }
    
    public Long getGuaranteedBps() {
        return guaranteedBps;
    }
    
    public BandwidthProfile setGuaranteedBps(Long guaranteedBps) {
        this.guaranteedBps = guaranteedBps;
        return this;
    }
    
    public Long getMaximumBps() {
        return maximumBps;
    }
    
    public BandwidthProfile setMaximumBps(Long maximumBps) {
        this.maximumBps = maximumBps;
        return this;
    }
    
    public Long getBurstBps() {
        return burstBps;
    }
    
    public BandwidthProfile setBurstBps(Long burstBps) {
        this.burstBps = burstBps;
        return this;
    }
    
    public Integer getBurstDurationMs() {
        return burstDurationMs;
    }
    
    public BandwidthProfile setBurstDurationMs(Integer burstDurationMs) {
        this.burstDurationMs = burstDurationMs;
        return this;
    }
    
    /**
     * Validates the bandwidth profile.
     * @return true if the profile is valid, false otherwise
     */
    public boolean isValid() {
        // Maximum must be positive if specified
        if (maximumBps != null && maximumBps <= 0) return false;
        
        // Guaranteed must be positive if specified
        if (guaranteedBps != null && guaranteedBps <= 0) return false;
        
        // Guaranteed cannot exceed maximum
        if (guaranteedBps != null && maximumBps != null && 
            guaranteedBps > maximumBps) return false;
        
        // Burst must be positive if specified
        if (burstBps != null && burstBps <= 0) return false;
        
        // Burst duration must be positive if specified
        if (burstDurationMs != null && burstDurationMs <= 0) return false;
        
        // Burst should be >= maximum if both specified
        if (burstBps != null && maximumBps != null && burstBps < maximumBps) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns the effective burst bandwidth.
     * If not specified, defaults to 2x maximum or 2x guaranteed.
     */
    public long getEffectiveBurstBps() {
        if (burstBps != null) return burstBps;
        if (maximumBps != null) return maximumBps * 2;
        if (guaranteedBps != null) return guaranteedBps * 2;
        return Long.MAX_VALUE; // No bandwidth limits
    }
    
    /**
     * Returns the effective burst duration.
     */
    public int getEffectiveBurstDurationMs() {
        if (burstDurationMs != null) return burstDurationMs;
        return 100; // Default 100ms burst duration
    }
    
    /**
     * Returns whether bandwidth limiting should be applied.
     */
    public boolean shouldLimitBandwidth() {
        return maximumBps != null || guaranteedBps != null;
    }
    
    /**
     * Returns whether this profile has a guaranteed bandwidth.
     */
    public boolean hasGuaranteedBandwidth() {
        return guaranteedBps != null && guaranteedBps > 0;
    }
    
    /**
     * Returns whether this profile has a maximum bandwidth limit.
     */
    public boolean hasMaximumBandwidth() {
        return maximumBps != null && maximumBps > 0;
    }
    
    /**
     * Creates a default bandwidth profile with no limits.
     */
    public static BandwidthProfile defaultProfile() {
        return new BandwidthProfile();
    }
    
    /**
     * Creates a bandwidth profile for real-time traffic.
     */
    public static BandwidthProfile forRealTime() {
        return new BandwidthProfile(2_000_000L, 10_000_000L) // 2 Mbps guaranteed, 10 Mbps max
            .setBurstBps(20_000_000L)
            .setBurstDurationMs(50);
    }
    
    /**
     * Creates a bandwidth profile for interactive traffic.
     */
    public static BandwidthProfile forInteractive() {
        return new BandwidthProfile(1_000_000L, 5_000_000L) // 1 Mbps guaranteed, 5 Mbps max
            .setBurstBps(10_000_000L)
            .setBurstDurationMs(100);
    }
    
    /**
     * Creates a bandwidth profile for bulk traffic.
     */
    public static BandwidthProfile forBulk() {
        return new BandwidthProfile()
            .setMaximumBps(100_000_000L) // 100 Mbps max, no guarantee
            .setBurstBps(200_000_000L)
            .setBurstDurationMs(200);
    }
    
    /**
     * Creates a bandwidth profile for background traffic.
     */
    public static BandwidthProfile forBackground() {
        return new BandwidthProfile()
            .setMaximumBps(10_000_000L) // 10 Mbps max, no guarantee
            .setBurstBps(20_000_000L)
            .setBurstDurationMs(500);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BandwidthProfile that = (BandwidthProfile) o;
        return Objects.equals(guaranteedBps, that.guaranteedBps) &&
               Objects.equals(maximumBps, that.maximumBps) &&
               Objects.equals(burstBps, that.burstBps) &&
               Objects.equals(burstDurationMs, that.burstDurationMs);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(guaranteedBps, maximumBps, burstBps, burstDurationMs);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BandwidthProfile{");
        if (guaranteedBps != null) {
            sb.append("guaranteed=").append(formatBps(guaranteedBps));
        }
        if (maximumBps != null) {
            if (sb.length() > 17) sb.append(", ");
            sb.append("max=").append(formatBps(maximumBps));
        }
        if (burstBps != null) {
            if (sb.length() > 17) sb.append(", ");
            sb.append("burst=").append(formatBps(burstBps));
        }
        if (burstDurationMs != null) {
            if (sb.length() > 17) sb.append(", ");
            sb.append("burstDuration=").append(burstDurationMs).append("ms");
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String formatBps(long bps) {
        if (bps >= 1_000_000_000L) {
            return String.format("%.1f Gbps", bps / 1_000_000_000.0);
        } else if (bps >= 1_000_000L) {
            return String.format("%.1f Mbps", bps / 1_000_000.0);
        } else if (bps >= 1_000L) {
            return String.format("%.1f Kbps", bps / 1_000.0);
        } else {
            return bps + " bps";
        }
    }
}