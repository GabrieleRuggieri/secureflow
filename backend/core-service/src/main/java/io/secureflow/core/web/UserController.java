/*
 * UserController — Endpoint REST per la gestione utenti.
 *
 * /api/users: CRUD. Il tenant è implicito dal JWT (TenantContext). Gli utenti sono
 * scoped al tenant; il filtro Hibernate applicato nei repository garantisce l'isolamento.
 */
package io.secureflow.core.web;

import io.secureflow.core.domain.UserService;
import io.secureflow.core.dto.UserDto;
import io.secureflow.core.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequiresPermission("user:read")
    public List<UserDto> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    @RequiresPermission("user:read")
    public ResponseEntity<UserDto> get(@PathVariable UUID id) {
        UserDto dto = userService.get(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @RequiresPermission("user:create")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody UserDto.Create request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @RequiresPermission("user:update")
    public ResponseEntity<UserDto> update(@PathVariable UUID id, @Valid @RequestBody UserDto.Update request) {
        UserDto dto = userService.update(id, request);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("user:delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }
}
