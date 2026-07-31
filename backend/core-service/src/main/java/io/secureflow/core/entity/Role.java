/*
 * Role — Ruolo RBAC, aggregato di Permission.
 *
 * Un ruolo è un nome logico (es. "admin", "operator") che raggruppa più permessi. La relazione
 * ManyToMany con Permission è gestita dalla tabella role_permission. Ogni ruolo è scoped al
 * tenant; il nome è univoco per tenant. Usato per assegnare permessi agli utenti in blocco.
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "role")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * @ManyToOne Tenant: ogni Role appartiene a un tenant. N ruoli → 1 tenant.
     * LAZY: non carica il Tenant se accedi solo a role.getName(). La FK tenant_id
     * è nella tabella role; @JoinColumn la mappa.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Nome del ruolo (es. "admin", "operator"). Univoco per tenant. */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * @ManyToMany Permission: un Role ha molti Permission, un Permission può essere
     * in molti Role. Relazione N:N richiede tabella di join.
     * @JoinTable: crea/usare tabella role_permission con:
     *   - joinColumns: colonna che punta a questa entity (role_id)
     *   - inverseJoinColumns: colonna che punta all'altra (permission_id)
     * LAZY: non carica i Permission finché non si accede a role.getPermissions().
     * Importante: senza fetch esplicito, una query su Role non carica i Permission.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

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
}
