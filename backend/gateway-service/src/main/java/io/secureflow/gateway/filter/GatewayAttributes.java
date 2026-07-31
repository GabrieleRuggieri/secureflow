/*
 * GatewayAttributes — Attributi scambiati tra filtri nella ServerWebExchange.
 */
package io.secureflow.gateway.filter;

import java.util.UUID;

public final class GatewayAttributes {

    public static final String API_KEY = "secureflow.apiKey";
    public static final String TENANT_ID = "secureflow.tenantId";
    public static final String KEY_PREFIX = "secureflow.keyPrefix";
    public static final String KEY_ID = "secureflow.keyId";
    public static final String RATE_LIMIT = "secureflow.rateLimit";
    public static final String RATE_REMAINING = "secureflow.rateRemaining";
    public static final String START_NANOS = "secureflow.startNanos";

    private GatewayAttributes() {
    }

    public static UUID asUuid(Object value) {
        return value instanceof UUID uuid ? uuid : null;
    }
}
