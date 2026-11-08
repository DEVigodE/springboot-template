package com.template.auth.application.shared;

import java.time.Duration;
import java.util.Optional;

/** Cache-aside store plus the atomic counter the login lockout relies on. */
public interface CachePort {

    <T> Optional<T> get(String key, Class<T> type);

    void set(String key, Object value, Duration ttl);

    void delete(String key);

    /**
     * Atomically increments the counter at {@code key} by 1 and refreshes its TTL,
     * avoiding the lost-update race a get-then-set couplet has under concurrent
     * callers. Returns the new value.
     */
    long increment(String key, Duration ttl);
}
