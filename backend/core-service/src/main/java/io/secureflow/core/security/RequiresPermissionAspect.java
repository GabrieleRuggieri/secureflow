/*
 * RequiresPermissionAspect — Aspect che applica @RequiresPermission.
 *
 * Intercetta le chiamate a metodi annotati, estrae il permesso richiesto, e delega a
 * RbacPermissionEvaluator. Se l'utente non ha il permesso, lancia AccessDeniedException.
 * L'aspect permette di usare il valore dell'annotazione (non possibile con @PreAuthorize
 * che richiede una stringa fissa nell'espressione).
 */
package io.secureflow.core.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect that enforces @RequiresPermission by delegating to RbacPermissionEvaluator.
 */
@Aspect
@Component
public class RequiresPermissionAspect {

    private final RbacPermissionEvaluator permissionEvaluator;

    public RequiresPermissionAspect(RbacPermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * @Around: intercetta le chiamate a metodi con @RequiresPermission. Prima di procedere,
     * legge il valore dell'annotazione (es. "tenant:create"), estrae l'Authentication dal
     * SecurityContextHolder, e chiede a RbacPermissionEvaluator se l'utente ha il permesso.
     * Se no: AccessDeniedException. Se sì: joinPoint.proceed() esegue il metodo.
     */
    @Around("@annotation(io.secureflow.core.security.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        String requiredPermission = annotation.value();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Authentication required");
        }

        if (!permissionEvaluator.hasPermission(auth, null, requiredPermission)) {
            throw new AccessDeniedException("Permission required: " + requiredPermission);
        }

        return joinPoint.proceed();
    }
}
