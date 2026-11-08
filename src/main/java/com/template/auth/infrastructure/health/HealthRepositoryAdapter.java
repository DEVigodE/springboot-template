package com.template.auth.infrastructure.health;

import com.template.auth.application.health.HealthRepositoryPort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The two Redis connections are checked independently: they're separate
 * clients against separate servers, so pinging one says nothing about the other.
 */
@Component
public class HealthRepositoryAdapter implements HealthRepositoryPort {

    private static final String REDIS_PING_KEY = "health:ping";

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate cacheRedis;
    private final StringRedisTemplate queueRedis;

    public HealthRepositoryAdapter(JdbcTemplate jdbc,
            @Qualifier("cacheRedisTemplate") StringRedisTemplate cacheRedis,
            @Qualifier("queueRedisTemplate") StringRedisTemplate queueRedis) {
        this.jdbc = jdbc;
        this.cacheRedis = cacheRedis;
        this.queueRedis = queueRedis;
    }

    @Override
    public boolean checkDatabase() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean checkCacheRedis() {
        try {
            cacheRedis.opsForValue().set(REDIS_PING_KEY, "ok", Duration.ofSeconds(5));
            return "ok".equals(cacheRedis.opsForValue().get(REDIS_PING_KEY));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean checkQueueRedis() {
        try {
            String pong = queueRedis.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(pong);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
