/*
 * Tenant — Entità radice del modello multi-tenant.
 *
 * Rappresenta un'organizzazione/cliente che usa la piattaforma. Ogni dato sensibile (User, Role,
 * Permission, ApiKey) appartiene a un tenant tramite tenant_id. Lo slug è univoco e usato per
 * identificare il tenant nelle URL e nel JWT (es. "default-tenant"). 
 * rate_limit_per_minute configura il limite di richieste API per il gateway.
 */
package io.secureflow.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    /**
     * Chiave primaria UUID. BINARY(16) in MySQL = 16 byte, più efficiente di VARCHAR(36).
     * Generato in @PrePersist se null. UUID evita collisioni in sistemi distribuiti e
     * non espone informazioni (es. numero di tenant creati).
     */
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** Nome leggibile del tenant (es. "GRAS Lab"). */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Slug univoco: identificatore URL-friendly (es. "GRAS Lab"). Usato nel JWT come
     * tenantId quando non si usa l'UUID; TenantContextFilter lo risolve in UUID via DB.
     */
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Limite richieste/minuto per il gateway. Default 60. Ogni tenant può
     * avere un limite diverso; il rate limiter Redis userà questo valore.
     */
    @NotNull
    @Positive
    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 60;

    /** Istante di creazione. updatable=false: non modificabile dopo il primo save. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Istante ultimo aggiornamento. Aggiornato automaticamente in @PreUpdate. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * @PrePersist: eseguito da JPA prima di INSERT. Genera id se mancante, imposta
     * createdAt e updatedAt. Necessario perché non usiamo @GeneratedValue (UUID custom).
     */
    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    /**
     * @PreUpdate: eseguito da JPA prima di UPDATE. Aggiorna updatedAt per tracciare
     * l'ultima modifica. Utile per audit e cache invalidation.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
