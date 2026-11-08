package com.template.auth.infrastructure.persistence.user;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.user.UserEntity;
import com.template.auth.infrastructure.persistence.permission.PermissionJpaEntity;
import com.template.auth.infrastructure.persistence.role.RoleJpaEntity;
import com.template.auth.infrastructure.persistence.role.RoleJpaRepository;
import com.template.auth.infrastructure.persistence.shared.PageMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final RoleJpaRepository roles;

    public UserRepositoryAdapter(UserJpaRepository jpa, RoleJpaRepository roles) {
        this.jpa = jpa;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return jpa.findWithRolesByEmailAndDeletedAtIsNull(email).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserEntity> findById(UUID id) {
        return jpa.findWithRolesByIdAndDeletedAtIsNull(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<UserEntity> findAll(PaginationParams params) {
        Page<UUID> ids = jpa.findIdsByDeletedAtIsNull(PageMapper.toPageable(params, Sort.by("createdAt").ascending()));
        Map<UUID, UserJpaEntity> byId = jpa.findWithRolesByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, Function.identity()));
        List<UserEntity> items = ids.getContent().stream().map(byId::get).map(UserRepositoryAdapter::toDomain)
                .toList();
        return PaginatedResult.of(items, ids.getTotalElements(), params);
    }

    @Override
    public UserEntity save(UserEntity user) {
        UserJpaEntity entity = jpa.findById(user.getId()).orElseGet(UserJpaEntity::new);
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setActive(user.isEnabled());
        entity.setEmailVerifiedAt(user.getEmailVerifiedAt());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setDeletedAt(user.getDeletedAt());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setUpdatedBy(user.getUpdatedBy());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email, UUID excludeUserId) {
        return excludeUserId == null
                ? jpa.existsByEmailAndDeletedAtIsNull(email)
                : jpa.existsByEmailAndDeletedAtIsNullAndIdNot(email, excludeUserId);
    }

    @Override
    public void softDelete(UUID id, UUID deletedBy) {
        jpa.findById(id).ifPresent(entity -> {
            Instant now = Instant.now();
            entity.setDeletedAt(now);
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(deletedBy);
            jpa.save(entity);
        });
    }

    @Override
    public void assignRole(UUID userId, UUID roleId) {
        jpa.findById(userId).ifPresent(user -> {
            RoleJpaEntity role = roles.getReferenceById(roleId);
            if (user.getRoles().add(role)) {
                jpa.save(user);
            }
        });
    }

    @Override
    public void removeRole(UUID userId, UUID roleId) {
        jpa.findById(userId).ifPresent(user -> {
            if (user.getRoles().removeIf(r -> r.getId().equals(roleId))) {
                jpa.save(user);
            }
        });
    }

    static UserEntity toDomain(UserJpaEntity entity) {
        List<RoleJpaEntity> activeRoles = entity.getRoles().stream()
                .filter(role -> role.getDeletedAt() == null)
                .toList();

        List<String> roleNames = activeRoles.stream().map(RoleJpaEntity::getName).sorted().toList();
        TreeSet<String> permissionNames = new TreeSet<>();
        for (RoleJpaEntity role : activeRoles) {
            for (PermissionJpaEntity permission : role.getPermissions()) {
                if (permission.getDeletedAt() == null) {
                    permissionNames.add(permission.getName());
                }
            }
        }

        return UserEntity.reconstitute(entity.getId(), entity.getName(), entity.getEmail(),
                entity.getPasswordHash(), entity.isActive(), entity.getEmailVerifiedAt(), roleNames,
                List.copyOf(permissionNames), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getDeletedAt(), entity.getCreatedBy(), entity.getUpdatedBy());
    }
}
