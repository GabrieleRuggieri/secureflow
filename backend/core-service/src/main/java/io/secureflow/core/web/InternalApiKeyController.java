/*
 * InternalApiKeyController — Endpoint interno per il Gateway (validazione API key).
 *
 * /internal/api-keys/validate: non richiede JWT utente; protetto da X-Internal-Token
 * condiviso tra gateway e core (rete interna / docker).
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.ApiKeyService;
import io.secureflow.core.dto.ApiKeyValidationDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/api-keys")
@RequiredArgsConstructor
public class InternalApiKeyController {

    private final ApiKeyService apiKeyService;

    @Value("${secureflow.internal-token:secureflow-internal}")
    private String internalToken;

    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidationDto.Response> validate(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody ApiKeyValidationDto.Request request) {
        if (token == null || !internalToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(apiKeyService.validateRawKey(request.apiKey()));
    }
}
