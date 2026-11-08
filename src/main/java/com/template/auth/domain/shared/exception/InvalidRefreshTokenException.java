package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
