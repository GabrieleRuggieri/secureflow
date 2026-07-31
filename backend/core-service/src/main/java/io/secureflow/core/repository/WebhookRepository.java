/*
 * WebhookRepository — Accesso dati per Webhook.
 *
 * findByTenantIdAndEnabledTrue: usato dal dispatcher (con TenantContext o query esplicita).
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findByTenant_IdAndEnabledTrue(UUID tenantId);
}
