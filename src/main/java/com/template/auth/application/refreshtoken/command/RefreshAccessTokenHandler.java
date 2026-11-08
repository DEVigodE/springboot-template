package com.template.auth.application.refreshtoken.command;

import com.template.auth.application.refreshtoken.IssueRefreshTokenService;
import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.application.shared.TokenHasherPort;
import com.template.auth.application.user.TokenServicePort;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.refreshtoken.RefreshTokenEntity;
import com.template.auth.domain.shared.exception.InvalidRefreshTokenException;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;

/** Rotation: the presented refresh token is revoked and a brand-new one issued. */
@Service
public class RefreshAccessTokenHandler {

    private final TokenHasherPort tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenServicePort tokenService;
    private final IssueRefreshTokenService issueRefreshTokenService;

    public RefreshAccessTokenHandler(TokenHasherPort tokenHasher, RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository, TokenServicePort tokenService,
            IssueRefreshTokenService issueRefreshTokenService) {
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.issueRefreshTokenService = issueRefreshTokenService;
    }

    public record RefreshedTokens(String accessToken, String refreshToken) {
    }

    public RefreshedTokens execute(RefreshAccessTokenCommand command) {
        String tokenHash = tokenHasher.hash(command.refreshToken());
        RefreshTokenEntity existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(RefreshTokenEntity::isValid)
                .orElseThrow(InvalidRefreshTokenException::new);

        UserEntity user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);
        user.verifyActive();

        refreshTokenRepository.revoke(existingToken.getId());

        String accessToken = tokenService.generateToken(user);
        String refreshToken = issueRefreshTokenService.issue(user.getId());

        return new RefreshedTokens(accessToken, refreshToken);
    }
}
