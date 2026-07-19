# 🛡️ Distributed Anti-Abuse & Rate Limiting Engine (DARLE)

A highly available, low-latency ($<3\text{ms}$ p99), multi-region distributed rate limiting system designed to protect tier-1 microservices from abuse, DDoS attacks, and API resource exhaustion at a scale of 100,000+ QPS.

---

## 1. Architectural Overview & Core Specifications

DARLE implements a hybrid **Token Bucket / Sliding Window Log** algorithm evaluated at the edge via API Gateways, backed by a globally distributed Redis Cluster cache layer with multi-region active-active replication via an asynchronous message bus (Apache Kafka).

### Core Performance Metrics (SLAs)
*   **Write/Evaluation Latency:** $<3\text{ms}$ at p99 inside the local availability zone.
*   **Availability:** $99.999\%$ uptime guarantee via local hard-fail open/close failover mechanics.
*   **Consistency:** Eventual consistency across global regions (converging within $<500\text{ms}$); sequential consistency within the local region.

### System Scope & Edge Cases Handled
*   **Thundering Herd Mitigation:** Cached token evaluation utilizes atomic Lua execution primitives to prevent race conditions during high-concurrency bursts.
*   **Regional Isolation Resiliency:** If a region is completely cut off from the global control plane, nodes degrade to local atomic counters to prevent system-wide outages.

---

## 2. Global Component Topology

```mermaid
graph TD
    User[Global Client Traffic] -->|Geo-DNS / Anycast| Edge[Edge Network / Cloudflare Worker]
    Edge -->|Local Low-Latency Route| App[Regional API Gateway / Envoy]
    App -->|Atomic Lua Check| Redis[(Local Redis Cluster Layer)]
    Redis -.->|Async Sync / Outbox Pattern| Kafka{Apache Kafka Mesh}
    Kafka -.->|Cross-Region Replication| RemoteRedis[(Remote Region Redis)]
    
    style Redis fill:#f9f,stroke:#333,stroke-width:2px
    style Kafka fill:#bbf,stroke:#333,stroke-width:2px
