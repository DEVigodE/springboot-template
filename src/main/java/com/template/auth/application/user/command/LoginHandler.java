package com.template.auth.application.user.command;

import static com.template.auth.application.user.LoginLockout.ESCALATED_LOCKOUT_WINDOW;
import static com.template.auth.application.user.LoginLockout.LOCKOUTS_BEFORE_ESCALATION;
import static com.template.auth.application.user.LoginLockout.LOCKOUT_ESCALATION_TRACKING_WINDOW;
import static com.template.auth.application.user.LoginLockout.LOGIN_LOCKOUT_WINDOW;
import static com.template.auth.application.user.LoginLockout.MAX_FAILED_LOGIN_ATTEMPTS;

import com.template.auth.application.refreshtoken.IssueRefreshTokenService;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.user.LoginLockout;
import com.template.auth.application.user.PasswordHasherPort;
import com.template.auth.application.user.TokenServicePort;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.AccountLockedException;
import com.template.auth.domain.shared.exception.InvalidCredentialsException;
import com.template.auth.domain.user.UserEntity;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class LoginHandler {

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;
    private final CachePort cache;
    private final IssueRefreshTokenService issueRefreshTokenService;

    public LoginHandler(UserRepository userRepository, PasswordHasherPort passwordHasher,
            TokenServicePort tokenService, CachePort cache, IssueRefreshTokenService issueRefreshTokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.cache = cache;
        this.issueRefreshTokenService = issueRefreshTokenService;
    }

    public record LoginResult(String accessToken, String refreshToken) {
    }

    public LoginResult execute(LoginCommand command) {
        String failureKey = LoginLockout.failureKey(command.email());
        long failures = cache.get(failureKey, Long.class).orElse(0L);

        if (failures >= MAX_FAILED_LOGIN_ATTEMPTS) {
            throw new AccountLockedException();
        }

        UserEntity user = userRepository.findByEmail(command.email()).orElse(null);

        if (user == null) {
            recordFailure(failureKey, command.email());
            throw new InvalidCredentialsException();
        }

        user.verifyActive();

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            recordFailure(failureKey, command.email());
            throw new InvalidCredentialsException();
        }

        cache.delete(failureKey);

        String accessToken = tokenService.generateToken(user);
        String refreshToken = issueRefreshTokenService.issue(user.getId());

        return new LoginResult(accessToken, refreshToken);
    }

    private void recordFailure(String key, String email) {
        // Atomic INCR (with the window refreshed on every call) instead of a
        // get-then-set couplet - concurrent failed attempts on the same account
        // would otherwise race, each reading the same stale count and losing
        // all but one of the increments.
        long nextCount = cache.increment(key, LOGIN_LOCKOUT_WINDOW);

        if (nextCount != MAX_FAILED_LOGIN_ATTEMPTS) {
            // Either still under the limit, or a concurrent request that landed
            // after another one already crossed the threshold below - that
            // request already recorded the (possibly escalated) lockout window.
            return;
        }

        // This failure is the one crossing the threshold into a fresh lockout -
        // escalate its duration if the account has already been locked out
        // repeatedly (see LoginLockout for why).
        long lockoutCount = cache.increment(LoginLockout.escalationKey(email), LOCKOUT_ESCALATION_TRACKING_WINDOW);

        Duration lockoutWindow = lockoutCount >= LOCKOUTS_BEFORE_ESCALATION
                ? ESCALATED_LOCKOUT_WINDOW
                : LOGIN_LOCKOUT_WINDOW;

        cache.set(key, nextCount, lockoutWindow);
    }
}
