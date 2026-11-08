package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidEmailVerificationTokenException extends DomainException {

    public InvalidEmailVerificationTokenException() {
        super("Invalid or expired email verification token");
    }
}
