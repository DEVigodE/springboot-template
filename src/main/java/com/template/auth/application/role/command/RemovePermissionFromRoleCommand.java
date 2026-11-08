package com.template.auth.application.role.command;

import java.util.UUID;

public record RemovePermissionFromRoleCommand(UUID roleId, UUID permissionId) {
}
