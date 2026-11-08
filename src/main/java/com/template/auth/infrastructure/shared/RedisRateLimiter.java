package com.template.auth.infrastructure.shared;

import com.template.auth.application.shared.RateLimiterPort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window limiter (INCR + EXPIRE on first hit) on the cache Redis. Shared
 * by every instance, so the effective limit stays what was configured instead
 * of (limit x replica count) as an in-memory counter would give.
 */
@Component
public class RedisRateLimiter implements RateLimiterPort {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(@Qualifier("cacheRedisTemplate") StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redis.expire(key, window);
        }
        return count <= limit;
    }
}
