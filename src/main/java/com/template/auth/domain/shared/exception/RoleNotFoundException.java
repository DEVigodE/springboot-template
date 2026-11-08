package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class RoleNotFoundException extends DomainException {

    public RoleNotFoundException(Object roleId) {
        super("Role \"" + roleId + "\" not found");
    }
}
