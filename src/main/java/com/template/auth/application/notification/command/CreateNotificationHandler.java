package com.template.auth.application.notification.command;

import com.template.auth.application.notification.NotificationDto;
import com.template.auth.application.notification.NotificationPublisherPort;
import com.template.auth.application.notification.NotificationRepository;
import com.template.auth.domain.notification.NotificationEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreateNotificationHandler {

    private final NotificationRepository notificationRepository;
    private final NotificationPublisherPort notificationPublisher;

    public CreateNotificationHandler(NotificationRepository notificationRepository,
            NotificationPublisherPort notificationPublisher) {
        this.notificationRepository = notificationRepository;
        this.notificationPublisher = notificationPublisher;
    }

    public record Result(UUID id) {
    }

    public Result execute(CreateNotificationCommand command) {
        NotificationEntity saved = notificationRepository.create(NotificationEntity.create(UUID.randomUUID(),
                command.userId(), command.type(), command.title(), command.message(), Instant.now()));

        // Best-effort - see NotificationPublisherPort. The notification is
        // already durably saved above regardless of whether anyone is connected.
        notificationPublisher.publish(NotificationDto.from(saved));

        return new Result(saved.getId());
    }
}
