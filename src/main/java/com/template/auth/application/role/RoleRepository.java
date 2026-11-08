package com.template.auth.application.role;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.role.RoleEntity;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    Optional<RoleEntity> findByName(String name);

    Optional<RoleEntity> findById(UUID id);

    PaginatedResult<RoleEntity> findAll(PaginationParams params);

    RoleEntity save(RoleEntity role);

    boolean nameExists(String name);

    void assignPermission(UUID roleId, UUID permissionId);

    void removePermission(UUID roleId, UUID permissionId);

    void softDelete(UUID id);
}
