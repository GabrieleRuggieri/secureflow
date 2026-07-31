/*
 * ApiKeyDto — DTO per l'API REST delle API key.
 *
 * Created include rawKey (mostrata una sola volta). List/Get non espongono mai keyHash
 * né la chiave in chiaro: solo prefix e metadati.
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyDto(
        UUID id,
        String keyPrefix,
        String name,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt,
        boolean valid
) {
    /** Response di creazione/rotazione: include la chiave in chiaro una sola volta. */
    public record Created(
            UUID id,
            String keyPrefix,
            String name,
            String rawKey,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record Create(
            @NotBlank String name,
            Instant expiresAt
    ) {}
}
