package com.template.auth.application.notification.command;

import java.util.UUID;

public record MarkNotificationAsReadCommand(UUID notificationId, UUID userId) {
}
