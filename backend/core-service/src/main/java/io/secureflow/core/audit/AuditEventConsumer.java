/*
 * AuditEventConsumer — Consuma audit-events, persiste su MySQL, gestisce TenantContext.
 *
 * Imposta TenantContext prima della transazione così il filtro Hibernate è attivo
 * sull'EntityManager. Errori non recuperabili → DLT via DefaultErrorHandler.
 */
package io.secureflow.core.audit;

import io.secureflow.core.domain.AuditService;
import io.secureflow.core.dto.AuditEventDto;
import io.secureflow.core.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "secureflow.audit.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(
            topics = "${secureflow.audit.topic:audit-events}",
            groupId = "secureflow-core-audit",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(AuditEventDto.Message message) {
        if (message == null || message.tenantId() == null) {
            throw new IllegalArgumentException("Invalid audit message: missing tenantId");
        }

        TenantContext.setTenantId(message.tenantId());
        try {
            AuditEventDto saved = auditService.persist(message);
            log.debug("Persisted audit event {} for tenant {}", saved.eventId(), saved.tenantId());
        } finally {
            TenantContext.clear();
        }
    }
}
