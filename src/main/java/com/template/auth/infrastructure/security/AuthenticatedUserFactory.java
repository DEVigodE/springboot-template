package com.template.auth.infrastructure.security;

import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.domain.user.UserEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * One place that decides how a domain user becomes a Spring Security
 * Authentication: roles become {@code ROLE_<name>} (for {@code hasRole}) and
 * permission strings are granted verbatim (for {@code hasAuthority}).
 */
public final class AuthenticatedUserFactory {

    private AuthenticatedUserFactory() {
    }

    public static Authentication authenticationFor(UserEntity user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRoles(),
                user.getPermissions());

        List<GrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        user.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }
}
