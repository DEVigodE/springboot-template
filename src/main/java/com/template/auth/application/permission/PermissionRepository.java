package com.template.auth.application.permission;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.permission.PermissionEntity;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {

    Optional<PermissionEntity> findByName(String name);

    Optional<PermissionEntity> findById(UUID id);

    PaginatedResult<PermissionEntity> findAll(PaginationParams params);

    PermissionEntity save(PermissionEntity permission);

    boolean nameExists(String name);

    void softDelete(UUID id);
}
