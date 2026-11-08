package com.template.auth.application.user.command;

import java.util.UUID;

public record ChangePasswordCommand(UUID userId, String currentPassword, String newPassword) {
}
