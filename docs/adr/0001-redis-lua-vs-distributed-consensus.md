# ADR 1: Local Redis Clusters with Lua Scripts vs. Distributed Consensus (Raft)

## Status
Accepted

## Context
We require an engine capable of evaluating rate limits globally at 100K+ QPS. The system must guarantee a strict <3ms p99 write/evaluation latency within a local Availability Zone while remaining resilient to network partitions between continental data centers.

## Decision
We choose a hybrid model using localized Redis Clusters paired with atomic Lua scripts for execution, coupled with asymmetric outbox replication via Apache Kafka for cross-region synchronization. We explicitly reject Raft-based distributed consensus systems (e.g., CockroachDB, custom Raft layers) for the hot data path.

## Consequences
* **Pros:** Achieves sub-millisecond local execution primitives via single-threaded Lua evaluations. No distributed lock overhead.
* **Cons:** We sacrifice strict global linearizability. The system accepts bounded eventual consistency (converging globally <500ms) across regions. In a split-brain scenario, local regions will degrade cleanly to localized traffic caps.
