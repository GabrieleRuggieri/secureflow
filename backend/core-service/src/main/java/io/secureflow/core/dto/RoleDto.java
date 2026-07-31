/*
 * RoleDto — DTO per l'API REST dei ruoli.
 *
 * permissionIds: set di UUID dei permessi associati al ruolo. La relazione Role-Permission
 * è ManyToMany; il DTO espone solo gli ID per evitare ricorsione e payload pesanti.
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        String description,
        Set<UUID> permissionIds
) {
    public record Create(
            @NotBlank String name,
            String description,
            Set<UUID> permissionIds
    ) {}

    public record Update(
            String name,
            String description,
            Set<UUID> permissionIds
    ) {}
}
