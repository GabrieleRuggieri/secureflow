/*
 * RoleAssignment — Assegnazione di un Role a un User.
 *
 * Tabella di join tra User e Role con chiave primaria composta (user_id, role_id). Un utente
 * può avere più ruoli e un ruolo può essere assegnato a più utenti. assigned_by traccia chi
 * ha effettuato l'assegnazione. Il filtro tenant scoped via user: si vedono solo le
 * assegnazioni relative agli utenti del tenant corrente.
 */
package io.secureflow.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name = "role_assignment")
/*
 * @Filter: RoleAssignment non ha tenant_id diretto. Il filtro usa una subquery:
 * "user_id IN (SELECT id FROM user WHERE tenant_id = :tenantId)". Così si vedono
 * solo le assegnazioni per utenti del tenant corrente. Alternativa: filtrare via
 * join su user, ma la subquery è più chiara per Hibernate.
 */
@Filter(name = "tenantFilter", condition = "user_id IN (SELECT id FROM user WHERE tenant_id = :tenantId)")
@Getter
@Setter
@NoArgsConstructor
public class RoleAssignment {

    /**
     * @EmbeddedId: chiave primaria composta (user_id, role_id). La coppia è univoca:
     * un utente non può avere lo stesso ruolo due volte. RoleAssignmentId è @Embeddable.
     */
    @Id
    @EmbeddedId
    private RoleAssignmentId id;

    /**
     * @ManyToOne User: questa assegnazione riguarda un utente. @MapsId("userId"): il
     * campo user.getId() popola la parte userId dell'EmbeddedId. Così JPA sa come
     * comporre la chiave dalla relazione. La FK user_id è sia nella PK che nella relazione.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * @ManyToOne Role: questa assegnazione assegna un ruolo. @MapsId("roleId"): come
     * sopra, role.getId() popola roleId nell'EmbeddedId. La tabella role_assignment
     * ha (user_id, role_id) come PK e come FKs verso user e role.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** Quando è stata effettuata l'assegnazione. updatable=false: immutabile. */
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    /**
     * @ManyToOne assignedBy: opzionale, chi ha assegnato il ruolo (audit). Nullable:
     * le assegnazioni iniziali possono non avere un "assigner". FK a user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    /**
     * @PrePersist: prima di INSERT. Costruisce l'EmbeddedId da user e role (necessario
     * perché non usiamo @GeneratedValue). Imposta assignedAt se null.
     */
    @PrePersist
    protected void onCreate() {
        if (id == null && user != null && role != null) {
            id = new RoleAssignmentId(user.getId(), role.getId());
        }
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
