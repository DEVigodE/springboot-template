package com.template.auth.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps this instance subscribed to the notification channel. The container's
 * own start() fails hard when Redis is unreachable, so instead of letting that
 * abort the boot (the push is best-effort by contract) it is started here and
 * retried every few seconds until it sticks; the container then handles later
 * connection drops with its built-in recovery.
 */
@Component
public class NotificationSubscriptionKeeper {

    private static final Logger log = LoggerFactory.getLogger(NotificationSubscriptionKeeper.class);

    private final RedisMessageListenerContainer container;

    public NotificationSubscriptionKeeper(
            @Qualifier("notificationListenerContainer") RedisMessageListenerContainer container) {
        this.container = container;
    }

    @Scheduled(fixedDelay = 5_000)
    public void ensureSubscribed() {
        if (container.isRunning()) {
            return;
        }
        try {
            container.start();
            log.info("Subscribed to Redis channel {} for WebSocket notification fan-out",
                    RedisNotificationPublisher.CHANNEL);
        } catch (RuntimeException e) {
            log.warn("Notification pub/sub not available yet ({}); WS push degraded, REST unaffected",
                    e.getMessage());
            try {
                container.stop();
            } catch (RuntimeException ignored) {
                // Best effort: leave the container in a clean state for the next attempt.
            }
        }
    }
}
