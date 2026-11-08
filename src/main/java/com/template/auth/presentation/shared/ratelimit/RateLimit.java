package com.template.auth.presentation.shared.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Per-route override of the global per-IP limit (100 requests / 60s). Sensitive
 * endpoints (login, register, password reset) use tighter budgets.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    int limit();

    int windowSeconds() default 60;
}
