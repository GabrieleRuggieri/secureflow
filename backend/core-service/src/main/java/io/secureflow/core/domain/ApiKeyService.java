/*
 * ApiKeyService — Ciclo di vita delle API key: generazione, revoca, rotazione.
 *
 * La chiave in chiaro (sf_live_ + random) viene mostrata una sola volta. In DB resta
 * solo SHA-256 (key_hash) e un prefisso leggibile. Revoca imposta revoked_at; rotazione
 * revoca la vecchia e ne crea una nuova. Dispatch webhook su create/revoke/rotate.
 */
package io.secureflow.core.domain;

import io.secureflow.core.dto.ApiKeyDto;
import io.secureflow.core.entity.ApiKey;
import io.secureflow.core.entity.Tenant;
import io.secureflow.core.entity.User;
import io.secureflow.core.repository.ApiKeyRepository;
import io.secureflow.core.repository.TenantRepository;
import io.secureflow.core.repository.UserRepository;
import io.secureflow.core.security.TenantContext;
import io.secureflow.core.webhook.WebhookDispatcher;
import io.secureflow.core.webhook.WebhookEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "sf_live_";
    private static final int SECRET_BYTES = 24;
    private static final int PREFIX_LENGTH = 16;

    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final WebhookDispatcher webhookDispatcher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<ApiKeyDto> list() {
        TenantContext.getTenantId().orElseThrow();
        return apiKeyRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiKeyDto get(UUID id) {
        return apiKeyRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Genera una nuova API key. rawKey mostrata una sola volta nella response Created.
     * key_hash = SHA-256(rawKey); key_prefix = primi 16 char per identificazione UI/log.
     */
    @Transactional
    public ApiKeyDto.Created create(ApiKeyDto.Create request) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        CreatedKey created = createInternal(request);
        webhookDispatcher.dispatch(tenantId, WebhookEventType.API_KEY_CREATED, keyEventData(created.entity()));
        return created.response();
    }

    /** Revoca una chiave impostando revoked_at. Idempotente se già revocata. */
    @Transactional
    public ApiKeyDto revoke(UUID id) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        return apiKeyRepository.findById(id)
                .map(apiKey -> {
                    if (apiKey.getRevokedAt() == null) {
                        apiKey.setRevokedAt(Instant.now());
                        apiKey = apiKeyRepository.save(apiKey);
                        webhookDispatcher.dispatch(tenantId, WebhookEventType.API_KEY_REVOKED, keyEventData(apiKey));
                    }
                    return toDto(apiKey);
                })
                .orElse(null);
    }

    /**
     * Rotazione: revoca la chiave esistente e ne genera una nuova con lo stesso nome
     * (e stessa scadenza residua se presente). Restituisce la nuova rawKey una sola volta.
     */
    @Transactional
    public ApiKeyDto.Created rotate(UUID id) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        ApiKey existing = apiKeyRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (existing.getRevokedAt() == null) {
            existing.setRevokedAt(Instant.now());
            apiKeyRepository.save(existing);
        }

        CreatedKey created = createInternal(new ApiKeyDto.Create(existing.getName(), existing.getExpiresAt()));
        webhookDispatcher.dispatch(tenantId, WebhookEventType.API_KEY_ROTATED, Map.of(
                "oldKeyId", existing.getId().toString(),
                "oldKeyPrefix", existing.getKeyPrefix(),
                "newKeyId", created.entity().getId().toString(),
                "newKeyPrefix", created.entity().getKeyPrefix()
        ));
        return created.response();
    }

    private CreatedKey createInternal(ApiKeyDto.Create request) {
        UUID tenantId = TenantContext.getTenantId().orElseThrow();
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

        String rawKey = generateRawKey();
        String keyPrefix = rawKey.substring(0, PREFIX_LENGTH);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenant(tenant);
        apiKey.setKeyHash(sha256Hex(rawKey));
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(request.name());
        apiKey.setExpiresAt(request.expiresAt());
        resolveCurrentUser().ifPresent(apiKey::setCreatedBy);
        apiKey = apiKeyRepository.save(apiKey);

        return new CreatedKey(apiKey, new ApiKeyDto.Created(
                apiKey.getId(),
                apiKey.getKeyPrefix(),
                apiKey.getName(),
                rawKey,
                apiKey.getExpiresAt(),
                apiKey.getCreatedAt()
        ));
    }

    private record CreatedKey(ApiKey entity, ApiKeyDto.Created response) {}

    /** Validazione usata dal gateway: hash della raw key → lookup → isValid(). */
    @Transactional(readOnly = true)
    public boolean isValidRawKey(String rawKey) {
        return apiKeyRepository.findByKeyHash(sha256Hex(rawKey))
                .map(ApiKey::isValid)
                .orElse(false);
    }

    private String generateRawKey() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX + HexFormat.of().formatHex(bytes);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private java.util.Optional<User> resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            UUID tenantId = TenantContext.getTenantId().orElse(null);
            if (sub != null && tenantId != null) {
                return userRepository.findByTenantIdAndKeycloakUserId(tenantId, sub);
            }
        }
        return java.util.Optional.empty();
    }

    private Map<String, Object> keyEventData(ApiKey apiKey) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("keyId", apiKey.getId().toString());
        data.put("keyPrefix", apiKey.getKeyPrefix());
        data.put("name", apiKey.getName());
        if (apiKey.getExpiresAt() != null) {
            data.put("expiresAt", apiKey.getExpiresAt().toString());
        }
        if (apiKey.getRevokedAt() != null) {
            data.put("revokedAt", apiKey.getRevokedAt().toString());
        }
        return data;
    }

    private ApiKeyDto toDto(ApiKey k) {
        return new ApiKeyDto(
                k.getId(),
                k.getKeyPrefix(),
                k.getName(),
                k.getExpiresAt(),
                k.getRevokedAt(),
                k.getCreatedAt(),
                k.isValid()
        );
    }
}
