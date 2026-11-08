package com.template.auth.application.permission.command;

import java.util.UUID;

public record DeletePermissionCommand(UUID permissionId, UUID actorId) {
}
