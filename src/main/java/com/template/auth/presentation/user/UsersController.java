package com.template.auth.presentation.user;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.application.user.UserDto;
import com.template.auth.application.user.command.ChangePasswordCommand;
import com.template.auth.application.user.command.ChangePasswordHandler;
import com.template.auth.application.user.command.DeleteUserCommand;
import com.template.auth.application.user.command.DeleteUserHandler;
import com.template.auth.application.user.command.UpdateUserCommand;
import com.template.auth.application.user.command.UpdateUserHandler;
import com.template.auth.application.user.query.GetUserHandler;
import com.template.auth.application.user.query.GetUserQuery;
import com.template.auth.application.user.query.ListUsersHandler;
import com.template.auth.application.user.query.ListUsersQuery;
import com.template.auth.presentation.shared.ApiPaths;
import com.template.auth.presentation.shared.security.RequirePermission;
import com.template.auth.presentation.user.dto.ChangePasswordRequest;
import com.template.auth.presentation.user.dto.UpdateProfileRequest;
import com.template.auth.presentation.user.dto.UpdateUserRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/users")
@Tag(name = "Users")
public class UsersController {

    private final ListUsersHandler listUsers;
    private final GetUserHandler getUser;
    private final UpdateUserHandler updateUser;
    private final ChangePasswordHandler changePassword;
    private final DeleteUserHandler deleteUser;

    public UsersController(ListUsersHandler listUsers, GetUserHandler getUser, UpdateUserHandler updateUser,
            ChangePasswordHandler changePassword, DeleteUserHandler deleteUser) {
        this.listUsers = listUsers;
        this.getUser = getUser;
        this.updateUser = updateUser;
        this.changePassword = changePassword;
        this.deleteUser = deleteUser;
    }

    @GetMapping
    @RequirePermission("users:read")
    public PaginatedResult<UserDto> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return listUsers.execute(new ListUsersQuery(PaginationParams.of(page, limit)));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthenticatedUser user) {
        return getUser.execute(new GetUserQuery(user.id()));
    }

    @PatchMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest body) {
        return updateUser.execute(new UpdateUserCommand(user.id(), body.name(), body.email(), null));
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest body) {
        changePassword.execute(new ChangePasswordCommand(user.id(), body.currentPassword(), body.newPassword()));
    }

    /** Self-service account deletion. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal AuthenticatedUser user) {
        deleteUser.execute(new DeleteUserCommand(user.id(), user.id()));
    }

    @GetMapping("/{userId}")
    @RequirePermission("users:read")
    public UserDto getById(@PathVariable UUID userId) {
        return getUser.execute(new GetUserQuery(userId));
    }

    @PatchMapping("/{userId}")
    @RequirePermission("users:write")
    public UserDto updateById(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest body) {
        return updateUser.execute(new UpdateUserCommand(userId, body.name(), body.email(), body.active()));
    }

    /** Admin-initiated deletion. */
    @DeleteMapping("/{userId}")
    @RequirePermission("users:write")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, @AuthenticationPrincipal AuthenticatedUser actor) {
        deleteUser.execute(new DeleteUserCommand(userId, actor.id()));
    }
}
