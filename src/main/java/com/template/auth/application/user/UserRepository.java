package com.template.auth.application.user;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.user.UserEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Port. Every read filters soft-deleted rows out; roles/permissions come back
 * flattened on the entity (only non-deleted roles and permissions count).
 */
public interface UserRepository {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(UUID id);

    PaginatedResult<UserEntity> findAll(PaginationParams params);

    UserEntity save(UserEntity user);

    boolean emailExists(String email, UUID excludeUserId);

    void softDelete(UUID id, UUID deletedBy);

    void assignRole(UUID userId, UUID roleId);

    void removeRole(UUID userId, UUID roleId);
}
