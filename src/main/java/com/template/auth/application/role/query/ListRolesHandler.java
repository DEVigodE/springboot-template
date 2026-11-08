package com.template.auth.application.role.query;

import com.template.auth.application.role.RoleDto;
import com.template.auth.application.role.RoleRepository;
import com.template.auth.application.shared.PaginatedResult;
import org.springframework.stereotype.Service;

@Service
public class ListRolesHandler {

    private final RoleRepository roleRepository;

    public ListRolesHandler(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public PaginatedResult<RoleDto> execute(ListRolesQuery query) {
        return roleRepository.findAll(query.pagination()).map(RoleDto::from);
    }
}
