package com.template.auth.presentation.role.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignPermissionRequest(@NotNull UUID permissionId) {
}
