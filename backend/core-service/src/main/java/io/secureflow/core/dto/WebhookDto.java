/*
 * WebhookDto — DTO per registrazione e gestione webhook.
 *
 * Created espone il secret una sola volta (come le API key). List/Get non lo rimostrano.
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WebhookDto(
        UUID id,
        String url,
        Set<String> events,
        boolean enabled,
        Instant createdAt
) {
    public record Created(
            UUID id,
            String url,
            String secret,
            Set<String> events,
            boolean enabled,
            Instant createdAt
    ) {}

    public record Create(
            @NotBlank String url,
            @NotEmpty Set<String> events
    ) {}

    public record Update(
            String url,
            Set<String> events,
            Boolean enabled
    ) {}

    public record DeliveryDto(
            UUID id,
            UUID webhookId,
            String eventType,
            String status,
            int attemptCount,
            String lastError,
            Instant createdAt,
            Instant completedAt
    ) {}
}
