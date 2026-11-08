package com.template.auth.presentation.shared.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Coarse role gate (e.g. {@code admin}) for the simpler cases; prefer
 * {@link RequirePermission} where a permission string exists. Roles are
 * granted as {@code ROLE_<name>} authorities, which is what hasRole checks.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('{value}')")
public @interface RequireRole {

    String value();
}
