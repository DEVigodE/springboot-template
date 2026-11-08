package com.template.auth.infrastructure.persistence.auditlog;

import com.template.auth.application.auditlog.AuditLogRepository;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.auditlog.AuditLogEntity;
import com.template.auth.infrastructure.persistence.shared.PageMapper;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
@Transactional
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuditLogJpaRepository jpa;
    private final JsonMapper json;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpa, JsonMapper json) {
        this.jpa = jpa;
        this.json = json;
    }

    @Override
    public AuditLogEntity create(AuditLogEntity log) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(log.getId());
        entity.setActorId(log.getActorId());
        entity.setAction(log.getAction());
        entity.setResourceType(log.getResourceType());
        entity.setResourceId(log.getResourceId());
        entity.setMetadata(log.getMetadata() == null ? null : json.writeValueAsString(log.getMetadata()));
        entity.setCreatedAt(log.getCreatedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<AuditLogEntity> findAll(PaginationParams params) {
        var page = jpa.findAll(PageMapper.toPageable(params, Sort.by("createdAt").descending()));
        return PageMapper.toResult(page, params, this::toDomain);
    }

    private AuditLogEntity toDomain(AuditLogJpaEntity entity) {
        Map<String, Object> metadata = entity.getMetadata() == null ? null
                : json.readValue(entity.getMetadata(), MAP_TYPE);
        return AuditLogEntity.reconstitute(entity.getId(), entity.getActorId(), entity.getAction(),
                entity.getResourceType(), entity.getResourceId(), metadata, entity.getCreatedAt());
    }
}
