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
@Filter(name = "tenantFilter", condition = "user_id IN (SELECT id FROM user WHERE tenant_id = :tenantId)")
@Getter
@NoArgsConstructor
public class RoleAssignment {

    @Id
    @EmbeddedId
    private RoleAssignmentId id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false, columnDefinition = "BINARY(16)")
    private Role role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", columnDefinition = "BINARY(16)")
    private User assignedBy;

    public void setId(RoleAssignmentId id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
        syncId();
    }

    public void setRole(Role role) {
        this.role = role;
        syncId();
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    private void syncId() {
        if (user != null && user.getId() != null && role != null && role.getId() != null) {
            this.id = new RoleAssignmentId(user.getId(), role.getId());
        }
    }

    @PrePersist
    protected void onCreate() {
        syncId();
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
