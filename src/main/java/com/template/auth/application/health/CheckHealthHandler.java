package com.template.auth.application.health;

import org.springframework.stereotype.Service;

@Service
public class CheckHealthHandler {

    private final HealthRepositoryPort healthRepository;

    public CheckHealthHandler(HealthRepositoryPort healthRepository) {
        this.healthRepository = healthRepository;
    }

    public record HealthCheckResult(boolean healthy, boolean database, boolean redis, boolean queue) {
    }

    public HealthCheckResult execute() {
        boolean databaseUp = healthRepository.checkDatabase();
        boolean redisUp = healthRepository.checkCacheRedis();
        boolean queueUp = healthRepository.checkQueueRedis();

        return new HealthCheckResult(databaseUp && redisUp && queueUp, databaseUp, redisUp, queueUp);
    }
}
