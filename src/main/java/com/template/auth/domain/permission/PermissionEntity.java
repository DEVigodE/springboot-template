package com.template.auth.domain.permission;

import com.template.auth.domain.shared.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public class PermissionEntity extends BaseEntity {

    private final String name;
    private final String description;

    private PermissionEntity(UUID id, String name, String description, Instant createdAt, Instant updatedAt,
            Instant deletedAt) {
        super(id, createdAt, updatedAt, deletedAt, null, null);
        this.name = name;
        this.description = description;
    }

    public static PermissionEntity create(UUID id, String name, String description, Instant now) {
        return new PermissionEntity(id, name, description, now, now, null);
    }

    public static PermissionEntity reconstitute(UUID id, String name, String description, Instant createdAt,
            Instant updatedAt, Instant deletedAt) {
        return new PermissionEntity(id, name, description, createdAt, updatedAt, deletedAt);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
