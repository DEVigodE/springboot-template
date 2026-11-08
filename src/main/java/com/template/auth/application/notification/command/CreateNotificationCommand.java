package com.template.auth.application.notification.command;

import java.util.UUID;

public record CreateNotificationCommand(UUID userId, String type, String title, String message) {
}
