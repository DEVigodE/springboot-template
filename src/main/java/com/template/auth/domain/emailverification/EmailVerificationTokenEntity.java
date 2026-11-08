package com.template.auth.domain.emailverification;

import java.time.Instant;
import java.util.UUID;

/** Single-use, time-boxed token; only its SHA-256 hash is persisted. */
public class EmailVerificationTokenEntity {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant usedAt;
    private final Instant createdAt;

    private EmailVerificationTokenEntity(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static EmailVerificationTokenEntity create(UUID id, UUID userId, String tokenHash, Instant expiresAt,
            Instant createdAt) {
        return new EmailVerificationTokenEntity(id, userId, tokenHash, expiresAt, null, createdAt);
    }

    public static EmailVerificationTokenEntity reconstitute(UUID id, UUID userId, String tokenHash,
            Instant expiresAt, Instant usedAt, Instant createdAt) {
        return new EmailVerificationTokenEntity(id, userId, tokenHash, expiresAt, usedAt, createdAt);
    }

    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void markAsUsed() {
        this.usedAt = Instant.now();
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

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
