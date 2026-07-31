/*
 * AuditEvent — Evento di audit persistito dal consumer Kafka (Fase 5).
 *
 * eventId: id generato dal gateway (UUID string), usato per idempotenza su redelivery.
 * outcome: success | denied | rate_limited | error. Filtro Hibernate tenant isolation.
 */
package io.secureflow.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** Id originale dell'evento gateway (idempotenza su Kafka redelivery). */
    @NotBlank
    @Column(name = "event_id", nullable = false, length = 36, unique = true)
    private String eventId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private Tenant tenant;

    @Column(name = "key_prefix", length = 20)
    private String keyPrefix;

    @NotBlank
    @Column(nullable = false, length = 16)
    private String method;

    @NotBlank
    @Column(nullable = false, length = 512)
    private String path;

    @NotNull
    @Column(nullable = false)
    private Integer status;

    @NotNull
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @NotBlank
    @Column(nullable = false, length = 32)
    private String outcome;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
