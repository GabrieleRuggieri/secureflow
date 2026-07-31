/*
 * ApiKeyRepository — Accesso dati per ApiKey.
 *
 * findByKeyHash: lookup dal gateway (validazione chiave). findByTenantIdAndKeyPrefix
 * per identificare una chiave senza esporre l'hash. Il filtro Hibernate isola per tenant.
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @Query("SELECT k FROM ApiKey k JOIN FETCH k.tenant WHERE k.keyHash = :keyHash")
    Optional<ApiKey> findByKeyHash(@Param("keyHash") String keyHash);

    boolean existsByTenant_IdAndKeyPrefix(UUID tenantId, String keyPrefix);
}
