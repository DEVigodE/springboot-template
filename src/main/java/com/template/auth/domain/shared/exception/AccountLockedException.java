package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class AccountLockedException extends DomainException {

    public AccountLockedException() {
        super("Account temporarily locked due to too many failed login attempts");
    }
}
