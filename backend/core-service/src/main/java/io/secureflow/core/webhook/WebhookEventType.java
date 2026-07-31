/*
 * WebhookEventType — Eventi che i tenant possono sottoscrivere.
 *
 * Costanti usate in dispatch e nella validazione della registrazione webhook.
 */
package io.secureflow.core.webhook;

import java.util.Set;

public final class WebhookEventType {

    public static final String API_KEY_CREATED = "api_key.created";
    public static final String API_KEY_REVOKED = "api_key.revoked";
    public static final String API_KEY_ROTATED = "api_key.rotated";
    public static final String USER_CREATED = "user.created";
    public static final String RATE_LIMIT_EXCEEDED = "rate_limit.exceeded";

    public static final Set<String> ALL = Set.of(
            API_KEY_CREATED,
            API_KEY_REVOKED,
            API_KEY_ROTATED,
            USER_CREATED,
            RATE_LIMIT_EXCEEDED
    );

    private WebhookEventType() {
    }
}
