package io.cresco.stunnel.qos.example;

import io.cresco.stunnel.PluginExecutor;
import io.cresco.stunnel.qos.QoSUtils;
import io.cresco.stunnel.qos.config.QoSTunnelConfig;
import io.cresco.library.messaging.MsgEvent;
import io.cresco.library.plugin.PluginBuilder;
import io.cresco.library.utilities.CLogger;
import io.cresco.stunnel.SocketController;
import com.google.gson.Gson;

import java.util.Map;

/**
 * Example of how to extend PluginExecutor to support QoS.
 * This shows the integration pattern without modifying the original code.
 */
public class QoSAwarePluginExecutorExample extends PluginExecutor {

    private final Gson gson;

    public QoSAwarePluginExecutorExample(PluginBuilder pluginBuilder, SocketController socketController) {
        super(pluginBuilder, socketController);
        this.gson = new Gson();
    }

    @Override
    public MsgEvent executeCONFIG(MsgEvent incoming) {
        String action = incoming.getParam("action");

        // Handle QoS-specific actions
        if ("configqostunnel".equals(action)) {
            return configQoSTunnel(incoming);
        }

        // For existing actions, check if they contain QoS parameters
        if ("configsrctunnel".equals(action)) {
            return handleQoSSrcTunnel(incoming);
        }

        // Let parent handle other actions
        return super.executeCONFIG(incoming);
    }

    private MsgEvent configQoSTunnel(MsgEvent incoming) {
        try {
            // Parse QoS configuration from message
            String qosConfigJson = incoming.getParam("qos_config");
            if (qosConfigJson == null) {
                incoming.setParam("status", "400");
                incoming.setParam("status_desc", "Missing qos_config parameter");
                return incoming;
            }

            // Parse the configuration
            Map<String, String> config = gson.fromJson(qosConfigJson, Map.class);

            // Validate QoS parameters
            if (!QoSUtils.validateQoSParameters(config)) {
                incoming.setParam("status", "400");
                incoming.setParam("status_desc", "Invalid QoS parameters");
                return incoming;
            }

            // Extract QoS configuration
            QoSTunnelConfig qosConfig = QoSUtils.extractQoSConfig(config);
            if (qosConfig == null) {
                incoming.setParam("status", "400");
                incoming.setParam("status_desc", "Failed to parse QoS configuration");
                return incoming;
            }

            // Convert to legacy format for compatibility
            Map<String, String> legacyConfig = qosConfig.toLegacyConfig();

            // Log QoS configuration
            getLogger().info("Creating QoS tunnel: " + QoSUtils.describeQoSConfig(legacyConfig));

            // Call parent's configSrcTunnel with enhanced configuration
            // This would need access to the parent's protected/package-private methods
            // In a real implementation, we might need to refactor or use composition

            incoming.setParam("status", "10");
            incoming.setParam("status_desc", "QoS tunnel configuration accepted");
            incoming.setParam("qos_applied", "true");

        } catch (Exception e) {
            getLogger().error("Failed to configure QoS tunnel", e);
            incoming.setParam("status", "500");
            incoming.setParam("status_desc", "Internal error: " + e.getMessage());
        }

        return incoming;
    }

    private MsgEvent handleQoSSrcTunnel(MsgEvent incoming) {
        // First, let parent handle the basic configuration
        MsgEvent response = super.executeCONFIG(incoming);

        // Check if the tunnel was created successfully
        if ("10".equals(response.getParam("status"))) {
            // Check if QoS parameters are present
            Map<String, String> params = incoming.getParams();
            if (QoSUtils.hasQoSParameters(params)) {
                // Extract and apply QoS configuration
                QoSTunnelConfig qosConfig = QoSUtils.extractQoSConfig(params);
                if (qosConfig != null) {
                    // Apply QoS configuration to the tunnel
                    applyQoSConfiguration(qosConfig);

                    // Update response to indicate QoS was applied
                    response.setParam("qos_applied", "true");
                    response.setParam("qos_class", qosConfig.getQosClass().name());
                }
            }
        }

        return response;
    }

    private void applyQoSConfiguration(QoSTunnelConfig qosConfig) {
        // In a real implementation, this would:
        // 1. Store QoS configuration with the tunnel
        // 2. Configure traffic shaping
        // 3. Set up QoS monitoring
        // 4. Apply priority settings

        getLogger().info("Applying QoS configuration for tunnel " + qosConfig.getStunnelId());
        getLogger().info("  QoS Class: " + qosConfig.getQosClass());
        getLogger().info("  Bandwidth: " + qosConfig.getBandwidthProfile());
        getLogger().info("  Latency: " + qosConfig.getLatencyRequirements());
        getLogger().info("  Buffer: " + qosConfig.getShapingBufferSize() + " bytes");
        getLogger().info("  Queue: " + qosConfig.getShapingQueueDepth() + " packets");

        // Example: Create and configure traffic shaper
        if (qosConfig.getBandwidthProfile().shouldLimitBandwidth()) {
            configureTrafficShaper(qosConfig);
        }

        // Example: Set up QoS monitoring
        configureQoSMetrics(qosConfig);
    }

    private void configureTrafficShaper(QoSTunnelConfig qosConfig) {
        // This would create and configure a TokenBucketRateLimiter
        // and attach it to the tunnel's Netty pipeline

        long maxBps = qosConfig.getBandwidthProfile().getMaximumBps();
        long burstBps = qosConfig.getBandwidthProfile().getEffectiveBurstBps();

        getLogger().info("Configuring traffic shaper: " +
                "rate=" + (maxBps / 8) + " B/s, " +
                "burst=" + (burstBps / 8) + " bytes");

        // In a real implementation:
        // TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(maxBps / 8,
        // burstBps / 8);
        // QoSTrafficShapingHandler shaper = new QoSTrafficShapingHandler(limiter, ...);
        // Add shaper to the tunnel's Netty pipeline
    }

    private void configureQoSMetrics(QoSTunnelConfig qosConfig) {
        // Set up QoS-specific metrics collection
        int samplingInterval = qosConfig.getMetricsSamplingInterval();
        boolean detailedMetrics = qosConfig.isEnableDetailedMetrics();

        getLogger().info("Configuring QoS metrics: " +
                "sampling=" + samplingInterval + "ms, " +
                "detailed=" + detailedMetrics);

        // In a real implementation:
        // 1. Create QoSPerformanceMonitor
        // 2. Register metrics with Micrometer/whatever monitoring system
        // 3. Set up alerts for QoS violations
    }

    // Helper method to get logger (would need proper access in real implementation)
    private CLogger getLogger() {
        // This is simplified - in reality we'd need proper access to the logger
        return null;
    }
}
