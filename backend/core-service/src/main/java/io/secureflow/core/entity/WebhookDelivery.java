/*
 * WebhookDelivery — Tentativo di consegna di un evento verso un webhook.
 *
 * status: PENDING → SUCCESS | DEAD_LETTER. Retry con backoff esponenziale su
 * next_attempt_at. attempt_count / max_attempts governano il passaggio a dead letter.
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
@Table(name = "webhook_delivery")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class WebhookDelivery {

    public enum Status {
        PENDING,
        SUCCESS,
        DEAD_LETTER
    }

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false, columnDefinition = "BINARY(16)")
    private Webhook webhook;

    @NotBlank
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (nextAttemptAt == null) nextAttemptAt = Instant.now();
    }
}
