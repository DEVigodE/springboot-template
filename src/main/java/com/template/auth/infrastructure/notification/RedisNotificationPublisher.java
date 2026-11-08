package com.template.auth.infrastructure.notification;

import com.template.auth.application.notification.NotificationDto;
import com.template.auth.application.notification.NotificationPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes new notifications on a Redis channel. Every instance subscribes
 * (see NotificationWebSocketForwarder) and pushes to the sockets *it* holds -
 * without this, a notification created on one replica would never reach a
 * user connected to another.
 */
@Component
public class RedisNotificationPublisher implements NotificationPublisherPort {

    public static final String CHANNEL = "notifications:events";

    private static final Logger log = LoggerFactory.getLogger(RedisNotificationPublisher.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisNotificationPublisher(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Override
    public void publish(NotificationDto notification) {
        try {
            redis.convertAndSend(CHANNEL, json.writeValueAsString(notification));
        } catch (DataAccessException e) {
            // Best-effort by contract: the notification is already persisted.
            log.warn("Could not publish notification {}: {}", notification.id(), e.getMessage());
        }
    }
}
