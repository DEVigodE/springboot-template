package com.template.auth.infrastructure.shared;

import com.template.auth.application.shared.CachePort;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * CachePort on the dedicated cache Redis (CACHE_REDIS_URL). Values are stored
 * as JSON strings; INCR gives the atomic counter the login lockout needs.
 */
@Component
public class RedisCache implements CachePort {

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisCache(@Qualifier("cacheRedisTemplate") StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        String raw = redis.opsForValue().get(key);
        return raw == null ? Optional.empty() : Optional.of(json.readValue(raw, type));
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, json.writeValueAsString(value), ttl);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        redis.expire(key, ttl);
        return value == null ? 0L : value;
    }
}
