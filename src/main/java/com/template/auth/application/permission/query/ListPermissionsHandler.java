package com.template.auth.application.permission.query;

import com.template.auth.application.permission.PermissionDto;
import com.template.auth.application.permission.PermissionRepository;
import com.template.auth.application.shared.PaginatedResult;
import org.springframework.stereotype.Service;

@Service
public class ListPermissionsHandler {

    private final PermissionRepository permissionRepository;

    public ListPermissionsHandler(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public PaginatedResult<PermissionDto> execute(ListPermissionsQuery query) {
        return permissionRepository.findAll(query.pagination()).map(PermissionDto::from);
    }
}
