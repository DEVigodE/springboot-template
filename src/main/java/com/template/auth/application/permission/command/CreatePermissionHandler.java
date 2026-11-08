package com.template.auth.application.permission.command;

import com.template.auth.application.permission.PermissionRepository;
import com.template.auth.domain.permission.PermissionEntity;
import com.template.auth.domain.shared.exception.PermissionAlreadyExistsException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreatePermissionHandler {

    private final PermissionRepository permissionRepository;

    public CreatePermissionHandler(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public record Result(UUID id) {
    }

    public Result execute(CreatePermissionCommand command) {
        if (permissionRepository.nameExists(command.name())) {
            throw new PermissionAlreadyExistsException(command.name());
        }

        PermissionEntity saved = permissionRepository.save(
                PermissionEntity.create(UUID.randomUUID(), command.name(), command.description(), Instant.now()));

        return new Result(saved.getId());
    }
}
