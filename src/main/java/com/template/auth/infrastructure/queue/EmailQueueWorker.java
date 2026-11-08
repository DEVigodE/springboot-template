package com.template.auth.infrastructure.queue;

import com.template.auth.application.shared.EmailSenderPort;
import java.util.Map;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailQueueWorker extends RedisStreamWorker {

    private final EmailSenderPort emailSender;

    public EmailQueueWorker(RedisConnectionFactory connectionFactory, StringRedisTemplate redis,
            RedisStreamQueue queue, EmailSenderPort emailSender) {
        super(connectionFactory, redis, queue, QueueNames.EMAILS);
        this.emailSender = emailSender;
    }

    @Override
    protected void process(Map<String, String> payload) {
        emailSender.send(payload.get("to"), payload.get("subject"), payload.get("body"));
    }
}
