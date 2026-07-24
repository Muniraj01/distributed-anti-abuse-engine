package com.darle.engine.service;

import com.darle.engine.domain.TokenSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.reactive.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private static final String CURRENT_REGION = "us-east-1"; // Extracted via infra env at runtime

    /**
     * Consumes global synchronization matrix updates.
     * Deducts tokens from the local cache if the event originated from a remote data center.
     */
    @KafkaListener(topics = "global-rate-limit-sync", groupId = "darle-sync-consumers")
    public void consumeSyncMetrics(TokenSyncEvent event) {
        // Skip echo events matching our own origin space
        if (CURRENT_REGION.equalsIgnoreCase(event.getOriginRegion())) {
            return;
        }

        String cacheKey = String.format("rl:%s:%s", event.getTenantId(), event.getClientIdentifier());
        
        // Atomically decrement remote token usage on local Redis cluster
        redisTemplate.opsForHash().increment(cacheKey, "tokens", -event.getTokensConsumed())
                .doOnSuccess(updatedVal -> log.debug("Synchronized cross-region states for {}. Updated token threshold baseline.", cacheKey))
                .doOnError(err -> log.error("Mesh adjustment failed for cache storage target: {}", cacheKey, err))
                .subscribe();
    }
}
