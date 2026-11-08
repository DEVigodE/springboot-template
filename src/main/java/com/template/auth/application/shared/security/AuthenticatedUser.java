package com.template.auth.application.shared.security;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * The caller, as resolved from a valid access token (see JwtAuthenticationFilter).
 * Roles and permissions are re-read from the DB on every request, so a revoked
 * role or a deactivated account takes effect immediately, not at token expiry.
 *
 * Implements {@link Principal} so Spring's user-destination WebSocket routing
 * ({@code convertAndSendToUser}) can address a user by id.
 */
public record AuthenticatedUser(UUID id, String email, List<String> roles, List<String> permissions)
        implements Principal {

    public AuthenticatedUser {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }

    @Override
    public String getName() {
        return id.toString();
    }
}
