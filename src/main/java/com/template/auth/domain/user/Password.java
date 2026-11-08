package com.template.auth.domain.user;

import com.template.auth.domain.shared.exception.InvalidPasswordException;
import java.util.regex.Pattern;

/**
 * Password policy as a domain invariant. Mirrors the Bean Validation
 * {@code @ValidPassword} constraint on the request DTOs - same rules, enforced
 * independently of whatever validated the request on its way in, so an entity
 * can never end up holding an invalid password regardless of which caller
 * reaches it.
 */
public final class Password {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_NUMBER = Pattern.compile("\\d");

    private Password() {
    }

    public static void validate(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < MIN_LENGTH) {
            throw new InvalidPasswordException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (plainPassword.length() > MAX_LENGTH) {
            throw new InvalidPasswordException("Password must be at most " + MAX_LENGTH + " characters long");
        }
        if (!HAS_UPPERCASE.matcher(plainPassword).find()) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }
        if (!HAS_LOWERCASE.matcher(plainPassword).find()) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }
        if (!HAS_NUMBER.matcher(plainPassword).find()) {
            throw new InvalidPasswordException("Password must contain at least one number");
        }
    }
}
