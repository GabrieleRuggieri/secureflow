/*
 * PermissionDto — DTO per l'API REST dei permessi.
 *
 * resource e action formano il permesso "resource:action" (es. "tenant:create"). Update
 * permette solo di modificare la description; resource e action sono immutabili dopo la creazione.
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PermissionDto(
        UUID id,
        String resource,
        String action,
        String description
) {
    public record Create(
            @NotBlank String resource,
            @NotBlank String action,
            String description
    ) {}

    public record Update(
            String description
    ) {}
}
