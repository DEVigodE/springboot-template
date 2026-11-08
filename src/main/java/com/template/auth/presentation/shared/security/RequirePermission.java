package com.template.auth.presentation.shared.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Fine-grained gate on a permission string (e.g. {@code users:write}), checked
 * against the caller's flattened permissions (JwtAuthenticationFilter grants
 * each one as an authority). Expands via Spring Security's annotation
 * templates - see MethodSecurityConfig.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('{value}')")
public @interface RequirePermission {

    String value();
}
