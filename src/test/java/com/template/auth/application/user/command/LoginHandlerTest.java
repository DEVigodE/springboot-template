package com.template.auth.application.user.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.template.auth.application.refreshtoken.IssueRefreshTokenService;
import com.template.auth.application.shared.CachePort;
import com.template.auth.application.user.LoginLockout;
import com.template.auth.application.user.PasswordHasherPort;
import com.template.auth.application.user.TokenServicePort;
import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.AccountLockedException;
import com.template.auth.domain.shared.exception.InvalidCredentialsException;
import com.template.auth.domain.shared.exception.UserInactiveException;
import com.template.auth.domain.user.UserEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    private static final String EMAIL = "user@example.com";
    private static final String FAILURE_KEY = LoginLockout.failureKey(EMAIL);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private TokenServicePort tokenService;
    @Mock
    private CachePort cache;
    @Mock
    private IssueRefreshTokenService issueRefreshTokenService;

    private LoginHandler handler;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        handler = new LoginHandler(userRepository, passwordHasher, tokenService, cache, issueRefreshTokenService);
        user = UserEntity.create(UUID.randomUUID(), "User", EMAIL, "hash", Instant.now(), null);
    }

    @Test
    void returnsBothTokensAndClearsFailuresOnSuccess() {
        when(cache.get(FAILURE_KEY, Long.class)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("Senha123", "hash")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("access");
        when(issueRefreshTokenService.issue(user.getId())).thenReturn("refresh");

        LoginHandler.LoginResult result = handler.execute(new LoginCommand(EMAIL, "Senha123"));

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(cache).delete(FAILURE_KEY);
    }

    @Test
    void rejectsWhenAccountIsLocked() {
        when(cache.get(FAILURE_KEY, Long.class))
                .thenReturn(Optional.of((long) LoginLockout.MAX_FAILED_LOGIN_ATTEMPTS));

        assertThatThrownBy(() -> handler.execute(new LoginCommand(EMAIL, "whatever")))
                .isInstanceOf(AccountLockedException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void countsAFailureForUnknownEmailWithoutRevealingIt() {
        when(cache.get(FAILURE_KEY, Long.class)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(cache.increment(eq(FAILURE_KEY), any())).thenReturn(1L);

        assertThatThrownBy(() -> handler.execute(new LoginCommand(EMAIL, "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(cache).increment(FAILURE_KEY, LoginLockout.LOGIN_LOCKOUT_WINDOW);
        verify(cache, never()).set(anyString(), any(), any());
    }

    @Test
    void theFailureCrossingTheThresholdStartsTheLockoutWindow() {
        when(cache.get(FAILURE_KEY, Long.class)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);
        when(cache.increment(eq(FAILURE_KEY), any())).thenReturn((long) LoginLockout.MAX_FAILED_LOGIN_ATTEMPTS);
        when(cache.increment(eq(LoginLockout.escalationKey(EMAIL)), any())).thenReturn(1L);

        assertThatThrownBy(() -> handler.execute(new LoginCommand(EMAIL, "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(cache).set(FAILURE_KEY, (long) LoginLockout.MAX_FAILED_LOGIN_ATTEMPTS,
                LoginLockout.LOGIN_LOCKOUT_WINDOW);
    }

    @Test
    void repeatedLockoutsEscalateTheWindow() {
        when(cache.get(FAILURE_KEY, Long.class)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);
        when(cache.increment(eq(FAILURE_KEY), any())).thenReturn((long) LoginLockout.MAX_FAILED_LOGIN_ATTEMPTS);
        when(cache.increment(eq(LoginLockout.escalationKey(EMAIL)), any()))
                .thenReturn((long) LoginLockout.LOCKOUTS_BEFORE_ESCALATION);

        assertThatThrownBy(() -> handler.execute(new LoginCommand(EMAIL, "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(cache).set(FAILURE_KEY, (long) LoginLockout.MAX_FAILED_LOGIN_ATTEMPTS,
                LoginLockout.ESCALATED_LOCKOUT_WINDOW);
    }

    @Test
    void rejectsInactiveUsersBeforeCheckingThePassword() {
        user.deactivate();
        when(cache.get(FAILURE_KEY, Long.class)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> handler.execute(new LoginCommand(EMAIL, "Senha123")))
                .isInstanceOf(UserInactiveException.class);

        verify(passwordHasher, never()).matches(anyString(), anyString());
    }
}
