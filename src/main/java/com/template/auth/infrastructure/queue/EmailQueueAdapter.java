package com.template.auth.infrastructure.queue;

import com.template.auth.application.shared.EmailQueuePort;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmailQueueAdapter implements EmailQueuePort {

    private final RedisStreamQueue queue;

    public EmailQueueAdapter(RedisStreamQueue queue) {
        this.queue = queue;
    }

    @Override
    public void enqueue(EmailJob job) {
        queue.enqueue(QueueNames.EMAILS, Map.of(
                "to", job.to(),
                "subject", job.subject(),
                "body", job.body()));
    }
}
