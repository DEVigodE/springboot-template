package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(String reason) {
        super(reason);
    }
}
