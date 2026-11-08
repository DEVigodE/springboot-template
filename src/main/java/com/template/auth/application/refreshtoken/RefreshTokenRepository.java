package com.template.auth.application.refreshtoken;

import com.template.auth.domain.refreshtoken.RefreshTokenEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshTokenEntity create(RefreshTokenEntity token);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    void revoke(UUID id);

    void revokeAllForUser(UUID userId);

    /** Deletes expired or already-revoked rows; returns the count removed. */
    long deleteExpiredOrRevoked();

    /** Non-revoked, non-expired tokens for a user, oldest first. */
    List<RefreshTokenEntity> findActiveByUserId(UUID userId);
}
