package com.darle.engine.service;

import com.darle.engine.domain.RateLimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.reactive.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimiterScript;

    /**
     * Evaluates rate limiting parameters atomically using Reactive Redis.
     *
     * @param tenantId         The identifier for the API owner
     * @param clientIdentifier Unique client footprint (e.g., API token, IP hash)
     * @param capacity         Max bucket depth allowed
     * @param refillRate       Tokens replenished per second
     * @return Mono encapsulating the reactive evaluation outcome
     */
    public Mono<RateLimitResult> evaluate(String tenantId, String clientIdentifier, long capacity, long refillRate) {
        // Construct the tracking namespace key
        String cacheKey = String.format("rl:%s:%s", tenantId, clientIdentifier);
        List<String> keys = Collections.singletonList(cacheKey);

        String now = String.valueOf(Instant.now().getEpochSecond());
        String requestedTokens = "1"; // Base standard increment per API invocation

        List<String> args = List.of(
                String.valueOf(capacity),
                String.valueOf(refillRate),
                now,
                requestedTokens
        );

        return redisTemplate.execute(rateLimiterScript, keys, args)
                .next() // Extract the singular Long response stream emitted from Redis
                .map(result -> RateLimitResult.builder()
                        .allowed(result == 1L)
                        .tenantId(tenantId)
                        .clientIdentifier(clientIdentifier)
                        .build())
                .onErrorResume(ex -> {
                    log.error("Cache degradation encountered during evaluation for key: {}", cacheKey, ex);
                    // Critical Staff Engineer Decision: Fail Open strategy during infrastructure drop
                    return Mono.just(RateLimitResult.builder()
                            .allowed(true)
                            .tenantId(tenantId)
                            .clientIdentifier(clientIdentifier)
                            .build());
                });
    }
}
