package com.template.auth.application.emailverification;

import com.template.auth.domain.emailverification.EmailVerificationTokenEntity;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository {

    EmailVerificationTokenEntity create(EmailVerificationTokenEntity token);

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    void markAsUsed(UUID id);

    /** Deletes expired or already-used rows; returns the count removed. */
    long deleteExpiredOrUsed();
}
