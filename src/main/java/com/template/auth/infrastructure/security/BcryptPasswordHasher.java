package com.template.auth.infrastructure.security;

import com.template.auth.application.user.PasswordHasherPort;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasherPort {

    private static final int STRENGTH = 10;

    // bcrypt silently truncates input at 72 bytes - anything beyond that is
    // simply ignored, meaning two different passwords sharing the same 72-byte
    // prefix would hash identically. Guard here so this holds regardless of
    // which caller/DTO validated the input.
    private static final int MAX_PASSWORD_BYTES = 72;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String hash(String plainText) {
        if (plainText.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException(
                    "Password exceeds the maximum supported length of " + MAX_PASSWORD_BYTES + " bytes");
        }
        return encoder.encode(plainText);
    }

    @Override
    public boolean matches(String plainText, String hash) {
        if (plainText.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return false;
        }
        return encoder.matches(plainText, hash);
    }
}
