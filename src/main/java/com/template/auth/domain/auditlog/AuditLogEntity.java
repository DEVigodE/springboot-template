package com.template.auth.domain.auditlog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Append-only record of a sensitive action. Immutable once created. */
public class AuditLogEntity {

    private final UUID id;
    private final UUID actorId;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final Map<String, Object> metadata;
    private final Instant createdAt;

    private AuditLogEntity(UUID id, UUID actorId, String action, String resourceType, String resourceId,
            Map<String, Object> metadata, Instant createdAt) {
        this.id = id;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata == null ? null : Map.copyOf(metadata);
        this.createdAt = createdAt;
    }

    public static AuditLogEntity create(UUID id, UUID actorId, String action, String resourceType,
            String resourceId, Map<String, Object> metadata, Instant createdAt) {
        return new AuditLogEntity(id, actorId, action, resourceType, resourceId, metadata, createdAt);
    }

    public static AuditLogEntity reconstitute(UUID id, UUID actorId, String action, String resourceType,
            String resourceId, Map<String, Object> metadata, Instant createdAt) {
        return new AuditLogEntity(id, actorId, action, resourceType, resourceId, metadata, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
