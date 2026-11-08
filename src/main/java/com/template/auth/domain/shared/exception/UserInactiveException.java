package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class UserInactiveException extends DomainException {

    public UserInactiveException() {
        super("User is inactive");
    }
}
