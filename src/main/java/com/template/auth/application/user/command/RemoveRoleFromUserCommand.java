package com.template.auth.application.user.command;

import java.util.UUID;

public record RemoveRoleFromUserCommand(UUID userId, UUID roleId) {
}
