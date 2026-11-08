package com.template.auth.infrastructure.queue;

import com.template.auth.application.notification.command.CreateNotificationCommand;
import com.template.auth.application.notification.command.CreateNotificationHandler;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationQueueWorker extends RedisStreamWorker {

    private final CreateNotificationHandler createNotification;

    public NotificationQueueWorker(RedisConnectionFactory connectionFactory, StringRedisTemplate redis,
            RedisStreamQueue queue, CreateNotificationHandler createNotification) {
        super(connectionFactory, redis, queue, QueueNames.NOTIFICATIONS);
        this.createNotification = createNotification;
    }

    @Override
    protected void process(Map<String, String> payload) {
        createNotification.execute(new CreateNotificationCommand(UUID.fromString(payload.get("userId")),
                payload.get("type"), payload.get("title"), payload.get("message")));
    }
}
