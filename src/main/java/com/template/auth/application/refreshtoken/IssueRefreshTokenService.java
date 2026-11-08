package com.template.auth.application.refreshtoken;

import com.template.auth.application.shared.TokenHasherPort;
import com.template.auth.domain.refreshtoken.RefreshTokenEntity;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IssueRefreshTokenService {

    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    // Evicting the oldest active session on the (MAX_CONCURRENT_SESSIONS + 1)th
    // login is a transparent cap, unlike rejecting the new login outright - a
    // legitimate sign-in never fails because of this.
    public static final int MAX_CONCURRENT_SESSIONS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenHasherPort tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;

    public IssueRefreshTokenService(TokenHasherPort tokenHasher, RefreshTokenRepository refreshTokenRepository) {
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Persists a new session and returns the raw token (the only time it exists in clear). */
    public String issue(UUID userId) {
        evictOldestIfOverLimit(userId);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);
        Instant now = Instant.now();

        refreshTokenRepository.create(RefreshTokenEntity.create(UUID.randomUUID(), userId,
                tokenHasher.hash(rawToken), now.plus(REFRESH_TOKEN_TTL), now));

        return rawToken;
    }

    private void evictOldestIfOverLimit(UUID userId) {
        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findActiveByUserId(userId);

        if (activeTokens.size() < MAX_CONCURRENT_SESSIONS) {
            return;
        }

        int excess = activeTokens.size() - MAX_CONCURRENT_SESSIONS + 1;
        activeTokens.stream().limit(excess).forEach(token -> refreshTokenRepository.revoke(token.getId()));
    }
}
