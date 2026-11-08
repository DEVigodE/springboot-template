package com.template.auth.application.user;

import com.template.auth.domain.user.UserEntity;

/** Issues the short-lived access token (JWT in infrastructure). */
public interface TokenServicePort {

    String generateToken(UserEntity user);
}
