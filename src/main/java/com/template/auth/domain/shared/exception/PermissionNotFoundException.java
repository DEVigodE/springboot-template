package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class PermissionNotFoundException extends DomainException {

    public PermissionNotFoundException(Object permissionId) {
        super("Permission \"" + permissionId + "\" not found");
    }
}
