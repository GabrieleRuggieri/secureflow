/*
 * TenantService — Logica di business per la gestione dei tenant.
 *
 * I tenant sono entità root (non hanno tenant_id nel filtro): chiunque con permesso
 * "tenant:read" può elencarli. Validazione: slug univoco in create. I controller delegano
 * qui tutta la logica; il service gestisce transazioni e conversione entity↔DTO.
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.TenantDto;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Elenca tutti i tenant. @Transactional(readOnly=true) ottimizza per query: no flush,
     * hint per il DB. Tenant non ha filtro: è entità root, visibile a chi ha tenant:read.
     */
    @Transactional(readOnly = true)
    public List<TenantDto> list() {
        return tenantRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /** Recupera un tenant per ID. Ritorna null se non esiste (il controller mappa in 404). */
    @Transactional(readOnly = true)
    public TenantDto get(UUID id) {
        return tenantRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Crea un nuovo tenant. Validazione: slug deve essere univoco (usato in URL e JWT).
     * L'id è generato in @PrePersist. Il repository.save() triggera il persist.
     */
    @Transactional
    public TenantDto create(TenantDto.Create request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Tenant with slug already exists: " + request.slug());
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setRateLimitPerMinute(request.rateLimitPerMinute());
        tenant = tenantRepository.save(tenant);
        return toDto(tenant);
    }

    /**
     * Aggiorna un tenant. Solo i campi non null nel request vengono modificati (PATCH-like).
     * Optional.map evita NPE; orElse(null) per segnalare "non trovato" al controller.
     */
    @Transactional
    public TenantDto update(UUID id, TenantDto.Update request) {
        return tenantRepository.findById(id)
                .map(tenant -> {
                    if (request.name() != null) tenant.setName(request.name());
                    if (request.rateLimitPerMinute() != null) tenant.setRateLimitPerMinute(request.rateLimitPerMinute());
                    return tenantRepository.save(tenant);
                })
                .map(this::toDto)
                .orElse(null);
    }

    /** Elimina un tenant. CASCADE nel DB elimina user, role, permission, api_key collegati. */
    @Transactional
    public void delete(UUID id) {
        tenantRepository.deleteById(id);
    }

    /** Converte entity in DTO per la response. Evita di esporre l'entità JPA (lazy, proxy). */
    private TenantDto toDto(Tenant t) {
        return new TenantDto(t.getId(), t.getName(), t.getSlug(), t.getRateLimitPerMinute());
    }
}
