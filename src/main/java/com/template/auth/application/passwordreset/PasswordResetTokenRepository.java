package com.template.auth.application.passwordreset;

import com.template.auth.domain.passwordreset.PasswordResetTokenEntity;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    PasswordResetTokenEntity create(PasswordResetTokenEntity token);

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    void markAsUsed(UUID id);

    /** Deletes expired or already-used rows; returns the count removed. */
    long deleteExpiredOrUsed();
}
