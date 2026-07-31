/*
 * AuditEventRepository — Persistenza e query filtrate degli eventi di audit.
 */
package io.secureflow.core.repository;

import io.secureflow.core.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Optional<AuditEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE (:outcome IS NULL OR e.outcome = :outcome)
              AND (:from IS NULL OR e.occurredAt >= :from)
              AND (:to IS NULL OR e.occurredAt <= :to)
            ORDER BY e.occurredAt DESC
            """)
    Page<AuditEvent> search(
            @Param("outcome") String outcome,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
