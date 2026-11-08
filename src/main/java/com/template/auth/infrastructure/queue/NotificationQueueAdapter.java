package com.template.auth.infrastructure.queue;

import com.template.auth.application.shared.NotificationQueuePort;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationQueueAdapter implements NotificationQueuePort {

    private final RedisStreamQueue queue;

    public NotificationQueueAdapter(RedisStreamQueue queue) {
        this.queue = queue;
    }

    @Override
    public void enqueue(NotificationJob job) {
        queue.enqueue(QueueNames.NOTIFICATIONS, Map.of(
                "userId", job.userId().toString(),
                "type", job.type(),
                "title", job.title(),
                "message", job.message()));
    }
}
