package com.template.auth.application.user.command;

import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.shared.PasswordBreachCheckerPort;
import com.template.auth.application.user.PasswordHasherPort;
import com.template.auth.application.user.UserCacheKeys;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.InvalidCredentialsException;
import com.template.auth.domain.shared.exception.InvalidPasswordException;
import com.template.auth.domain.shared.exception.UserNotFoundException;
import com.template.auth.domain.user.Password;
import com.template.auth.domain.user.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordHandler {

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CachePort cache;
    private final PasswordBreachCheckerPort passwordBreachChecker;

    public ChangePasswordHandler(UserRepository userRepository, PasswordHasherPort passwordHasher,
            RefreshTokenRepository refreshTokenRepository, CachePort cache,
            PasswordBreachCheckerPort passwordBreachChecker) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cache = cache;
        this.passwordBreachChecker = passwordBreachChecker;
    }

    @Transactional
    public void execute(ChangePasswordCommand command) {
        UserEntity user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (!passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Password.validate(command.newPassword());
        if (passwordBreachChecker.isBreached(command.newPassword())) {
            throw new InvalidPasswordException(RegisterUserHandler.BREACHED_PASSWORD_MESSAGE);
        }
        user.changePasswordHash(passwordHasher.hash(command.newPassword()));
        user.registerUpdate();

        userRepository.save(user);
        // Every other session of this account is invalidated along with the
        // old password.
        refreshTokenRepository.revokeAllForUser(user.getId());
        cache.delete(UserCacheKeys.user(user.getId()));
    }
}
