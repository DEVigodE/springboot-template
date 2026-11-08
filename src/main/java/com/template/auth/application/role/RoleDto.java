package com.template.auth.application.role;

import com.template.auth.domain.role.RoleEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoleDto(UUID id, String name, String description, List<String> permissions, Instant createdAt,
        Instant updatedAt) {

    public static RoleDto from(RoleEntity role) {
        return new RoleDto(role.getId(), role.getName(), role.getDescription(), role.getPermissions(),
                role.getCreatedAt(), role.getUpdatedAt());
    }
}
