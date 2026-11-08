package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email \"" + email + "\" is already registered");
    }
}
