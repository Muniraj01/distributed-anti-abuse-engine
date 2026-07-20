package com.darle.engine.controller;

import com.darle.engine.domain.RateLimitResult;
import com.darle.engine.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/validate")
@RequiredArgsConstructor
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    /**
     * Non-blocking endpoint to evaluate incoming client traffic metrics.
     * Returns HTTP 200 OK if allowed, or HTTP 429 Too Many Requests if rate-limited.
     */
    @GetMapping
    public Mono<ResponseEntity<RateLimitResult>> validateRateLimit(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Client-Identifier") String clientIdentifier,
            @RequestParam(defaultValue = "100") long capacity,
            @RequestParam(defaultValue = "10") long refillRate) {
        
        return rateLimiterService.evaluate(tenantId, clientIdentifier, capacity, refillRate)
                .map(result -> {
                    if (result.isAllowed()) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
                    }
                });
    }
}
