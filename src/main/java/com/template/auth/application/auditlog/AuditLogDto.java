package com.template.auth.application.auditlog;

import com.template.auth.domain.auditlog.AuditLogEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(UUID id, UUID actorId, String action, String resourceType, String resourceId,
        Map<String, Object> metadata, Instant createdAt) {

    public static AuditLogDto from(AuditLogEntity log) {
        return new AuditLogDto(log.getId(), log.getActorId(), log.getAction(), log.getResourceType(),
                log.getResourceId(), log.getMetadata(), log.getCreatedAt());
    }
}
