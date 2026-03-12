package io.cresco.stunnel.qos.config;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;

/**
 * Extended tunnel configuration with QoS parameters.
 * This class is immutable - use the Builder pattern to create instances.
 */
public final class QoSTunnelConfig {
    // === Core Identification (from base config) ===
    private final String stunnelId;
    private final int srcPort;
    private final String dstHost;
    private final int dstPort;
    private final String dstRegion;
    private final String dstAgent;
    private final String dstPlugin;
    private final String srcRegion;
    private final String srcAgent;
    private final String srcPlugin;

    // === QoS Classification ===
    private final QoSClass qosClass;

    // === Bandwidth Profile ===
    private final BandwidthProfile bandwidthProfile;

    // === Latency Requirements ===
    private final LatencyRequirements latencyRequirements;

    // === Reliability Requirements ===
    private final ReliabilityRequirements reliabilityRequirements;

    // === Traffic Shaping Parameters ===
    private final int shapingBufferSize; // bytes
    private final int shapingQueueDepth; // packets
    private final boolean enableBackpressure;
    private final BackpressureStrategy backpressureStrategy;

    // === Monitoring Parameters ===
    private final int metricsSamplingInterval; // milliseconds
    private final boolean enableDetailedMetrics;

    // === Advanced/Extension Parameters ===
    private final Map<String, String> advancedParams;

    // === Builder Class ===
    public static class Builder {
        // Required parameters
        private final String stunnelId;
        private final int srcPort;
        private final String dstHost;
        private final int dstPort;

        // Optional parameters with defaults
        private QoSClass qosClass = QoSClass.INTERACTIVE;
        private BandwidthProfile bandwidthProfile = BandwidthProfile.defaultProfile();
        private LatencyRequirements latencyRequirements = LatencyRequirements.defaultRequirements();
        private ReliabilityRequirements reliabilityRequirements = ReliabilityRequirements.defaultRequirements();
        private int shapingBufferSize = 8192;
        private int shapingQueueDepth = 100;
        private boolean enableBackpressure = true;
        private BackpressureStrategy backpressureStrategy = BackpressureStrategy.PAUSE_READER;
        private int metricsSamplingInterval = 1000;
        private boolean enableDetailedMetrics = false;
        private Map<String, String> advancedParams = new HashMap<>();

        // Destination addressing (with defaults from plugin context)
        private String dstRegion = "";
        private String dstAgent = "";
        private String dstPlugin = "";
        private String srcRegion = "";
        private String srcAgent = "";
        private String srcPlugin = "";

        public Builder(String stunnelId, int srcPort, String dstHost, int dstPort) {
            this.stunnelId = Objects.requireNonNull(stunnelId, "stunnelId cannot be null");
            this.srcPort = srcPort;
            this.dstHost = Objects.requireNonNull(dstHost, "dstHost cannot be null");
            this.dstPort = dstPort;

            // Validate port ranges
            if (srcPort < 1 || srcPort > 65535) {
                throw new IllegalArgumentException("srcPort must be between 1 and 65535");
            }
            if (dstPort < 1 || dstPort > 65535) {
                throw new IllegalArgumentException("dstPort must be between 1 and 65535");
            }
        }

        public Builder qosClass(QoSClass qosClass) {
            this.qosClass = Objects.requireNonNull(qosClass);
            return this;
        }

        public Builder bandwidthProfile(BandwidthProfile bandwidthProfile) {
            this.bandwidthProfile = Objects.requireNonNull(bandwidthProfile);
            return this;
        }

        public Builder latencyRequirements(LatencyRequirements latencyRequirements) {
            this.latencyRequirements = Objects.requireNonNull(latencyRequirements);
            return this;
        }

        public Builder reliabilityRequirements(ReliabilityRequirements reliabilityRequirements) {
            this.reliabilityRequirements = Objects.requireNonNull(reliabilityRequirements);
            return this;
        }

        public Builder shapingBufferSize(int shapingBufferSize) {
            this.shapingBufferSize = shapingBufferSize;
            return this;
        }

        public Builder shapingQueueDepth(int shapingQueueDepth) {
            this.shapingQueueDepth = shapingQueueDepth;
            return this;
        }

        public Builder enableBackpressure(boolean enableBackpressure) {
            this.enableBackpressure = enableBackpressure;
            return this;
        }

        public Builder backpressureStrategy(BackpressureStrategy backpressureStrategy) {
            this.backpressureStrategy = Objects.requireNonNull(backpressureStrategy);
            return this;
        }

        public Builder metricsSamplingInterval(int metricsSamplingInterval) {
            this.metricsSamplingInterval = metricsSamplingInterval;
            return this;
        }

        public Builder enableDetailedMetrics(boolean enableDetailedMetrics) {
            this.enableDetailedMetrics = enableDetailedMetrics;
            return this;
        }

        public Builder advancedParams(Map<String, String> advancedParams) {
            this.advancedParams = new HashMap<>(advancedParams);
            return this;
        }

        public Builder addAdvancedParam(String key, String value) {
            this.advancedParams.put(key, value);
            return this;
        }

        public Builder dstRegion(String dstRegion) {
            this.dstRegion = dstRegion != null ? dstRegion : "";
            return this;
        }

        public Builder dstAgent(String dstAgent) {
            this.dstAgent = dstAgent != null ? dstAgent : "";
            return this;
        }

        public Builder dstPlugin(String dstPlugin) {
            this.dstPlugin = dstPlugin != null ? dstPlugin : "";
            return this;
        }

        public Builder srcRegion(String srcRegion) {
            this.srcRegion = srcRegion != null ? srcRegion : "";
            return this;
        }

        public Builder srcAgent(String srcAgent) {
            this.srcAgent = srcAgent != null ? srcAgent : "";
            return this;
        }

        public Builder srcPlugin(String srcPlugin) {
            this.srcPlugin = srcPlugin != null ? srcPlugin : "";
            return this;
        }

        public QoSTunnelConfig build() {
            // Apply validation
            validate();

            return new QoSTunnelConfig(this);
        }

        private void validate() {
            // Validate bandwidth profile
            if (!bandwidthProfile.isValid()) {
                throw new IllegalArgumentException("Invalid bandwidth profile: " + bandwidthProfile);
            }

            // Validate latency requirements
            if (!latencyRequirements.isValid()) {
                throw new IllegalArgumentException("Invalid latency requirements: " + latencyRequirements);
            }

            // Validate reliability requirements
            if (!reliabilityRequirements.isValid()) {
                throw new IllegalArgumentException("Invalid reliability requirements: " + reliabilityRequirements);
            }

            // Validate shaping parameters
            if (shapingBufferSize < 512 || shapingBufferSize > 65536) {
                throw new IllegalArgumentException("shapingBufferSize must be between 512 and 65536 bytes");
            }

            if (shapingQueueDepth < 1 || shapingQueueDepth > 10000) {
                throw new IllegalArgumentException("shapingQueueDepth must be between 1 and 10000 packets");
            }

            // Validate sampling interval
            if (metricsSamplingInterval < 10 || metricsSamplingInterval > 60000) {
                throw new IllegalArgumentException("metricsSamplingInterval must be between 10 and 60000 ms");
            }
        }
    }

    // Private constructor
    private QoSTunnelConfig(Builder builder) {
        this.stunnelId = builder.stunnelId;
        this.srcPort = builder.srcPort;
        this.dstHost = builder.dstHost;
        this.dstPort = builder.dstPort;
        this.dstRegion = builder.dstRegion;
        this.dstAgent = builder.dstAgent;
        this.dstPlugin = builder.dstPlugin;
        this.srcRegion = builder.srcRegion;
        this.srcAgent = builder.srcAgent;
        this.srcPlugin = builder.srcPlugin;
        this.qosClass = builder.qosClass;
        this.bandwidthProfile = builder.bandwidthProfile;
        this.latencyRequirements = builder.latencyRequirements;
        this.reliabilityRequirements = builder.reliabilityRequirements;
        this.shapingBufferSize = builder.shapingBufferSize;
        this.shapingQueueDepth = builder.shapingQueueDepth;
        this.enableBackpressure = builder.enableBackpressure;
        this.backpressureStrategy = builder.backpressureStrategy;
        this.metricsSamplingInterval = builder.metricsSamplingInterval;
        this.enableDetailedMetrics = builder.enableDetailedMetrics;
        this.advancedParams = Collections.unmodifiableMap(new HashMap<>(builder.advancedParams));
    }

    // Getters (no setters - immutable)
    public String getStunnelId() {
        return stunnelId;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public String getDstHost() {
        return dstHost;
    }

    public int getDstPort() {
        return dstPort;
    }

    public String getDstRegion() {
        return dstRegion;
    }

    public String getDstAgent() {
        return dstAgent;
    }

    public String getDstPlugin() {
        return dstPlugin;
    }

    public String getSrcRegion() {
        return srcRegion;
    }

    public String getSrcAgent() {
        return srcAgent;
    }

    public String getSrcPlugin() {
        return srcPlugin;
    }

    public QoSClass getQosClass() {
        return qosClass;
    }

    public BandwidthProfile getBandwidthProfile() {
        return bandwidthProfile;
    }

    public LatencyRequirements getLatencyRequirements() {
        return latencyRequirements;
    }

    public ReliabilityRequirements getReliabilityRequirements() {
        return reliabilityRequirements;
    }

    public int getShapingBufferSize() {
        return shapingBufferSize;
    }

    public int getShapingQueueDepth() {
        return shapingQueueDepth;
    }

    public boolean isEnableBackpressure() {
        return enableBackpressure;
    }

    public BackpressureStrategy getBackpressureStrategy() {
        return backpressureStrategy;
    }

    public int getMetricsSamplingInterval() {
        return metricsSamplingInterval;
    }

    public boolean isEnableDetailedMetrics() {
        return enableDetailedMetrics;
    }

    public Map<String, String> getAdvancedParams() {
        return advancedParams;
    }

    /**
     * Convert to legacy configuration map for backward compatibility.
     */
    public Map<String, String> toLegacyConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("stunnel_id", stunnelId);
        config.put("src_port", String.valueOf(srcPort));
        config.put("dst_host", dstHost);
        config.put("dst_port", String.valueOf(dstPort));
        config.put("dst_region", dstRegion);
        config.put("dst_agent", dstAgent);
        config.put("dst_plugin", dstPlugin);
        config.put("src_region", srcRegion);
        config.put("src_agent", srcAgent);
        config.put("src_plugin", srcPlugin);

        // Add QoS parameters as extended attributes
        config.put("qos_class", qosClass.name());
        if (bandwidthProfile.getMaximumBps() != null) {
            config.put("qos_bandwidth_max_bps", String.valueOf(bandwidthProfile.getMaximumBps()));
        }
        if (bandwidthProfile.getGuaranteedBps() != null) {
            config.put("qos_bandwidth_guaranteed_bps", String.valueOf(bandwidthProfile.getGuaranteedBps()));
        }
        if (latencyRequirements.getMaxLatencyMs() != null) {
            config.put("qos_latency_max_ms", String.valueOf(latencyRequirements.getMaxLatencyMs()));
        }
        config.put("qos_shaping_buffer_size", String.valueOf(shapingBufferSize));
        config.put("qos_shaping_queue_depth", String.valueOf(shapingQueueDepth));
        config.put("qos_enable_backpressure", String.valueOf(enableBackpressure));
        config.put("qos_backpressure_strategy", backpressureStrategy.getKey());
        config.put("qos_metrics_sampling_interval", String.valueOf(metricsSamplingInterval));
        config.put("qos_enable_detailed_metrics", String.valueOf(enableDetailedMetrics));

        // Add advanced parameters
        advancedParams.forEach((key, value) -> {
            config.put("qos_advanced_" + key, value);
        });

        return config;
    }

    /**
     * Create from legacy configuration map.
     */
    public static QoSTunnelConfig fromLegacyConfig(Map<String, String> config) {
        Builder builder = new Builder(
                config.get("stunnel_id"),
                Integer.parseInt(config.get("src_port")),
                config.get("dst_host"),
                Integer.parseInt(config.get("dst_port")));

        // Set destination addressing
        builder.dstRegion(config.get("dst_region"));
        builder.dstAgent(config.get("dst_agent"));
        builder.dstPlugin(config.get("dst_plugin"));
        builder.srcRegion(config.get("src_region"));
        builder.srcAgent(config.get("src_agent"));
        builder.srcPlugin(config.get("src_plugin"));

        // Parse QoS parameters if present
        if (config.containsKey("qos_class")) {
            try {
                builder.qosClass(QoSClass.valueOf(config.get("qos_class").toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Use default if invalid
                builder.qosClass(QoSClass.INTERACTIVE);
            }
        }

        // Parse bandwidth parameters
        BandwidthProfile bandwidthProfile = new BandwidthProfile();
        if (config.containsKey("qos_bandwidth_max_bps")) {
            try {
                bandwidthProfile.setMaximumBps(Long.parseLong(config.get("qos_bandwidth_max_bps")));
            } catch (NumberFormatException e) {
                // Ignore invalid value
            }
        }
        if (config.containsKey("qos_bandwidth_guaranteed_bps")) {
            try {
                bandwidthProfile.setGuaranteedBps(Long.parseLong(config.get("qos_bandwidth_guaranteed_bps")));
            } catch (NumberFormatException e) {
                // Ignore invalid value
            }
        }
        builder.bandwidthProfile(bandwidthProfile);

        // Parse latency parameters
        LatencyRequirements latencyRequirements = new LatencyRequirements();
        if (config.containsKey("qos_latency_max_ms")) {
            try {
                latencyRequirements.setMaxLatencyMs(Integer.parseInt(config.get("qos_latency_max_ms")));
            } catch (NumberFormatException e) {
                // Ignore invalid value
            }
        }
        builder.latencyRequirements(latencyRequirements);

        // Parse shaping parameters
        if (config.containsKey("qos_shaping_buffer_size")) {
            try {
                builder.shapingBufferSize(Integer.parseInt(config.get("qos_shaping_buffer_size")));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        if (config.containsKey("qos_shaping_queue_depth")) {
            try {
                builder.shapingQueueDepth(Integer.parseInt(config.get("qos_shaping_queue_depth")));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        if (config.containsKey("qos_enable_backpressure")) {
            builder.enableBackpressure(Boolean.parseBoolean(config.get("qos_enable_backpressure")));
        }

        if (config.containsKey("qos_backpressure_strategy")) {
            String strategy = config.get("qos_backpressure_strategy");
            for (BackpressureStrategy bs : BackpressureStrategy.values()) {
                if (bs.getKey().equals(strategy)) {
                    builder.backpressureStrategy(bs);
                    break;
                }
            }
        }

        // Parse monitoring parameters
        if (config.containsKey("qos_metrics_sampling_interval")) {
            try {
                builder.metricsSamplingInterval(Integer.parseInt(config.get("qos_metrics_sampling_interval")));
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        if (config.containsKey("qos_enable_detailed_metrics")) {
            builder.enableDetailedMetrics(Boolean.parseBoolean(config.get("qos_enable_detailed_metrics")));
        }

        // Parse advanced parameters
        Map<String, String> advanced = new HashMap<>();
        config.forEach((key, value) -> {
            if (key.startsWith("qos_advanced_")) {
                advanced.put(key.substring("qos_advanced_".length()), value);
            }
        });
        if (!advanced.isEmpty()) {
            builder.advancedParams(advanced);
        }

        return builder.build();
    }

    /**
     * Create a default configuration for a given QoS class.
     */
    public static QoSTunnelConfig defaultForClass(QoSClass qosClass,
            String stunnelId,
            int srcPort,
            String dstHost,
            int dstPort) {
        Builder builder = new Builder(stunnelId, srcPort, dstHost, dstPort)
                .qosClass(qosClass);

        // Set defaults based on QoS class
        switch (qosClass) {
            case REAL_TIME:
                builder.bandwidthProfile(BandwidthProfile.forRealTime())
                        .latencyRequirements(LatencyRequirements.forRealTime())
                        .reliabilityRequirements(ReliabilityRequirements.forRealTime())
                        .shapingBufferSize(4096)
                        .shapingQueueDepth(20)
                        .backpressureStrategy(BackpressureStrategy.DROP_LOWEST)
                        .metricsSamplingInterval(100)
                        .enableDetailedMetrics(true);
                break;
            case INTERACTIVE:
                builder.bandwidthProfile(BandwidthProfile.forInteractive())
                        .latencyRequirements(LatencyRequirements.forInteractive())
                        .reliabilityRequirements(ReliabilityRequirements.forInteractive())
                        .shapingBufferSize(8192)
                        .shapingQueueDepth(50)
                        .backpressureStrategy(BackpressureStrategy.PAUSE_READER)
                        .metricsSamplingInterval(500)
                        .enableDetailedMetrics(true);
                break;
            case BULK:
                builder.bandwidthProfile(BandwidthProfile.forBulk())
                        .latencyRequirements(LatencyRequirements.forBulk())
                        .reliabilityRequirements(ReliabilityRequirements.forBulk())
                        .shapingBufferSize(16384)
                        .shapingQueueDepth(200)
                        .backpressureStrategy(BackpressureStrategy.DROP_NEWEST)
                        .metricsSamplingInterval(2000)
                        .enableDetailedMetrics(false);
                break;
            case BACKGROUND:
                builder.bandwidthProfile(BandwidthProfile.forBackground())
                        .latencyRequirements(LatencyRequirements.forBackground())
                        .reliabilityRequirements(ReliabilityRequirements.forBackground())
                        .shapingBufferSize(32768)
                        .shapingQueueDepth(500)
                        .backpressureStrategy(BackpressureStrategy.BLOCK_WRITER)
                        .metricsSamplingInterval(5000)
                        .enableDetailedMetrics(false);
                break;
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QoSTunnelConfig that = (QoSTunnelConfig) o;
        return srcPort == that.srcPort &&
                dstPort == that.dstPort &&
                shapingBufferSize == that.shapingBufferSize &&
                shapingQueueDepth == that.shapingQueueDepth &&
                enableBackpressure == that.enableBackpressure &&
                metricsSamplingInterval == that.metricsSamplingInterval &&
                enableDetailedMetrics == that.enableDetailedMetrics &&
                Objects.equals(stunnelId, that.stunnelId) &&
                Objects.equals(dstHost, that.dstHost) &&
                Objects.equals(dstRegion, that.dstRegion) &&
                Objects.equals(dstAgent, that.dstAgent) &&
                Objects.equals(dstPlugin, that.dstPlugin) &&
                Objects.equals(srcRegion, that.srcRegion) &&
                Objects.equals(srcAgent, that.srcAgent) &&
                Objects.equals(srcPlugin, that.srcPlugin) &&
                qosClass == that.qosClass &&
                Objects.equals(bandwidthProfile, that.bandwidthProfile) &&
                Objects.equals(latencyRequirements, that.latencyRequirements) &&
                Objects.equals(reliabilityRequirements, that.reliabilityRequirements) &&
                backpressureStrategy == that.backpressureStrategy &&
                Objects.equals(advancedParams, that.advancedParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stunnelId, srcPort, dstHost, dstPort, dstRegion, dstAgent, dstPlugin,
                srcRegion, srcAgent, srcPlugin, qosClass, bandwidthProfile,
                latencyRequirements, reliabilityRequirements, shapingBufferSize,
                shapingQueueDepth, enableBackpressure, backpressureStrategy,
                metricsSamplingInterval, enableDetailedMetrics, advancedParams);
    }

    @Override
    public String toString() {
        return "QoSTunnelConfig{" +
                "stunnelId='" + stunnelId + '\'' +
                ", srcPort=" + srcPort +
                ", dstHost='" + dstHost + '\'' +
                ", dstPort=" + dstPort +
                ", qosClass=" + qosClass +
                ", bandwidthProfile=" + bandwidthProfile +
                ", latencyRequirements=" + latencyRequirements +
                ", shapingBufferSize=" + shapingBufferSize +
                ", shapingQueueDepth=" + shapingQueueDepth +
                ", enableBackpressure=" + enableBackpressure +
                ", backpressureStrategy=" + backpressureStrategy +
                '}';
    }
}