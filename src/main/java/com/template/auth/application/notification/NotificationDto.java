package com.template.auth.application.notification;

import com.template.auth.domain.notification.NotificationEntity;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(UUID id, UUID userId, String type, String title, String message, Instant readAt,
        Instant createdAt) {

    public static NotificationDto from(NotificationEntity notification) {
        return new NotificationDto(notification.getId(), notification.getUserId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getReadAt(),
                notification.getCreatedAt());
    }
}
