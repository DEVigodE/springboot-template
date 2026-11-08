package com.template.auth.presentation.role;

import com.template.auth.application.role.RoleDto;
import com.template.auth.application.role.command.AssignPermissionToRoleCommand;
import com.template.auth.application.role.command.AssignPermissionToRoleHandler;
import com.template.auth.application.role.command.CreateRoleCommand;
import com.template.auth.application.role.command.CreateRoleHandler;
import com.template.auth.application.role.command.DeleteRoleCommand;
import com.template.auth.application.role.command.DeleteRoleHandler;
import com.template.auth.application.role.command.RemovePermissionFromRoleCommand;
import com.template.auth.application.role.command.RemovePermissionFromRoleHandler;
import com.template.auth.application.role.query.ListRolesHandler;
import com.template.auth.application.role.query.ListRolesQuery;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.application.user.command.AssignRoleToUserCommand;
import com.template.auth.application.user.command.AssignRoleToUserHandler;
import com.template.auth.application.user.command.RemoveRoleFromUserCommand;
import com.template.auth.application.user.command.RemoveRoleFromUserHandler;
import com.template.auth.presentation.role.dto.AssignPermissionRequest;
import com.template.auth.presentation.role.dto.CreateRoleRequest;
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
@RequestMapping(ApiPaths.V1 + "/roles")
@Tag(name = "Roles")
public class RolesController {

    private final CreateRoleHandler createRole;
    private final ListRolesHandler listRoles;
    private final DeleteRoleHandler deleteRole;
    private final AssignPermissionToRoleHandler assignPermission;
    private final RemovePermissionFromRoleHandler removePermission;
    private final AssignRoleToUserHandler assignToUser;
    private final RemoveRoleFromUserHandler removeFromUser;

    public RolesController(CreateRoleHandler createRole, ListRolesHandler listRoles, DeleteRoleHandler deleteRole,
            AssignPermissionToRoleHandler assignPermission, RemovePermissionFromRoleHandler removePermission,
            AssignRoleToUserHandler assignToUser, RemoveRoleFromUserHandler removeFromUser) {
        this.createRole = createRole;
        this.listRoles = listRoles;
        this.deleteRole = deleteRole;
        this.assignPermission = assignPermission;
        this.removePermission = removePermission;
        this.assignToUser = assignToUser;
        this.removeFromUser = removeFromUser;
    }

    @PostMapping
    @RequirePermission("roles:write")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRoleHandler.Result create(@Valid @RequestBody CreateRoleRequest body) {
        return createRole.execute(new CreateRoleCommand(body.name(), body.description()));
    }

    @GetMapping
    @RequirePermission("roles:read")
    public PaginatedResult<RoleDto> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return listRoles.execute(new ListRolesQuery(PaginationParams.of(page, limit)));
    }

    @DeleteMapping("/{roleId}")
    @RequirePermission("roles:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID roleId, @AuthenticationPrincipal AuthenticatedUser actor) {
        deleteRole.execute(new DeleteRoleCommand(roleId, actor.id()));
    }

    @PostMapping("/{roleId}/permissions")
    @RequirePermission("roles:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPermission(@PathVariable UUID roleId, @Valid @RequestBody AssignPermissionRequest body) {
        assignPermission.execute(new AssignPermissionToRoleCommand(roleId, body.permissionId()));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @RequirePermission("roles:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePermission(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        removePermission.execute(new RemovePermissionFromRoleCommand(roleId, permissionId));
    }

    @PostMapping("/{roleId}/users/{userId}")
    @RequirePermission("roles:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignToUser(@PathVariable UUID roleId, @PathVariable UUID userId) {
        assignToUser.execute(new AssignRoleToUserCommand(userId, roleId));
    }

    @DeleteMapping("/{roleId}/users/{userId}")
    @RequirePermission("roles:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromUser(@PathVariable UUID roleId, @PathVariable UUID userId) {
        removeFromUser.execute(new RemoveRoleFromUserCommand(userId, roleId));
    }
}
