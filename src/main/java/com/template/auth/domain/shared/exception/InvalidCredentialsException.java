package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
