package com.template.auth.application.refreshtoken.command;

import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.domain.refreshtoken.RefreshTokenEntity;
import com.template.auth.domain.shared.exception.RefreshTokenNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RevokeSessionHandler {

    private final RefreshTokenRepository refreshTokenRepository;

    public RevokeSessionHandler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void execute(RevokeSessionCommand command) {
        // Scoped through the caller's own active sessions rather than a direct
        // findById - the only way to revoke someone else's session would be to
        // guess their session id, and this makes that guess useless instead of
        // needing a separate ownership check after the fact.
        RefreshTokenEntity session = refreshTokenRepository.findActiveByUserId(command.userId()).stream()
                .filter(s -> s.getId().equals(command.sessionId()))
                .findFirst()
                .orElseThrow(() -> new RefreshTokenNotFoundException(command.sessionId()));

        refreshTokenRepository.revoke(session.getId());
    }
}
