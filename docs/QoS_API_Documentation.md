# QoS API Documentation for Stunnel

## Overview

The Quality of Service (QoS) system for stunnel provides traffic management capabilities for TCP tunnels in the Cresco agent framework. It enables:

- **Traffic Shaping**: Rate limiting and bandwidth control
- **Priority Scheduling**: Differentiated service based on QoS classes
- **Latency Management**: Enforcing latency and jitter requirements
- **Reliability Features**: Packet loss mitigation and error correction
- **Backpressure Handling**: Configurable strategies for congestion

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    QoS System Architecture               │
├─────────────────────────────────────────────────────────┤
│  Application Layer                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Plugin    │  │   Plugin    │  │   Plugin    │    │
│  │  Executor   │  │  Executor   │  │  Executor   │    │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │
│         │                 │                 │           │
├─────────┼─────────────────┼─────────────────┼───────────┤
│  QoS Configuration Layer                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │QoSTunnelConfig│ │QoSUtils    │  │QoS Classes  │    │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │
│         │                 │                 │           │
├─────────┼─────────────────┼─────────────────┼───────────┤
│  Traffic Shaping Layer                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │TokenBucket  │  │Traffic      │  │QoS Handler  │    │
│  │RateLimiter  │  │Shaping      │  │(Netty)      │    │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │
│         │                 │                 │           │
├─────────┼─────────────────┼─────────────────┼───────────┤
│  Network Layer                                           │
│  ┌────────────────────────────────────────────────────┐ │
│  │                Netty TCP Tunnel                    │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Core Components

### 1. QoS Classes

Four predefined QoS classes with different priority levels:

| Class | Priority | Weight | Description | Typical Use Cases |
|-------|----------|--------|-------------|-------------------|
| `REAL_TIME` | 100 | 40 | Lowest latency, highest priority | Video conferencing, VoIP, gaming |
| `INTERACTIVE` | 75 | 30 | Low latency, high priority | Database queries, SSH, remote desktop |
| `BULK` | 50 | 20 | High throughput, medium priority | File transfers, backups, downloads |
| `BACKGROUND` | 25 | 10 | Best effort, lowest priority | Logging, monitoring, updates |

### 2. Bandwidth Profiles

Control bandwidth allocation with guaranteed and maximum limits:

```java
BandwidthProfile profile = BandwidthProfile.builder()
    .guaranteedBps(10_000_000L)   // 10 Mbps guaranteed
    .maximumBps(100_000_000L)     // 100 Mbps maximum
    .burstBps(150_000_000L)       // 150 Mbps burst capacity
    .burstDurationMs(1000)        // 1 second burst duration
    .build();
```

### 3. Latency Requirements

Define latency and jitter constraints:

```java
LatencyRequirements latency = LatencyRequirements.builder()
    .maxLatencyMs(100)            // 100ms maximum latency
    .maxJitterMs(20)              // 20ms maximum jitter
    .maxReorder(10)               // Max 10 packets out of order
    .build();
```

### 4. Reliability Requirements

Configure packet loss mitigation and error correction:

```java
ReliabilityRequirements reliability = ReliabilityRequirements.builder()
    .maxPacketLoss(0.001)         // 0.1% maximum packet loss
    .enableFec(true)              // Enable forward error correction
    .fecRedundancy(20)            // 20% redundancy
    .enableRetransmission(true)   // Enable packet retransmission
    .maxRetransmissions(3)        // Max 3 retransmission attempts
    .build();
```

### 5. Backpressure Strategies

Choose how to handle congestion:

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `DROP_LOWEST` | Drop lowest priority packets first | Mixed traffic with priorities |
| `DROP_NEWEST` | Drop newest packets first | Real-time streams |
| `PAUSE_READER` | Pause reading from source | Critical data integrity |
| `BLOCK_WRITER` | Block writing until buffer clears | Memory-constrained systems |
| `NOTIFY_ONLY` | Only notify, don't drop | Monitoring/debugging |

## Configuration

### Creating a QoS Tunnel Configuration

```java
QoSTunnelConfig config = new QoSTunnelConfig.Builder(
        "video-tunnel-1", 1935, "video-server.internal", 1935)
    .qosClass(QoSClass.REAL_TIME)
    .bandwidthProfile(BandwidthProfile.builder()
        .guaranteedBps(5_000_000L)
        .maximumBps(10_000_000L)
        .build())
    .latencyRequirements(LatencyRequirements.builder()
        .maxLatencyMs(150)
        .build())
    .shapingBufferSize(64 * 1024)     // 64KB buffer
    .shapingQueueDepth(1000)          // 1000 packet queue
    .backpressureStrategy(BackpressureStrategy.DROP_LOWEST)
    .enableDetailedMetrics(true)
    .build();
```

### Legacy Configuration Format

The QoS system maintains backward compatibility with existing stunnel configuration:

```java
// Convert to legacy format for existing PluginExecutor
Map<String, String> legacyConfig = config.toLegacyConfig();

// Legacy config includes both original and QoS parameters:
// stunnel_id=video-tunnel-1
// src_port=1935
// dst_host=video-server.internal
// dst_port=1935
// qos_class=REAL_TIME
// max_bandwidth_bps=10000000
// max_latency_ms=150
// ...
```

## Integration with Existing Stunnel

### 1. Extending PluginExecutor

Create a QoS-aware executor that handles QoS parameters:

```java
public class QoSAwarePluginExecutor extends PluginExecutor {
    @Override
    public MsgEvent executeCONFIG(MsgEvent incoming) {
        String action = incoming.getParam("action");
        
        if ("configqostunnel".equals(action)) {
            return configQoSTunnel(incoming);
        }
        
        // Handle existing actions with QoS parameters
        if (QoSUtils.hasQoSParameters(incoming.getParams())) {
            return handleQoSTunnel(incoming);
        }
        
        return super.executeCONFIG(incoming);
    }
    
    private MsgEvent configQoSTunnel(MsgEvent incoming) {
        // Parse and apply QoS configuration
        QoSTunnelConfig qosConfig = QoSUtils.extractQoSConfig(incoming.getParams());
        
        // Apply traffic shaping
        configureTrafficShaping(qosConfig);
        
        // Create tunnel with QoS
        return createQoSTunnel(qosConfig);
    }
}
```

### 2. Traffic Shaping Integration

Integrate with Netty pipeline:

```java
public class QoSSrcChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final QoSTunnelConfig qosConfig;
    
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        
        // Add QoS traffic shaping handler
        if (qosConfig.getBandwidthProfile().shouldLimitBandwidth()) {
            TokenBucketRateLimiter limiter = createRateLimiter(qosConfig);
            pipeline.addLast("qosTrafficShaper", 
                new QoSTrafficShapingHandler(limiter, qosConfig));
        }
        
        // Add existing handlers
        pipeline.addLast("handler", new SrcHandler());
    }
}
```

## API Reference

### QoSUtils Class

Utility methods for QoS operations:

| Method | Description |
|--------|-------------|
| `validateQoSParameters(Map<String, String> params)` | Validate QoS parameters |
| `extractQoSConfig(Map<String, String> params)` | Extract QoS config from parameters |
| `hasQoSParameters(Map<String, String> params)` | Check if parameters contain QoS settings |
| `describeQoSConfig(Map<String, String> params)` | Human-readable description of QoS config |
| `calculateOptimalBufferSize(long bps, int latencyMs)` | Calculate optimal buffer size |
| `calculateOptimalQueueDepth(long bps, int packetSize)` | Calculate optimal queue depth |

### TokenBucketRateLimiter Class

Thread-safe token bucket implementation:

```java
// Create a rate limiter for 1 MB/s with 2 MB burst
TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
    1_000_000L,   // 1 MB/s
    2_000_000L);  // 2 MB burst

// Check if bytes can be sent
if (limiter.tryConsume(500_000L)) {
    // Send 500KB
}

// Get current available tokens
long available = limiter.getAvailableTokens();

// Reset the limiter
limiter.reset();
```

## Usage Examples

### Example 1: Video Conferencing Tunnel

```java
// High-priority video stream
QoSTunnelConfig videoConfig = new QoSTunnelConfig.Builder(
        "video-conf", 1935, "meet.example.com", 1935)
    .qosClass(QoSClass.REAL_TIME)
    .bandwidthProfile(BandwidthProfile.builder()
        .guaranteedBps(4_000_000L)   // 4 Mbps for 720p
        .maximumBps(8_000_000L)      // 8 Mbps for 1080p
        .build())
    .latencyRequirements(LatencyRequirements.builder()
        .maxLatencyMs(150)           // 150ms for interactive
        .maxJitterMs(30)             // 30ms jitter
        .build())
    .build();
```

### Example 2: Database Replication

```java
// Critical database sync
QoSTunnelConfig dbConfig = new QoSTunnelConfig.Builder(
        "db-repl", 5432, "db-replica.internal", 5432)
    .qosClass(QoSClass.INTERACTIVE)
    .bandwidthProfile(BandwidthProfile.builder()
        .guaranteedBps(10_000_000L)  // 10 Mbps guaranteed
        .maximumBps(100_000_000L)    // 100 Mbps maximum
        .build())
    .reliabilityRequirements(ReliabilityRequirements.builder()
        .maxPacketLoss(0.0001)       // 0.01% max loss
        .enableRetransmission(true)  // Critical for DB
        .build())
    .backpressureStrategy(BackpressureStrategy.PAUSE_READER)
    .build();
```

### Example 3: Background Backup

```java
// Low-priority backup
QoSTunnelConfig backupConfig = new QoSTunnelConfig.Builder(
        "backup", 445, "backup-server.internal", 445)
    .qosClass(QoSClass.BACKGROUND)
    .bandwidthProfile(BandwidthProfile.builder()
        .maximumBps(50_000_000L)     // 50 Mbps maximum
        .build())
    .latencyRequirements(LatencyRequirements.builder()
        .maxLatencyMs(5000)          // 5 seconds OK
        .build())
    .backpressureStrategy(BackpressureStrategy.DROP_NEWEST)
    .build();
```

## JMS Message Format

### Creating a QoS Tunnel

```json
{
  "action": "configsrctunnel",
  "stunnel_id": "video-tunnel-1",
  "src_port": "1935",
  "dst_host": "video-server.internal",
  "dst_port": "1935",
  "qos_class": "REAL_TIME",
  "max_bandwidth_bps": "10000000",
  "max_latency_ms": "150",
  "guaranteed_bandwidth_bps": "5000000",
  "backpressure_strategy": "DROP_LOWEST",
  "enable_detailed_metrics": "true"
}
```

### QoS-Specific Action

```json
{
  "action": "configqostunnel",
  "qos_config": {
    "stunnel_id": "video-tunnel-1",
    "src_port": 1935,
    "dst_host": "video-server.internal",
    "dst_port": 1935,
    "qos_class": "REAL_TIME",
    "bandwidth_profile": {
      "guaranteed_bps": 5000000,
      "maximum_bps": 10000000
    },
    "latency_requirements": {
      "max_latency_ms": 150,
      "max_jitter_ms": 30
    }
  }
}
```

## Monitoring and Metrics

The QoS system provides detailed metrics:

### Available Metrics

- **Bandwidth Utilization**: Current/peak/average bandwidth
- **Latency Statistics**: Min/max/average latency and jitter
- **Packet Loss**: Loss rate and retransmission count
- **Queue Statistics**: Buffer usage and drop counts
- **Token Bucket**: Token consumption and wait times

### Integration with PerformanceMonitor

```java
// Extend existing PerformanceMonitor with QoS metrics
public class QoSPerformanceMonitor extends PerformanceMonitor {
    private final QoSTunnelConfig qosConfig;
    private final AtomicLong bytesShaped = new AtomicLong();
    private final AtomicLong packetsDropped = new AtomicLong();
    
    public void recordShapedBytes(long bytes) {
        bytesShaped.addAndGet(bytes);
        // Update metrics
    }
    
    public void recordPacketDrop(QoSPacket packet) {
        packetsDropped.incrementAndGet();
        // Log drop reason
    }
}
```

## Best Practices

### 1. Buffer Sizing

- **Real-time traffic**: Smaller buffers (32-128KB) to minimize latency
- **Bulk transfers**: Larger buffers (256KB-1MB) to maximize throughput
- **Interactive traffic**: Medium buffers (64-256KB) for balance

### 2. Queue Depth

- Calculate based on bandwidth-delay product:
  ```java
  // Example: 100ms RTT, 10 Mbps = 125KB bandwidth-delay product
  int optimalQueue = (int)((bandwidth_bps * latency_seconds) / 8 / packet_size);
  ```

### 3. QoS Class Selection

- Use `REAL_TIME` for latency-sensitive applications (<200ms)
- Use `INTERACTIVE` for user-facing applications (<500ms)
- Use `BULK` for data transfers where throughput matters most
- Use `BACKGROUND` for non-critical operations

### 4. Bandwidth Allocation

- Set realistic maximums based on network capacity
- Use guaranteed bandwidth for critical applications
- Allow bursts for variable-rate traffic
- Monitor and adjust based on actual usage

## Troubleshooting

### Common Issues

1. **High Latency with QoS Enabled**
   - Check buffer sizes (may be too large)
   - Verify QoS class matches traffic type
   - Monitor queue depths for congestion

2. **Low Throughput**
   - Check bandwidth limits are not too restrictive
   - Verify token bucket parameters
   - Check for packet drops in backpressure strategy

3. **Packet Loss**
   - Adjust reliability requirements
   - Enable FEC or retransmission
   - Consider different backpressure strategy

### Debugging Commands

```bash
# Check QoS configuration
curl -X POST http://agent:8080/config -d '{"action":"getqosconfig","stunnel_id":"tunnel-1"}'

# Get QoS statistics
curl -X POST http://agent:8080/config -d '{"action":"getqosstats","stunnel_id":"tunnel-1"}'

# Update QoS parameters
curl -X POST http://agent:8080/config -d '{"action":"updateqos","stunnel_id":"tunnel-1","max_bandwidth_bps":"5000000"}'
```

## Migration Guide

### From Legacy to QoS-Enabled

1. **Phase 1: Configuration Compatibility**
   - Existing tunnels work unchanged
   - New QoS parameters ignored by legacy code

2. **Phase 2: QoS-Aware PluginExecutor**
   - Deploy new executor alongside existing
   - Gradually migrate tunnels to QoS configuration

3. **Phase 3: Full QoS Integration**
   - All new tunnels use QoS
   - Legacy tunnels can be upgraded incrementally

### Backward Compatibility

- All existing JMS messages continue to work
- QoS parameters are optional additions
- Missing QoS parameters use sensible defaults
- Legacy configuration format fully supported

## Future Enhancements

### Planned Features

1. **Dynamic QoS Adjustment**
   - Automatic bandwidth adjustment based on network conditions
   - QoS class promotion/demotion based on application needs

2. **Cross-Tunnel Coordination**
   - Global bandwidth allocation across multiple tunnels
   - Priority inheritance between related tunnels

3. **Advanced Traffic Shaping**
   - Hierarchical token buckets
   - Time-based rate limiting schedules
   - Application-aware shaping

4. **Integration with SDN**
   - Coordinate with network switches/routers
   - End-to-end QoS across network segments

## Conclusion

The QoS system provides a comprehensive solution for traffic management in stunnel, enabling differentiated service for various types of network traffic while maintaining full backward compatibility with existing deployments.
