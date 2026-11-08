package com.template.auth.application.role.command;

import java.util.UUID;

public record AssignPermissionToRoleCommand(UUID roleId, UUID permissionId) {
}
