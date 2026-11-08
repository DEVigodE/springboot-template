package com.template.auth.presentation.user.dto;

import com.template.auth.application.shared.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id, String email, List<String> roles, List<String> permissions) {

    public static ProfileResponse from(AuthenticatedUser user) {
        return new ProfileResponse(user.id(), user.email(), user.roles(), user.permissions());
    }
}
