package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class InvalidEmailException extends DomainException {

    public InvalidEmailException(String email) {
        super("Invalid email format: \"" + email + "\"");
    }
}
