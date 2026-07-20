package com.darle.engine.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RateLimitResult {
    boolean allowed;
    long remainingTokens;
    String tenantId;
    String clientIdentifier;
}
