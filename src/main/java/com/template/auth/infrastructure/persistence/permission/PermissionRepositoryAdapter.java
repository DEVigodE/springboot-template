package com.template.auth.infrastructure.persistence.permission;

import com.template.auth.application.permission.PermissionRepository;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.permission.PermissionEntity;
import com.template.auth.infrastructure.persistence.shared.PageMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpa;

    public PermissionRepositoryAdapter(PermissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PermissionEntity> findByName(String name) {
        return jpa.findByNameAndDeletedAtIsNull(name).map(PermissionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PermissionEntity> findById(UUID id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(PermissionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<PermissionEntity> findAll(PaginationParams params) {
        var page = jpa.findAllByDeletedAtIsNull(PageMapper.toPageable(params, Sort.by("createdAt").ascending()));
        return PageMapper.toResult(page, params, PermissionRepositoryAdapter::toDomain);
    }

    @Override
    public PermissionEntity save(PermissionEntity permission) {
        PermissionJpaEntity entity = jpa.findById(permission.getId()).orElseGet(PermissionJpaEntity::new);
        entity.setId(permission.getId());
        entity.setName(permission.getName());
        entity.setDescription(permission.getDescription());
        entity.setCreatedAt(permission.getCreatedAt());
        entity.setUpdatedAt(permission.getUpdatedAt());
        entity.setDeletedAt(permission.getDeletedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExists(String name) {
        return jpa.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public void softDelete(UUID id) {
        jpa.findById(id).ifPresent(entity -> {
            Instant now = Instant.now();
            entity.setDeletedAt(now);
            entity.setUpdatedAt(now);
            jpa.save(entity);
        });
    }

    static PermissionEntity toDomain(PermissionJpaEntity entity) {
        return PermissionEntity.reconstitute(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt());
    }
}
