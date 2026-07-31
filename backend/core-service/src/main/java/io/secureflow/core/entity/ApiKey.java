/*
 * ApiKey — Chiave API per autenticazione M2M (Fase 3).
 *
 * key_hash: SHA-256 della chiave; la chiave in chiaro viene mostrata una sola volta.
 * key_prefix: prefisso leggibile (es. "sf_live_") per identificare la chiave senza esporla.
 * expires_at: scadenza opzionale; revoked_at: timestamp di revoca. isRevoked(), isExpired(),
 * isValid() sono metodi di dominio per lo stato della chiave. Gestione completa in Fase 3.
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
@Table(name = "api_key")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** @ManyToOne Tenant: ogni ApiKey appartiene a un tenant. N:1. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Hash SHA-256 della chiave. La chiave in chiaro non viene mai persistita; si mostra
     * una sola volta alla creazione. 64 char = 32 byte hex per SHA-256.
     */
    @NotBlank
    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    /**
     * Prefisso leggibile (es. "sf_live_abc123"). Permette di identificare la chiave
     * nei log o nell'UI senza esporre la chiave. Univoco per tenant.
     */
    @NotBlank
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    /** Nome opzionale per l'utente (es. "Chiave produzione"). */
    @Column(length = 255)
    private String name;

    /** Scadenza opzionale. Null = mai scade. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Se non null, la chiave è revocata. Timestamp della revoca per audit. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** @ManyToOne User: chi ha creato la chiave. Opzionale (es. creazione sistema). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    /** La chiave è stata revocata manualmente. revokedAt != null. */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** La chiave è scaduta. expiresAt è passato. */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /** La chiave è utilizzabile: non revocata e non scaduta. */
    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }
}
