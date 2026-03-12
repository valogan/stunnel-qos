package io.cresco.stunnel.qos.example;

import io.cresco.stunnel.qos.config.*;
import io.cresco.stunnel.qos.shaping.TokenBucketRateLimiter;
import io.cresco.stunnel.qos.QoSUtils;

import java.util.Map;
import java.util.HashMap;

/**
 * Simple test example demonstrating the QoS system functionality.
 * This shows how to create and use QoS configurations.
 */
public class QoSTestExample {

    public static void main(String[] args) {
        System.out.println("=== QoS System Test Example ===\n");

        // Test 1: Create a QoS configuration for a real-time tunnel
        testRealTimeQoS();

        // Test 2: Create a QoS configuration for a bulk data transfer
        testBulkTransferQoS();

        // Test 3: Test traffic shaping with token bucket
        testTrafficShaping();

        // Test 4: Test QoS parameter validation
        testQoSParameterValidation();

        System.out.println("\n=== All tests completed ===");
    }

    private static void testRealTimeQoS() {
        System.out.println("Test 1: Real-time QoS Configuration");

        // Create a QoS configuration for real-time traffic (e.g., video streaming)
        QoSTunnelConfig realTimeConfig = new QoSTunnelConfig.Builder(
                "rt-video-tunnel-1", 8080, "192.168.1.100", 1935)
                .qosClass(QoSClass.REAL_TIME)
                .bandwidthProfile(BandwidthProfile.builder()
                        .guaranteedBps(5_000_000L) // 5 Mbps guaranteed
                        .maximumBps(10_000_000L) // 10 Mbps maximum
                        .burstBps(15_000_000L) // 15 Mbps burst
                        .build())
                .latencyRequirements(LatencyRequirements.builder()
                        .maxLatencyMs(100) // 100ms max latency
                        .maxJitterMs(20) // 20ms max jitter
                        .build())
                .reliabilityRequirements(ReliabilityRequirements.builder()
                        .maxPacketLoss(0.001) // 0.1% max packet loss
                        .enableRetransmission(true)
                        .maxRetransmissions(3)
                        .build())
                .shapingBufferSize(64 * 1024) // 64KB buffer
                .shapingQueueDepth(1000) // 1000 packet queue
                .backpressureStrategy(BackpressureStrategy.DROP_LOWEST)
                .enableDetailedMetrics(true)
                .build();

        System.out.println("  Created real-time tunnel configuration:");
        System.out.println("    Tunnel ID: " + realTimeConfig.getStunnelId());
        System.out.println("    QoS Class: " + realTimeConfig.getQosClass());
        System.out.println("    Bandwidth: " + realTimeConfig.getBandwidthProfile());
        System.out.println("    Latency: " + realTimeConfig.getLatencyRequirements());
        System.out.println("    Buffer: " + realTimeConfig.getShapingBufferSize() + " bytes");
        System.out.println("    Queue: " + realTimeConfig.getShapingQueueDepth() + " packets");

        // Convert to legacy configuration format
        Map<String, String> legacyConfig = realTimeConfig.toLegacyConfig();
        System.out.println("  Legacy config keys: " + legacyConfig.keySet().size());

        // Validate the configuration
        boolean isValid = QoSUtils.validateQoSParameters(legacyConfig);
        System.out.println("  Configuration valid: " + isValid);

        System.out.println();
    }

    private static void testBulkTransferQoS() {
        System.out.println("Test 2: Bulk Transfer QoS Configuration");

        // Create a QoS configuration for bulk data transfer
        QoSTunnelConfig bulkConfig = new QoSTunnelConfig.Builder(
                "bulk-backup-tunnel-1", 9000, "10.0.0.50", 445)
                .qosClass(QoSClass.BULK)
                .bandwidthProfile(BandwidthProfile.builder()
                        .maximumBps(100_000_000L) // 100 Mbps maximum
                        .build())
                .latencyRequirements(LatencyRequirements.builder()
                        .maxLatencyMs(1000) // 1 second max latency
                        .build())
                .shapingBufferSize(256 * 1024) // 256KB buffer
                .shapingQueueDepth(5000) // 5000 packet queue
                .backpressureStrategy(BackpressureStrategy.PAUSE_READER)
                .build();

        System.out.println("  Created bulk transfer configuration:");
        System.out.println("    Tunnel ID: " + bulkConfig.getStunnelId());
        System.out.println("    QoS Class: " + bulkConfig.getQosClass());
        System.out.println("    Priority: " + bulkConfig.getQosClass().getPriority());
        System.out.println("    Weight: " + bulkConfig.getQosClass().getWeight());

        // Test bandwidth limiting
        BandwidthProfile profile = bulkConfig.getBandwidthProfile();
        System.out.println("    Should limit bandwidth: " + profile.shouldLimitBandwidth());
        System.out.println("    Maximum BPS: " + profile.getMaximumBps());
        System.out.println("    Effective burst: " + profile.getEffectiveBurstBps());

        System.out.println();
    }

    private static void testTrafficShaping() {
        System.out.println("Test 3: Traffic Shaping with Token Bucket");

        // Create a token bucket rate limiter for 1 MB/s with 2 MB burst
        long rateBytesPerSecond = 1_000_000L; // 1 MB/s
        long burstBytes = 2_000_000L; // 2 MB burst

        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                rateBytesPerSecond, burstBytes);

        System.out.println("  Created token bucket limiter:");
        System.out.println(
                "    Rate: " + rateBytesPerSecond + " B/s (" + (rateBytesPerSecond * 8 / 1_000_000) + " Mbps)");
        System.out.println("    Burst: " + burstBytes + " bytes");
        System.out.println("    Available tokens: " + limiter.getAvailableTokens());

        // Simulate consuming tokens
        long bytesToSend = 500_000L; // 500KB
        boolean allowed = limiter.tryConsume(bytesToSend);

        System.out.println("  Trying to send " + bytesToSend + " bytes: " + (allowed ? "ALLOWED" : "DENIED"));
        System.out.println("  Remaining tokens: " + limiter.getAvailableTokens());

        // Try to send more than available
        bytesToSend = 2_000_000L; // 2MB
        allowed = limiter.tryConsume(bytesToSend);
        System.out.println("  Trying to send " + bytesToSend + " bytes: " + (allowed ? "ALLOWED" : "DENIED"));

        // Wait for tokens to refill
        try {
            Thread.sleep(1100); // Wait 1.1 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  After 1.1 seconds, available tokens: " + limiter.getAvailableTokens());

        System.out.println();
    }

    private static void testQoSParameterValidation() {
        System.out.println("Test 4: QoS Parameter Validation");

        // Create a test configuration map
        Map<String, String> testConfig = new HashMap<>();
        testConfig.put("stunnel_id", "test-tunnel-1");
        testConfig.put("src_port", "8080");
        testConfig.put("dst_host", "192.168.1.100");
        testConfig.put("dst_port", "80");
        testConfig.put("qos_class", "INTERACTIVE");
        testConfig.put("max_bandwidth_bps", "10000000"); // 10 Mbps
        testConfig.put("max_latency_ms", "200");

        System.out.println("  Test configuration:");
        for (Map.Entry<String, String> entry : testConfig.entrySet()) {
            System.out.println("    " + entry.getKey() + " = " + entry.getValue());
        }

        // Validate the configuration
        boolean isValid = QoSUtils.validateQoSParameters(testConfig);
        System.out.println("  Configuration valid: " + isValid);

        // Extract QoS configuration
        QoSTunnelConfig extracted = QoSUtils.extractQoSConfig(testConfig);
        if (extracted != null) {
            System.out.println("  Successfully extracted QoS config:");
            System.out.println("    QoS Class: " + extracted.getQosClass());
            System.out.println("    Max Bandwidth: " + extracted.getBandwidthProfile().getMaximumBps());
        }

        // Test with invalid configuration
        Map<String, String> invalidConfig = new HashMap<>(testConfig);
        invalidConfig.put("max_bandwidth_bps", "not-a-number");

        boolean invalidIsValid = QoSUtils.validateQoSParameters(invalidConfig);
        System.out.println("  Invalid config validation: " + invalidIsValid);

        System.out.println();
    }
}
