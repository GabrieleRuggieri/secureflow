/*
 * AuditEventFilterTest — Matching tenant/outcome/intervallo per SSE e query.
 */
package io.secureflow.core;

import io.secureflow.core.audit.AuditEventFilter;
import io.secureflow.core.dto.AuditEventDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventFilterTest {

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @Test
    void matches_filtersTenantOutcomeAndTimeWindow() {
        AuditEventFilter filter = AuditEventFilter.of(
                tenantA,
                "success",
                Instant.parse("2026-07-31T10:00:00Z"),
                Instant.parse("2026-07-31T12:00:00Z")
        );

        assertThat(filter.matches(event(tenantA, "success", Instant.parse("2026-07-31T11:00:00Z")))).isTrue();
        assertThat(filter.matches(event(tenantA, "denied", Instant.parse("2026-07-31T11:00:00Z")))).isFalse();
        assertThat(filter.matches(event(tenantB, "success", Instant.parse("2026-07-31T11:00:00Z")))).isFalse();
        assertThat(filter.matches(event(tenantA, "success", Instant.parse("2026-07-31T09:00:00Z")))).isFalse();
        assertThat(filter.matches(event(tenantA, "success", Instant.parse("2026-07-31T13:00:00Z")))).isFalse();
    }

    @Test
    void matches_nullFiltersAcceptAllForTenant() {
        AuditEventFilter filter = AuditEventFilter.of(tenantA, null, null, null);
        assertThat(filter.matches(event(tenantA, "error", Instant.parse("2026-07-31T11:00:00Z")))).isTrue();
        assertThat(filter.matches(event(tenantB, "error", Instant.parse("2026-07-31T11:00:00Z")))).isFalse();
    }

    private AuditEventDto event(UUID tenantId, String outcome, Instant at) {
        return new AuditEventDto(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                tenantId,
                "sf_live_",
                "GET",
                "/api/x",
                200,
                5L,
                outcome,
                at
        );
    }
}
