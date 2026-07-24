package com.darle.engine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenSyncEvent {
    private String tenantId;
    private String clientIdentifier;
    private long tokensConsumed;
    private long timestamp;
    private String originRegion;
}
