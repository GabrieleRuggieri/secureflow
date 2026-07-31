/*
 * AuditEventDto — Payload REST/SSE e messaggio Kafka deserializzato.
 */
package io.secureflow.core.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventDto(
        UUID id,
        String eventId,
        UUID tenantId,
        String keyPrefix,
        String method,
        String path,
        int status,
        long durationMs,
        String outcome,
        Instant occurredAt
) {
    /**
     * Forma del messaggio pubblicato dal gateway su Kafka (senza id DB).
     * Campi allineati a io.secureflow.gateway.audit.AuditEvent.
     */
    public record Message(
            String id,
            UUID tenantId,
            String keyPrefix,
            String method,
            String path,
            int status,
            long durationMs,
            String outcome,
            Instant occurredAt
    ) {}
}
