package com.template.auth.domain.role;

import com.template.auth.domain.shared.BaseEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RoleEntity extends BaseEntity {

    private final String name;
    private final String description;
    private final List<String> permissions;

    private RoleEntity(UUID id, String name, String description, List<String> permissions, Instant createdAt,
            Instant updatedAt, Instant deletedAt, UUID createdBy, UUID updatedBy) {
        super(id, createdAt, updatedAt, deletedAt, createdBy, updatedBy);
        this.name = name;
        this.description = description;
        this.permissions = List.copyOf(permissions);
    }

    public static RoleEntity create(UUID id, String name, String description, Instant now) {
        return new RoleEntity(id, name, description, List.of(), now, now, null, null, null);
    }

    public static RoleEntity reconstitute(UUID id, String name, String description, List<String> permissions,
            Instant createdAt, Instant updatedAt, Instant deletedAt) {
        return new RoleEntity(id, name, description, permissions, createdAt, updatedAt, deletedAt, null, null);
    }

    public boolean hasPermission(String permissionName) {
        return permissions.contains(permissionName);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
