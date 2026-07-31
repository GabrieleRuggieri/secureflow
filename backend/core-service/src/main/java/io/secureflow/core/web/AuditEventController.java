/*
 * AuditEventController — Lista paginata e stream SSE degli eventi di audit.
 *
 * GET /api/audit-events — storico con filtri outcome/from/to.
 * GET /api/audit-events/stream — SSE live (stessi filtri).
 */
package io.secureflow.core.web;

import io.secureflow.core.audit.AuditSseHub;
import io.secureflow.core.domain.AuditService;
import io.secureflow.core.dto.AuditEventDto;
import io.secureflow.core.security.RequiresPermission;
import io.secureflow.core.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditService auditService;
    private final AuditSseHub auditSseHub;

    @GetMapping
    @RequiresPermission("audit:read")
    public Page<AuditEventDto> list(
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditService.search(outcome, from, to, pageable);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresPermission("audit:read")
    public SseEmitter stream(
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        UUID tenantId = TenantContext.getTenantId()
                .orElseThrow(() -> new IllegalStateException("Tenant context required"));
        return auditSseHub.subscribe(tenantId, outcome, from, to);
    }
}
