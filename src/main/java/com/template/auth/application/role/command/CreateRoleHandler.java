package com.template.auth.application.role.command;

import com.template.auth.application.role.RoleRepository;
import com.template.auth.domain.role.RoleEntity;
import com.template.auth.domain.shared.exception.RoleAlreadyExistsException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreateRoleHandler {

    private final RoleRepository roleRepository;

    public CreateRoleHandler(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public record Result(UUID id) {
    }

    public Result execute(CreateRoleCommand command) {
        if (roleRepository.nameExists(command.name())) {
            throw new RoleAlreadyExistsException(command.name());
        }

        RoleEntity saved = roleRepository.save(
                RoleEntity.create(UUID.randomUUID(), command.name(), command.description(), Instant.now()));

        return new Result(saved.getId());
    }
}
