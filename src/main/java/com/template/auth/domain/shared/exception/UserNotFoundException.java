package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(Object userId) {
        super("User \"" + userId + "\" not found");
    }
}
