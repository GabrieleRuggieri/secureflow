/*
 * UserDto — DTO per l'API REST degli utenti.
 *
 * keycloakUserId: claim "sub" del JWT, usato per sincronizzare con Keycloak. roleIds: set
 * di UUID dei ruoli assegnati. Create richiede email e username; Update permette solo di
 * modificare le assegnazioni di ruolo.
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String keycloakUserId,
        String email,
        String username,
        Set<UUID> roleIds
) {
    public record Create(
            @NotBlank String keycloakUserId,
            @NotBlank @Email String email,
            @NotBlank String username,
            Set<UUID> roleIds
    ) {}

    public record Update(
            Set<UUID> roleIds
    ) {}
}
