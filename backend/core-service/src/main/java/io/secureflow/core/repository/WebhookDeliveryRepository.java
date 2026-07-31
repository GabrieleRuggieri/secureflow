/*
 * WebhookDeliveryRepository — Coda delivery e dead letter.
 *
 * findDueDeliveries: worker scheduled senza TenantContext (tutti i tenant).
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    @Query("""
            SELECT d FROM WebhookDelivery d
            JOIN FETCH d.webhook
            WHERE d.status = io.secureflow.core.entity.WebhookDelivery.Status.PENDING
              AND (d.nextAttemptAt IS NULL OR d.nextAttemptAt <= :now)
            ORDER BY d.nextAttemptAt ASC
            """)
    List<WebhookDelivery> findDueDeliveries(@Param("now") Instant now);

    List<WebhookDelivery> findByStatus(WebhookDelivery.Status status);
}
