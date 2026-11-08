package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class PermissionAlreadyExistsException extends DomainException {

    public PermissionAlreadyExistsException(String name) {
        super("Permission \"" + name + "\" already exists");
    }
}
