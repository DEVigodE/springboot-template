package com.template.auth.application.permission;

import com.template.auth.domain.permission.PermissionEntity;
import java.time.Instant;
import java.util.UUID;

public record PermissionDto(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {

    public static PermissionDto from(PermissionEntity permission) {
        return new PermissionDto(permission.getId(), permission.getName(), permission.getDescription(),
                permission.getCreatedAt(), permission.getUpdatedAt());
    }
}
