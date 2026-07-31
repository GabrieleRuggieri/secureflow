/*
 * AuditEventFilter — Criteri di matching per lista/SSE (outcome, intervallo temporale).
 */
package io.secureflow.core.audit;

import io.secureflow.core.dto.AuditEventDto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventFilter(UUID tenantId, String outcome, Instant from, Instant to) {

    public static AuditEventFilter of(UUID tenantId, String outcome, Instant from, Instant to) {
        String normalized = outcome == null || outcome.isBlank() ? null : outcome;
        return new AuditEventFilter(tenantId, normalized, from, to);
    }

    public boolean matches(AuditEventDto event) {
        if (tenantId != null && !tenantId.equals(event.tenantId())) {
            return false;
        }
        if (outcome != null && !outcome.equals(event.outcome())) {
            return false;
        }
        if (from != null && event.occurredAt().isBefore(from)) {
            return false;
        }
        if (to != null && event.occurredAt().isAfter(to)) {
            return false;
        }
        return true;
    }
}
