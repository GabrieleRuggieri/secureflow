/*
 * Permission — Permesso granulare per il RBAC (resource:action).
 *
 * Esempio: resource="tenant", action="create" → permesso "tenant:create". La combinazione
 * (tenant_id, resource, action) è univoca. getPermissionString() restituisce il formato usato
 * da @RequiresPermission e RbacPermissionEvaluator. I permessi sono assegnati ai Role, non
 * direttamente agli User.
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
@Table(name = "permission")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** @ManyToOne Tenant: ogni Permission appartiene a un tenant. N:1, FK in permission. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private Tenant tenant;

    /**
     * Risorsa (es. "tenant", "user", "role"). Insieme ad action forma "resource:action".
     * Esempio: resource=tenant, action=create → permesso "tenant:create".
     */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String resource;

    /** Azione sulla risorsa (es. "create", "read", "update", "delete"). */
    @NotBlank
    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String description;

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

    /**
     * Restituisce il permesso nel formato usato da @RequiresPermission e RbacPermissionEvaluator.
     * Esempio: resource="tenant", action="create" → "tenant:create".
     */
    public String getPermissionString() {
        return resource + ":" + action;
    }
}
