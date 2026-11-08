package com.template.auth.application.role.command;

import com.template.auth.application.permission.PermissionRepository;
import com.template.auth.application.role.RoleRepository;
import com.template.auth.domain.permission.PermissionEntity;
import com.template.auth.domain.role.RoleEntity;
import com.template.auth.domain.shared.exception.PermissionNotFoundException;
import com.template.auth.domain.shared.exception.RoleNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RemovePermissionFromRoleHandler {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RemovePermissionFromRoleHandler(RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public void execute(RemovePermissionFromRoleCommand command) {
        RoleEntity role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId()));
        PermissionEntity permission = permissionRepository.findById(command.permissionId())
                .orElseThrow(() -> new PermissionNotFoundException(command.permissionId()));

        roleRepository.removePermission(role.getId(), permission.getId());
    }
}
