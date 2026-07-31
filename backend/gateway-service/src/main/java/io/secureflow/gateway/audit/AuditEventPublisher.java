/*
 * AuditEventPublisher — Fire-and-forget su Kafka topic audit-events.
 *
 * Non blocca la response: errori di publish sono solo loggati.
 */
package io.secureflow.gateway.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    @Value("${secureflow.audit-topic:audit-events}")
    private String topic;

    public void publish(AuditEvent event) {
        String key = event.tenantId() != null ? event.tenantId().toString() : "unknown";
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish audit event {}: {}", event.id(), ex.toString());
                    }
                });
    }
}
