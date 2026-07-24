package com.darle.engine.service;

import com.darle.engine.domain.TokenSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "global-rate-limit-sync";

    /**
     * Publishes a token consumption event asynchronously to the global cluster mesh.
     */
    public void publishSyncEvent(String tenantId, String clientIdentifier, long tokensConsumed, String currentRegion) {
        TokenSyncEvent event = TokenSyncEvent.builder()
                .tenantId(tenantId)
                .clientIdentifier(clientIdentifier)
                .tokensConsumed(tokensConsumed)
                .timestamp(Instant.now().getEpochSecond())
                .originRegion(currentRegion)
                .build();

        String partitionKey = String.format("%s:%s", tenantId, clientIdentifier);

        // Standard asynchronous callback execution to prevent thread stalling
        this.kafkaTemplate.send(TOPIC, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to route sync topology event to partition cluster for key: {}", partitionKey, ex);
                    } else {
                        log.debug("Dispatched cross-region telemetry update to partition: {}", result.getRecordMetadata().partition());
                    }
                });
    }
}
