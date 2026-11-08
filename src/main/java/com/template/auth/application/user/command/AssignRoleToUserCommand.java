package com.template.auth.application.user.command;

import java.util.UUID;

public record AssignRoleToUserCommand(UUID userId, UUID roleId) {
}
