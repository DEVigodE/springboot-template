package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidPasswordResetTokenException extends DomainException {

    public InvalidPasswordResetTokenException() {
        super("Invalid or expired password reset token");
    }
}
