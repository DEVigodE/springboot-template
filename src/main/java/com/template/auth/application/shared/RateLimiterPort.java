package com.template.auth.application.shared;

import java.time.Duration;

/** Fixed-window counter shared across instances (Redis-backed in infrastructure). */
public interface RateLimiterPort {

    /** Registers one hit for {@code key}; false when the window's limit is exceeded. */
    boolean tryAcquire(String key, int limit, Duration window);
}
