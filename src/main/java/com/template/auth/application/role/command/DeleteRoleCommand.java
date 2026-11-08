package com.template.auth.application.role.command;

import java.util.UUID;

public record DeleteRoleCommand(UUID roleId, UUID actorId) {
}
