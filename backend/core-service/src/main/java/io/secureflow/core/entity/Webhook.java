/*
 * Webhook — URL di callback registrato da un tenant.
 *
 * events: insieme di tipi evento sottoscritti (JSON). secret: chiave HMAC per firmare
 * i payload in delivery. enabled: disabilitabile senza cancellare la registrazione.
 */
package io.secureflow.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "webhook")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class Webhook {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private Tenant tenant;

    @NotBlank
    @Column(nullable = false, length = 2048)
    private String url;

    /** Secret HMAC-SHA256 per X-SecureFlow-Signature. Generato alla creazione. */
    @NotBlank
    @Column(nullable = false, length = 64)
    private String secret;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Set<String> events = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean subscribesTo(String eventType) {
        return events != null && events.contains(eventType);
    }
}
