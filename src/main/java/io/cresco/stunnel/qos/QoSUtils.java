package io.cresco.stunnel.qos;

import io.cresco.stunnel.qos.config.QoSTunnelConfig;
import io.cresco.stunnel.qos.config.QoSClass;
import io.cresco.stunnel.qos.config.BandwidthProfile;
import io.cresco.stunnel.qos.config.LatencyRequirements;
import io.cresco.stunnel.qos.config.ReliabilityRequirements;
import io.cresco.stunnel.qos.config.BackpressureStrategy;

import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for QoS operations.
 */
public class QoSUtils {

    /**
     * Checks if a configuration map contains QoS parameters.
     * Returns true if any parameter starts with "qos_" prefix.
     */
    public static boolean hasQoSParameters(Map<String, String> config) {
        for (String key : config.keySet()) {
            if (key.startsWith("qos_")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts QoS configuration from a tunnel configuration map.
     * Returns null if no QoS parameters are present.
     */
    public static QoSTunnelConfig extractQoSConfig(Map<String, String> config) {
        if (!hasQoSParameters(config)) {
            return null;
        }

        try {
            return QoSTunnelConfig.fromLegacyConfig(config);
        } catch (Exception e) {
            // If parsing fails, return null (no QoS)
            return null;
        }
    }

    /**
     * Creates a QoS-enabled configuration for a video conference tunnel.
     */
    public static Map<String, String> createVideoConferenceConfig(
            String stunnelId, int srcPort, String dstHost, int dstPort) {

        QoSTunnelConfig qosConfig = QoSTunnelConfig.defaultForClass(
                QoSClass.REAL_TIME, stunnelId, srcPort, dstHost, dstPort);

        return qosConfig.toLegacyConfig();
    }

    /**
     * Creates a QoS-enabled configuration for a file transfer tunnel.
     */
    public static Map<String, String> createFileTransferConfig(
            String stunnelId, int srcPort, String dstHost, int dstPort) {

        QoSTunnelConfig qosConfig = QoSTunnelConfig.defaultForClass(
                QoSClass.BULK, stunnelId, srcPort, dstHost, dstPort);

        return qosConfig.toLegacyConfig();
    }

    /**
     * Creates a QoS-enabled configuration for interactive SSH tunnel.
     */
    public static Map<String, String> createInteractiveConfig(
            String stunnelId, int srcPort, String dstHost, int dstPort) {

        QoSTunnelConfig qosConfig = QoSTunnelConfig.defaultForClass(
                QoSClass.INTERACTIVE, stunnelId, srcPort, dstHost, dstPort);

        return qosConfig.toLegacyConfig();
    }

    /**
     * Creates a QoS-enabled configuration for background sync tunnel.
     */
    public static Map<String, String> createBackgroundConfig(
            String stunnelId, int srcPort, String dstHost, int dstPort) {

        QoSTunnelConfig qosConfig = QoSTunnelConfig.defaultForClass(
                QoSClass.BACKGROUND, stunnelId, srcPort, dstHost, dstPort);

        return qosConfig.toLegacyConfig();
    }

    /**
     * Validates that QoS parameters are consistent and reasonable.
     */
    public static boolean validateQoSParameters(Map<String, String> config) {
        try {
            QoSTunnelConfig qosConfig = QoSTunnelConfig.fromLegacyConfig(config);
            // The builder already validates, so if we get here it's valid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets a human-readable description of QoS configuration.
     */
    public static String describeQoSConfig(Map<String, String> config) {
        QoSTunnelConfig qosConfig = extractQoSConfig(config);
        if (qosConfig == null) {
            return "No QoS configuration";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("QoS Class: ").append(qosConfig.getQosClass().getDisplayName());

        BandwidthProfile bandwidth = qosConfig.getBandwidthProfile();
        if (bandwidth.hasMaximumBandwidth()) {
            sb.append(", Max Bandwidth: ").append(formatBps(bandwidth.getMaximumBps()));
        }
        if (bandwidth.hasGuaranteedBandwidth()) {
            sb.append(", Guaranteed Bandwidth: ").append(formatBps(bandwidth.getGuaranteedBps()));
        }

        LatencyRequirements latency = qosConfig.getLatencyRequirements();
        if (latency.shouldLimitLatency()) {
            sb.append(", Max Latency: ").append(latency.getMaxLatencyMs()).append("ms");
        }

        sb.append(", Buffer: ").append(qosConfig.getShapingBufferSize()).append(" bytes");
        sb.append(", Queue: ").append(qosConfig.getShapingQueueDepth()).append(" packets");
        sb.append(", Backpressure: ").append(qosConfig.getBackpressureStrategy().getDescription());

        return sb.toString();
    }

    private static String formatBps(long bps) {
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