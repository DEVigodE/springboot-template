package com.template.auth.application.health;

/** Dependency probes; each must return quickly and never throw. */
public interface HealthRepositoryPort {

    boolean checkDatabase();

    boolean checkCacheRedis();

    boolean checkQueueRedis();
}
