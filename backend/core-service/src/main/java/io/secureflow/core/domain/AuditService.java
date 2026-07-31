/*
 * AuditService — Persistenza idempotente e query filtrate degli eventi di audit.
 *
 * Dopo il save pubblica AuditEventPersistedEvent per fan-out SSE.
 * TenantContext deve essere già impostato (listener Kafka o TenantContextFilter HTTP).
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.AuditEventDto;
import io.secureflow.core.entity.AuditEvent;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.repository.AuditEventRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuditEventDto persist(AuditEventDto.Message message) {
        if (message == null || message.id() == null || message.tenantId() == null) {
            throw new IllegalArgumentException("Audit event id and tenantId are required");
        }

        return auditEventRepository.findByEventId(message.id())
                .map(this::toDto)
                .orElseGet(() -> saveNew(message));
    }

    private AuditEventDto saveNew(AuditEventDto.Message message) {
        Tenant tenant = tenantRepository.findById(message.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + message.tenantId()));

        AuditEvent entity = new AuditEvent();
        entity.setEventId(message.id());
        entity.setTenant(tenant);
        entity.setKeyPrefix(message.keyPrefix());
        entity.setMethod(message.method());
        entity.setPath(message.path());
        entity.setStatus(message.status());
        entity.setDurationMs(message.durationMs());
        entity.setOutcome(message.outcome());
        entity.setOccurredAt(message.occurredAt() != null ? message.occurredAt() : Instant.now());

        AuditEvent saved = auditEventRepository.save(entity);
        AuditEventDto dto = toDto(saved);
        eventPublisher.publishEvent(new AuditEventPersistedEvent(dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDto> search(String outcome, Instant from, Instant to, Pageable pageable) {
        requireTenant();
        return auditEventRepository.search(blankToNull(outcome), from, to, pageable).map(this::toDto);
    }

    private void requireTenant() {
        TenantContext.getTenantId()
                .orElseThrow(() -> new IllegalStateException("Tenant context required"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AuditEventDto toDto(AuditEvent e) {
        return new AuditEventDto(
                e.getId(),
                e.getEventId(),
                e.getTenant().getId(),
                e.getKeyPrefix(),
                e.getMethod(),
                e.getPath(),
                e.getStatus(),
                e.getDurationMs(),
                e.getOutcome(),
                e.getOccurredAt()
        );
    }

    /** Evento Spring interno: notifica i subscriber SSE dopo commit. */
    public record AuditEventPersistedEvent(AuditEventDto event) {}
}
