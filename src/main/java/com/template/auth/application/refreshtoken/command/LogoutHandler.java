package com.template.auth.application.refreshtoken.command;

import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.application.shared.TokenHasherPort;
import org.springframework.stereotype.Service;

@Service
public class LogoutHandler {

    private final TokenHasherPort tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutHandler(TokenHasherPort tokenHasher, RefreshTokenRepository refreshTokenRepository) {
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void execute(LogoutCommand command) {
        String tokenHash = tokenHasher.hash(command.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> refreshTokenRepository.revoke(token.getId()));
    }
}
