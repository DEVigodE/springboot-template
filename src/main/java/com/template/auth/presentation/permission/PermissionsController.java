package com.template.auth.presentation.permission;

import com.template.auth.application.permission.PermissionDto;
import com.template.auth.application.permission.command.CreatePermissionCommand;
import com.template.auth.application.permission.command.CreatePermissionHandler;
import com.template.auth.application.permission.command.DeletePermissionCommand;
import com.template.auth.application.permission.command.DeletePermissionHandler;
import com.template.auth.application.permission.query.ListPermissionsHandler;
import com.template.auth.application.permission.query.ListPermissionsQuery;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.presentation.permission.dto.CreatePermissionRequest;
import com.template.auth.presentation.shared.ApiPaths;
import com.template.auth.presentation.shared.security.RequirePermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/permissions")
@Tag(name = "Permissions")
public class PermissionsController {

    private final CreatePermissionHandler createPermission;
    private final ListPermissionsHandler listPermissions;
    private final DeletePermissionHandler deletePermission;

    public PermissionsController(CreatePermissionHandler createPermission, ListPermissionsHandler listPermissions,
            DeletePermissionHandler deletePermission) {
        this.createPermission = createPermission;
        this.listPermissions = listPermissions;
        this.deletePermission = deletePermission;
    }

    @PostMapping
    @RequirePermission("permissions:write")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePermissionHandler.Result create(@Valid @RequestBody CreatePermissionRequest body) {
        return createPermission.execute(new CreatePermissionCommand(body.name(), body.description()));
    }

    @GetMapping
    @RequirePermission("permissions:read")
    public PaginatedResult<PermissionDto> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return listPermissions.execute(new ListPermissionsQuery(PaginationParams.of(page, limit)));
    }

    @DeleteMapping("/{permissionId}")
    @RequirePermission("permissions:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID permissionId, @AuthenticationPrincipal AuthenticatedUser actor) {
        deletePermission.execute(new DeletePermissionCommand(permissionId, actor.id()));
    }
}
