package io.cresco.stunnel.qos.example;

import io.cresco.stunnel.qos.config.*;
import io.cresco.stunnel.qos.shaping.TokenBucketRateLimiter;
import io.cresco.stunnel.qos.QoSUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Real-world integration example showing how QoS would be used
 * in a production stunnel deployment with multiple tunnel types.
 */
public class RealWorldQoSIntegrationExample {

    public static void main(String[] args) {
        System.out.println("=== Real-World QoS Integration Example ===\n");

        // Scenario: A company needs to deploy multiple tunnels with different QoS
        // requirements
        // 1. Video conferencing tunnel (real-time, low latency)
        // 2. Database replication tunnel (interactive, reliable)
        // 3. Backup tunnel (bulk, high throughput)
        // 4. Monitoring tunnel (background, low priority)

        System.out.println("Scenario: Multi-tenant stunnel deployment with QoS\n");

        // Create QoS configurations for each tunnel type
        List<QoSTunnelConfig> tunnelConfigs = new ArrayList<>();

        // 1. Video conferencing tunnel
        tunnelConfigs.add(createVideoConferencingTunnel());

        // 2. Database replication tunnel
        tunnelConfigs.add(createDatabaseReplicationTunnel());

        // 3. Backup tunnel
        tunnelConfigs.add(createBackupTunnel());

        // 4. Monitoring tunnel
        tunnelConfigs.add(createMonitoringTunnel());

        // Display all configurations
        System.out.println("Created " + tunnelConfigs.size() + " tunnel configurations:\n");

        for (QoSTunnelConfig config : tunnelConfigs) {
            displayTunnelConfig(config);
        }

        // Simulate network resource allocation
        System.out.println("\n=== Simulating Network Resource Allocation ===\n");
        simulateResourceAllocation(tunnelConfigs);

        // Show how to integrate with existing stunnel configuration
        System.out.println("\n=== Integration with Existing Stunnel Configuration ===\n");
        showLegacyIntegration(tunnelConfigs.get(0));

        System.out.println("\n=== Example Complete ===");
    }

    private static QoSTunnelConfig createVideoConferencingTunnel() {
        return new QoSTunnelConfig.Builder(
                "video-conf-1", 1935, "video-server.internal", 1935)
                .qosClass(QoSClass.REAL_TIME)
                .bandwidthProfile(BandwidthProfile.builder()
                        .guaranteedBps(4_000_000L) // 4 Mbps guaranteed for 720p video
                        .maximumBps(8_000_000L) // 8 Mbps maximum for bursts
                        .burstBps(12_000_000L) // 12 Mbps burst capacity
                        .burstDurationMs(500) // 500ms burst duration
                        .build())
                .latencyRequirements(LatencyRequirements.builder()
                        .maxLatencyMs(150) // 150ms max for interactive video
                        .maxJitterMs(30) // 30ms max jitter
                        .maxReorder(5) // Max 5 packets out of order
                        .build())
                .reliabilityRequirements(ReliabilityRequirements.builder()
                        .maxPacketLoss(0.001) // 0.1% max packet loss
                        .enableFec(true) // Enable forward error correction
                        .fecRedundancy(20) // 20% redundancy
                        .enableRetransmission(true) // Enable retransmission
                        .maxRetransmissions(2) // Max 2 retransmissions
                        .build())
                .shapingBufferSize(128 * 1024) // 128KB shaping buffer
                .shapingQueueDepth(2000) // 2000 packet queue
                .backpressureStrategy(BackpressureStrategy.DROP_LOWEST)
                .enableDetailedMetrics(true)
                .metricsSamplingInterval(1000) // 1 second sampling
                .build();
    }

    private static QoSTunnelConfig createDatabaseReplicationTunnel() {
        return new QoSTunnelConfig.Builder(
                "db-repl-1", 5432, "db-replica.internal", 5432)
                .qosClass(QoSClass.INTERACTIVE)
                .bandwidthProfile(BandwidthProfile.builder()
                        .guaranteedBps(10_000_000L) // 10 Mbps guaranteed
                        .maximumBps(100_000_000L) // 100 Mbps maximum
                        .build())
                .latencyRequirements(LatencyRequirements.builder()
                        .maxLatencyMs(50) // 50ms max for database sync
                        .maxJitterMs(10) // 10ms max jitter
                        .build())
                .reliabilityRequirements(ReliabilityRequirements.builder()
                        .maxPacketLoss(0.0001) // 0.01% max packet loss
                        .enableRetransmission(true) // Critical for database
                        .maxRetransmissions(5) // Up to 5 retransmissions
                        .build())
                .shapingBufferSize(256 * 1024) // 256KB buffer
                .shapingQueueDepth(5000) // 5000 packet queue
                .backpressureStrategy(BackpressureStrategy.PAUSE_READER)
                .enableDetailedMetrics(true)
                .build();
    }

    private static QoSTunnelConfig createBackupTunnel() {
        return new QoSTunnelConfig.Builder(
                "backup-1", 445, "backup-server.internal", 445)
                .qosClass(QoSClass.BULK)
                .bandwidthProfile(BandwidthProfile.builder()
                        .maximumBps(1_000_000_000L) // 1 Gbps maximum
                        .build())
                .latencyRequirements(LatencyRequirements.builder()
                        .maxLatencyMs(1000) // 1 second max latency
                        .build())
                .shapingBufferSize(1_024 * 1024) // 1MB buffer
                .shapingQueueDepth(10000) // 10,000 packet queue
                .backpressureStrategy(BackpressureStrategy.DROP_NEWEST)
                .enableDetailedMetrics(false) // Don't need detailed metrics for backups
                .build();
    }

    private static QoSTunnelConfig createMonitoringTunnel() {
        return new QoSTunnelConfig.Builder(
                "monitoring-1", 9090, "monitoring.internal", 9090)
                .qosClass(QoSClass.BACKGROUND)
                .bandwidthProfile(BandwidthProfile.builder()
                        .maximumBps(1_000_000L) // 1 Mbps maximum
                        .build())
                .latencyRequirements(LatencyRequirements.builder()
                        .maxLatencyMs(5000) // 5 seconds max latency
                        .build())
                .shapingBufferSize(32 * 1024) // 32KB buffer
                .shapingQueueDepth(500) // 500 packet queue
                .backpressureStrategy(BackpressureStrategy.DROP_LOWEST)
                .enableDetailedMetrics(false)
                .build();
    }

    private static void displayTunnelConfig(QoSTunnelConfig config) {
        System.out.println("Tunnel: " + config.getStunnelId());
        System.out.println("  Endpoint: " + config.getSrcPort() + " -> " +
                config.getDstHost() + ":" + config.getDstPort());
        System.out.println("  QoS Class: " + config.getQosClass() +
                " (Priority: " + config.getQosClass().getPriority() +
                ", Weight: " + config.getQosClass().getWeight() + ")");

        BandwidthProfile bp = config.getBandwidthProfile();
        System.out.println("  Bandwidth: " +
                (bp.getGuaranteedBps() != null ? formatBandwidth(bp.getGuaranteedBps()) + " guaranteed, " : "") +
                formatBandwidth(bp.getMaximumBps()) + " maximum");

        LatencyRequirements lr = config.getLatencyRequirements();
        if (lr.getMaxLatencyMs() != null) {
            System.out.println("  Latency: ≤" + lr.getMaxLatencyMs() + "ms" +
                    (lr.getMaxJitterMs() != null ? ", Jitter: ≤" + lr.getMaxJitterMs() + "ms" : ""));
        }

        System.out.println("  Buffer/Queue: " + config.getShapingBufferSize() + " bytes / " +
                config.getShapingQueueDepth() + " packets");
        System.out.println("  Backpressure: " + config.getBackpressureStrategy());
        System.out.println();
    }

    private static void simulateResourceAllocation(List<QoSTunnelConfig> configs) {
        // Simulate how a QoS-aware scheduler would allocate resources
        long totalAvailableBandwidth = 10_000_000_000L; // 10 Gbps total
        long allocatedBandwidth = 0;

        System.out.println("Total available bandwidth: " + formatBandwidth(totalAvailableBandwidth));
        System.out.println("\nAllocating bandwidth by QoS class:\n");

        // Sort by QoS priority (higher priority first)
        configs.sort((a, b) -> Integer.compare(b.getQosClass().getPriority(), a.getQosClass().getPriority()));

        for (QoSTunnelConfig config : configs) {
            BandwidthProfile bp = config.getBandwidthProfile();
            Long guaranteed = bp.getGuaranteedBps();
            Long maximum = bp.getMaximumBps();

            // Calculate allocation based on QoS class and requirements
            long allocation = calculateAllocation(config, totalAvailableBandwidth, allocatedBandwidth);

            System.out.println(config.getStunnelId() + " (" + config.getQosClass() + "):");
            System.out.println("  Guaranteed: " + (guaranteed != null ? formatBandwidth(guaranteed) : "none"));
            System.out.println("  Maximum: " + (maximum != null ? formatBandwidth(maximum) : "unlimited"));
            System.out.println("  Allocated: " + formatBandwidth(allocation));

            allocatedBandwidth += allocation;
        }

        System.out.println("\nTotal allocated: " + formatBandwidth(allocatedBandwidth) +
                " (" + (allocatedBandwidth * 100 / totalAvailableBandwidth) + "% of total)");
    }

    private static long calculateAllocation(QoSTunnelConfig config,
            long totalAvailable, long alreadyAllocated) {
        BandwidthProfile bp = config.getBandwidthProfile();
        Long guaranteed = bp.getGuaranteedBps();
        Long maximum = bp.getMaximumBps();

        // Start with guaranteed if specified
        long allocation = (guaranteed != null) ? guaranteed : 0;

        // Add weighted share of remaining bandwidth based on QoS class weight
        long remaining = totalAvailable - alreadyAllocated;
        if (remaining > 0 && maximum == null) {
            // No maximum limit, allocate based on weight
            double weight = config.getQosClass().getWeight();
            allocation += (long) (remaining * weight / 100.0);
        } else if (remaining > 0 && maximum != null) {
            // Has maximum limit, allocate up to that limit
            long availableForThisTunnel = Math.min(remaining, maximum - allocation);
            if (availableForThisTunnel > 0) {
                allocation += availableForThisTunnel;
            }
        }

        return allocation;
    }

    private static void showLegacyIntegration(QoSTunnelConfig qosConfig) {
        System.out.println("Converting QoS configuration to legacy stunnel format:\n");

        // Convert to legacy configuration
        Map<String, String> legacyConfig = qosConfig.toLegacyConfig();

        System.out.println("Legacy configuration parameters:");
        for (Map.Entry<String, String> entry : legacyConfig.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }

        System.out.println("\nExample JMS message for creating tunnel with QoS:");
        System.out.println("{");
        System.out.println("  \"action\": \"configsrctunnel\",");
        System.out.println("  \"stunnel_id\": \"" + qosConfig.getStunnelId() + "\",");
        System.out.println("  \"src_port\": \"" + qosConfig.getSrcPort() + "\",");
        System.out.println("  \"dst_host\": \"" + qosConfig.getDstHost() + "\",");
        System.out.println("  \"dst_port\": \"" + qosConfig.getDstPort() + "\",");
        System.out.println("  \"qos_class\": \"" + qosConfig.getQosClass().name() + "\",");
        System.out.println("  \"max_bandwidth_bps\": \"" +
                qosConfig.getBandwidthProfile().getMaximumBps() + "\",");
        System.out.println("  \"max_latency_ms\": \"" +
                qosConfig.getLatencyRequirements().getMaxLatencyMs() + "\"");
        System.out.println("}");

        System.out.println("\nThe QoS-aware PluginExecutor would:");
        System.out.println("1. Parse QoS parameters from the message");
        System.out.println("2. Create QoSTunnelConfig object");
        System.out.println("3. Configure traffic shaping handlers");
        System.out.println("4. Set up QoS monitoring");
        System.out.println("5. Create the tunnel with QoS enforcement");
    }

    private static String formatBandwidth(long bps) {
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
