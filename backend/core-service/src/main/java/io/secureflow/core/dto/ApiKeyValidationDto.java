/*
 * ApiKeyValidationDto — Request/response per validazione interna gateway → core.
 */
package io.secureflow.core.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class ApiKeyValidationDto {

    private ApiKeyValidationDto() {
    }

    public record Request(@NotBlank String apiKey) {}

    public record Response(
            boolean valid,
            UUID tenantId,
            String keyPrefix,
            Integer rateLimitPerMinute,
            UUID keyId
    ) {
        public static Response invalid() {
            return new Response(false, null, null, null, null);
        }
    }
}
