package com.template.auth.application.passwordreset.command;

import com.template.auth.application.passwordreset.PasswordResetTokenRepository;
import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.shared.PasswordBreachCheckerPort;
import com.template.auth.application.shared.TokenHasherPort;
import com.template.auth.application.user.LoginLockout;
import com.template.auth.application.user.PasswordHasherPort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.passwordreset.PasswordResetTokenEntity;
import com.template.auth.domain.shared.exception.InvalidPasswordException;
import com.template.auth.domain.shared.exception.InvalidPasswordResetTokenException;
import com.template.auth.domain.shared.exception.UserInactiveException;
import com.template.auth.domain.user.Password;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class ResetPasswordHandler {

    private final TokenHasherPort tokenHasher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CachePort cache;
    private final PasswordBreachCheckerPort passwordBreachChecker;
    private final TransactionOperations transaction;

    public ResetPasswordHandler(TokenHasherPort tokenHasher,
            PasswordResetTokenRepository passwordResetTokenRepository, UserRepository userRepository,
            PasswordHasherPort passwordHasher, RefreshTokenRepository refreshTokenRepository, CachePort cache,
            PasswordBreachCheckerPort passwordBreachChecker, TransactionOperations transaction) {
        this.tokenHasher = tokenHasher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cache = cache;
        this.passwordBreachChecker = passwordBreachChecker;
        this.transaction = transaction;
    }

    public void execute(ResetPasswordCommand command) {
        String tokenHash = tokenHasher.hash(command.token());
        PasswordResetTokenEntity existingToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .filter(PasswordResetTokenEntity::isValid)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        UserEntity user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        // A deactivated account also fails as "invalid token" here, not
        // USER_INACTIVE/403 - this endpoint is unauthenticated, so it must not
        // reveal account state to whoever is holding the token.
        try {
            user.verifyActive();
        } catch (UserInactiveException e) {
            throw new InvalidPasswordResetTokenException();
        }

        Password.validate(command.newPassword());
        if (passwordBreachChecker.isBreached(command.newPassword())) {
            throw new InvalidPasswordException(
                    "This password has appeared in a known data breach - choose a different one");
        }
        user.changePasswordHash(passwordHasher.hash(command.newPassword()));
        user.registerUpdate();

        transaction.executeWithoutResult(status -> {
            userRepository.save(user);
            passwordResetTokenRepository.markAsUsed(existingToken.getId());
            refreshTokenRepository.revokeAllForUser(user.getId());
        });

        cache.delete(UserCacheKeys.user(user.getId()));
        // A successful reset is a legitimate, self-service recovery path - it
        // must not leave the account locked out from failed login attempts that
        // happened before the user regained access, and proving ownership this
        // way also resets the escalation count from LoginHandler.
        cache.delete(LoginLockout.failureKey(user.getEmail()));
        cache.delete(LoginLockout.escalationKey(user.getEmail()));
    }
}
