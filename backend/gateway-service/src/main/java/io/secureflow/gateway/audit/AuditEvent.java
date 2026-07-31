/*
 * AuditEvent — Payload pubblicato su Kafka dopo ogni richiesta gateway.
 */
package io.secureflow.gateway.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        String id,
        UUID tenantId,
        String keyPrefix,
        String method,
        String path,
        int status,
        long durationMs,
        String outcome,
        Instant occurredAt
) {
    public static AuditEvent of(UUID tenantId, String keyPrefix, String method, String path,
                                int status, long durationMs, String outcome) {
        return new AuditEvent(
                UUID.randomUUID().toString(),
                tenantId,
                keyPrefix,
                method,
                path,
                status,
                durationMs,
                outcome,
                Instant.now()
        );
    }
}
