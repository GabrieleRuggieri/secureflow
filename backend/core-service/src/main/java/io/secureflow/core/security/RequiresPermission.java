/*
 * RequiresPermission — Annotazione per proteggere metodi con permesso RBAC.
 *
 * Esempio: @RequiresPermission("tenant:create"). Enforced da RequiresPermissionAspect che
 * delega a RbacPermissionEvaluator. Alternativa a @PreAuthorize("hasPermission(...)") con
 * sintassi più concisa e valore dell'annotazione leggibile a runtime.
 */
package io.secureflow.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require a specific permission (format: "resource:action") for method access.
 * Uses the RBAC engine to verify the current user has the permission via their roles.
 * Enforced by RequiresPermissionAspect.
 *
 * Example: @RequiresPermission("tenant:create")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * Permission string in format "resource:action", e.g. "tenant:create", "user:read"
     */
    String value();
}
