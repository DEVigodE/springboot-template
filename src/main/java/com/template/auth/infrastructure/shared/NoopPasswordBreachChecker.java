package com.template.auth.infrastructure.shared;

import com.template.auth.application.shared.PasswordBreachCheckerPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default adapter: never flags anything, no network dependency. Set
 * PASSWORD_BREACH_CHECKER=hibp to swap in HibpPasswordBreachChecker.
 */
@Component
@ConditionalOnProperty(name = "app.password-breach-checker", havingValue = "noop", matchIfMissing = true)
public class NoopPasswordBreachChecker implements PasswordBreachCheckerPort {

    @Override
    public boolean isBreached(String password) {
        return false;
    }
}
