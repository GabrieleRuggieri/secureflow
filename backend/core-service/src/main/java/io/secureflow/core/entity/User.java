/*
 * User — Utente della dashboard, sincronizzato con Keycloak.
 *
 * keycloak_user_id corrisponde al claim "sub" del JWT. Gli utenti sono creati nel Core Service
 * quando accedono alla dashboard (o via sync). L'associazione con Role avviene tramite
 * RoleAssignment. Ogni User appartiene a un tenant tramite tenant_id; il filtro Hibernate
 * garantisce l'isolamento automatico nelle query.
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
@Table(name = "user")
/*
 * @Filter tenantFilter: Hibernate aggiunge "WHERE tenant_id = :tenantId" a ogni query
 * su User. tenantId da TenantContext (JWT). Isolamento: utente tenant A non vede B.
 */
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * @ManyToOne: molti User appartengono a un Tenant. Relazione N:1 — la FK tenant_id
     * vive nella tabella user. @JoinColumn(name="tenant_id") indica la colonna FK.
     * FetchType.LAZY: non carica il Tenant finché non si accede a user.getTenant().
     * Evita JOIN inutili quando si legge solo user.email; fondamentale per performance.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Claim "sub" del JWT Keycloak — identificatore globale dell'utente in Keycloak.
     * Univoco per tenant (stesso utente Keycloak può esistere in tenant diversi con
     * ruoli diversi). Usato da RbacPermissionEvaluator per caricare User + Role + Permission.
     */
    @NotBlank
    @Column(name = "keycloak_user_id", nullable = false, length = 255)
    private String keycloakUserId;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String username;

    /**
     * @OneToMany: un User ha molte RoleAssignment (una per ogni ruolo assegnato).
     * mappedBy="user": il lato "padrone" è RoleAssignment.user; questa è la collezione
     * inversa, non crea colonna aggiuntiva (la FK user_id è in role_assignment).
     * cascade=ALL: save/update/delete su User si propaga alle RoleAssignment.
     * orphanRemoval=true: rimuovere una RoleAssignment dalla collection e fare save
     * elimina la riga dal DB (evita "orfani" senza user). HashSet per evitare duplicati.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoleAssignment> roleAssignments = new HashSet<>();

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
