package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class RoleAlreadyExistsException extends DomainException {

    public RoleAlreadyExistsException(String name) {
        super("Role \"" + name + "\" already exists");
    }
}
