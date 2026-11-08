package com.template.auth.application.user;

import com.template.auth.domain.user.UserEntity;
import java.util.UUID;

/** Public shape of a user - never exposes the password hash. Also what gets cached. */
public record UserDto(UUID id, String name, String email, boolean active, boolean emailVerified) {

    public static UserDto from(UserEntity user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.isEnabled(),
                user.getEmailVerifiedAt() != null);
    }
}
