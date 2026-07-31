/*
 * AuditSseHub — Fan-out SSE degli eventi di audit verso i client connessi.
 *
 * Ogni subscriber ha filtri opzionali (outcome, from, to) e riceve solo eventi
 * del proprio tenant. Completamento/timeout rimuove l'emitter dalla lista.
 */
package io.secureflow.core.audit;

import io.secureflow.core.domain.AuditService;
import io.secureflow.core.dto.AuditEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class AuditSseHub {

    private static final long SSE_TIMEOUT_MS = 0L; // no timeout; client chiude

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(UUID tenantId, String outcome, Instant from, Instant to) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Subscription sub = new Subscription(emitter, AuditEventFilter.of(tenantId, outcome, from, to));
        subscriptions.add(sub);

        emitter.onCompletion(() -> subscriptions.remove(sub));
        emitter.onTimeout(() -> {
            subscriptions.remove(sub);
            emitter.complete();
        });
        emitter.onError(ex -> subscriptions.remove(sub));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            subscriptions.remove(sub);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPersisted(AuditService.AuditEventPersistedEvent event) {
        publish(event.event());
    }

    public void publish(AuditEventDto event) {
        for (Subscription sub : subscriptions) {
            if (!sub.filter().matches(event)) {
                continue;
            }
            try {
                sub.emitter().send(SseEmitter.event()
                        .name("audit")
                        .id(event.eventId())
                        .data(event));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Removing SSE subscriber after send failure: {}", ex.toString());
                subscriptions.remove(sub);
                try {
                    sub.emitter().complete();
                } catch (Exception ignored) {
                    // already closed
                }
            }
        }
    }

    int subscriberCount() {
        return subscriptions.size();
    }

    private record Subscription(SseEmitter emitter, AuditEventFilter filter) {}
}
