package com.template.auth.domain.user;

import com.template.auth.domain.shared.exception.InvalidEmailException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Value object: a trimmed, lower-cased, format-checked e-mail address. */
public final class Email {

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (raw == null) {
            throw new InvalidEmailException("null");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_REGEX.matcher(normalized).matches()) {
            throw new InvalidEmailException(raw);
        }
        return new Email(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
