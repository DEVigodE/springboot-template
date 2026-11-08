package com.template.auth.application.user.command;

import com.template.auth.application.role.RoleRepository;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.role.RoleEntity;
import com.template.auth.domain.shared.exception.RoleNotFoundException;
import com.template.auth.domain.shared.exception.UserNotFoundException;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class AssignRoleToUserHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CachePort cache;

    public AssignRoleToUserHandler(UserRepository userRepository, RoleRepository roleRepository, CachePort cache) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.cache = cache;
    }

    public void execute(AssignRoleToUserCommand command) {
        UserEntity user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));
        RoleEntity role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId()));

        userRepository.assignRole(user.getId(), role.getId());
        cache.delete(UserCacheKeys.user(user.getId()));
    }
}
