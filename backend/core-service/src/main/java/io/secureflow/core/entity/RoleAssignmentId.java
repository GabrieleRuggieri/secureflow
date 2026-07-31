/*
 * RoleAssignmentId — Chiave primaria composta per RoleAssignment.
 *
 * @Embeddable usato con @EmbeddedId in RoleAssignment. La chiave (user_id, role_id) garantisce
 * che un utente non possa avere lo stesso ruolo assegnato più volte. Serializable richiesto
 * da JPA per le chiavi composte.
 */
package io.secureflow.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Chiave primaria composta per RoleAssignment. @Embeddable = può essere incorporata
 * in un'altra entity con @EmbeddedId. Serializable richiesto da JPA per le chiavi
 * composte (sessioni, cache). @EqualsAndHashCode: necessario per confrontare entity
 * con chiave composta (Set, Map, equals). Le due colonne formano la PK di role_assignment.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoleAssignmentId implements Serializable {

    /** Prima parte della PK. FK verso user(id). */
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    /** Seconda parte della PK. FK verso role(id). */
    @Column(name = "role_id", columnDefinition = "BINARY(16)")
    private UUID roleId;
}
