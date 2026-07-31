/*
 * TenantDto — DTO per l'API REST dei tenant.
 *
 * Record immutabili per trasferimento dati. Create: validazione per creazione (slug univoco).
 * Update: campi opzionali per PATCH parziale. L'id non è presente in Create (generato server-side).
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TenantDto(
        UUID id,
        String name,
        String slug,
        Integer rateLimitPerMinute
) {
    public record Create(
            @NotBlank String name,
            @NotBlank String slug,
            @NotNull @Positive Integer rateLimitPerMinute
    ) {}

    public record Update(
            String name,
            Integer rateLimitPerMinute
    ) {}
}
