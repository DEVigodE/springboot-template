package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class RefreshTokenNotFoundException extends DomainException {

    public RefreshTokenNotFoundException(Object sessionId) {
        super("Session \"" + sessionId + "\" not found");
    }
}
