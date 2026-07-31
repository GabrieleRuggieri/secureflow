/*
 * ValidatedApiKey — Risultato della validazione chiave (cache o Core Service).
 */
package io.secureflow.gateway.client;

import java.util.UUID;

public record ValidatedApiKey(
        boolean valid,
        UUID tenantId,
        String keyPrefix,
        Integer rateLimitPerMinute,
        UUID keyId
) {
    public static ValidatedApiKey invalid() {
        return new ValidatedApiKey(false, null, null, null, null);
    }
}
