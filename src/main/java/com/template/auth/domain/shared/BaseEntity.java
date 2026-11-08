package com.template.auth.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared identity + audit columns for soft-deletable aggregates (users, roles,
 * permissions). Pure Java - no framework or persistence annotations here.
 */
public abstract class BaseEntity {

    private final UUID id;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected BaseEntity(UUID id, Instant createdAt, Instant updatedAt, Instant deletedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    /** Not soft-deleted. (UserEntity additionally has an explicit `active` flag.) */
    public boolean isActive() {
        return deletedAt == null;
    }

    public void markAsDeleted(UUID deletedBy) {
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
        this.updatedBy = deletedBy;
    }

    public void registerUpdate(UUID updatedBy) {
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;
    }

    public void registerUpdate() {
        registerUpdate(null);
    }
}
