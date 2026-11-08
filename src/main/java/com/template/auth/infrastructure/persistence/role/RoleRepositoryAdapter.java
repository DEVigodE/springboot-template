package com.template.auth.infrastructure.persistence.role;

import com.template.auth.application.role.RoleRepository;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.role.RoleEntity;
import com.template.auth.infrastructure.persistence.permission.PermissionJpaEntity;
import com.template.auth.infrastructure.persistence.permission.PermissionJpaRepository;
import com.template.auth.infrastructure.persistence.shared.PageMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpa;
    private final PermissionJpaRepository permissions;

    public RoleRepositoryAdapter(RoleJpaRepository jpa, PermissionJpaRepository permissions) {
        this.jpa = jpa;
        this.permissions = permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleEntity> findByName(String name) {
        return jpa.findWithPermissionsByNameAndDeletedAtIsNull(name).map(RoleRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleEntity> findById(UUID id) {
        return jpa.findWithPermissionsByIdAndDeletedAtIsNull(id).map(RoleRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<RoleEntity> findAll(PaginationParams params) {
        Page<UUID> ids = jpa.findIdsByDeletedAtIsNull(PageMapper.toPageable(params, Sort.by("createdAt").ascending()));
        Map<UUID, RoleJpaEntity> byId = jpa.findWithPermissionsByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(RoleJpaEntity::getId, Function.identity()));
        List<RoleEntity> items = ids.getContent().stream().map(byId::get).map(RoleRepositoryAdapter::toDomain)
                .toList();
        return PaginatedResult.of(items, ids.getTotalElements(), params);
    }

    @Override
    public RoleEntity save(RoleEntity role) {
        RoleJpaEntity entity = jpa.findById(role.getId()).orElseGet(RoleJpaEntity::new);
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setCreatedAt(role.getCreatedAt());
        entity.setUpdatedAt(role.getUpdatedAt());
        entity.setDeletedAt(role.getDeletedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExists(String name) {
        return jpa.existsByNameAndDeletedAtIsNull(name);
    }

    @Override
    public void assignPermission(UUID roleId, UUID permissionId) {
        jpa.findById(roleId).ifPresent(role -> {
            PermissionJpaEntity permission = permissions.getReferenceById(permissionId);
            if (role.getPermissions().add(permission)) {
                jpa.save(role);
            }
        });
    }

    @Override
    public void removePermission(UUID roleId, UUID permissionId) {
        jpa.findById(roleId).ifPresent(role -> {
            if (role.getPermissions().removeIf(p -> p.getId().equals(permissionId))) {
                jpa.save(role);
            }
        });
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

    static RoleEntity toDomain(RoleJpaEntity entity) {
        List<String> permissionNames = entity.getPermissions().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionJpaEntity::getName)
                .sorted()
                .toList();
        return RoleEntity.reconstitute(entity.getId(), entity.getName(), entity.getDescription(), permissionNames,
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt());
    }
}
