package com.template.auth.domain.refreshtoken;

import java.time.Instant;
import java.util.UUID;

/** One login session. Only the SHA-256 hash of the raw token is ever stored. */
public class RefreshTokenEntity {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private final Instant createdAt;

    private RefreshTokenEntity(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant revokedAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public static RefreshTokenEntity create(UUID id, UUID userId, String tokenHash, Instant expiresAt,
            Instant createdAt) {
        return new RefreshTokenEntity(id, userId, tokenHash, expiresAt, null, createdAt);
    }

    public static RefreshTokenEntity reconstitute(UUID id, UUID userId, String tokenHash, Instant expiresAt,
            Instant revokedAt, Instant createdAt) {
        return new RefreshTokenEntity(id, userId, tokenHash, expiresAt, revokedAt, createdAt);
    }

    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
