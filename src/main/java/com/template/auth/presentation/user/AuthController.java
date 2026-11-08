package com.template.auth.presentation.user;

import com.template.auth.application.passwordreset.command.RequestPasswordResetCommand;
import com.template.auth.application.passwordreset.command.RequestPasswordResetHandler;
import com.template.auth.application.passwordreset.command.ResetPasswordCommand;
import com.template.auth.application.passwordreset.command.ResetPasswordHandler;
import com.template.auth.application.refreshtoken.command.LogoutCommand;
import com.template.auth.application.refreshtoken.command.LogoutHandler;
import com.template.auth.application.refreshtoken.command.RefreshAccessTokenCommand;
import com.template.auth.application.refreshtoken.command.RefreshAccessTokenHandler;
import com.template.auth.application.refreshtoken.command.RefreshAccessTokenHandler.RefreshedTokens;
import com.template.auth.application.refreshtoken.command.RevokeSessionCommand;
import com.template.auth.application.refreshtoken.command.RevokeSessionHandler;
import com.template.auth.application.refreshtoken.query.ListSessionsHandler;
import com.template.auth.application.refreshtoken.query.ListSessionsHandler.SessionDto;
import com.template.auth.application.refreshtoken.query.ListSessionsQuery;
import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.application.user.command.LoginCommand;
import com.template.auth.application.user.command.LoginHandler;
import com.template.auth.application.user.command.LoginHandler.LoginResult;
import com.template.auth.application.user.command.RegisterUserCommand;
import com.template.auth.application.user.command.RegisterUserHandler;
import com.template.auth.application.user.command.VerifyEmailCommand;
import com.template.auth.application.user.command.VerifyEmailHandler;
import com.template.auth.domain.shared.exception.InvalidRefreshTokenException;
import com.template.auth.presentation.shared.ApiPaths;
import com.template.auth.presentation.shared.ratelimit.RateLimit;
import com.template.auth.presentation.user.dto.AccessTokenResponse;
import com.template.auth.presentation.user.dto.LoginRequest;
import com.template.auth.presentation.user.dto.ProfileResponse;
import com.template.auth.presentation.user.dto.RegisterUserRequest;
import com.template.auth.presentation.user.dto.RequestPasswordResetRequest;
import com.template.auth.presentation.user.dto.ResetPasswordRequest;
import com.template.auth.presentation.user.dto.VerifyEmailRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH)
@Tag(name = "Auth")
public class AuthController {

    private final RegisterUserHandler registerUser;
    private final LoginHandler login;
    private final RefreshAccessTokenHandler refreshAccessToken;
    private final LogoutHandler logout;
    private final RequestPasswordResetHandler requestPasswordReset;
    private final ResetPasswordHandler resetPassword;
    private final VerifyEmailHandler verifyEmail;
    private final ListSessionsHandler listSessions;
    private final RevokeSessionHandler revokeSession;
    private final RefreshTokenCookies cookies;

    public AuthController(RegisterUserHandler registerUser, LoginHandler login,
            RefreshAccessTokenHandler refreshAccessToken, LogoutHandler logout,
            RequestPasswordResetHandler requestPasswordReset, ResetPasswordHandler resetPassword,
            VerifyEmailHandler verifyEmail, ListSessionsHandler listSessions, RevokeSessionHandler revokeSession,
            RefreshTokenCookies cookies) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshAccessToken = refreshAccessToken;
        this.logout = logout;
        this.requestPasswordReset = requestPasswordReset;
        this.resetPassword = resetPassword;
        this.verifyEmail = verifyEmail;
        this.listSessions = listSessions;
        this.revokeSession = revokeSession;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(limit = 5)
    @SecurityRequirements
    public RegisterUserHandler.Result register(@Valid @RequestBody RegisterUserRequest body) {
        return registerUser.execute(new RegisterUserCommand(body.name(), body.email(), body.password()));
    }

    @PostMapping("/login")
    @RateLimit(limit = 5)
    @SecurityRequirements
    public AccessTokenResponse login(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        LoginResult result = login.execute(new LoginCommand(body.email(), body.password()));
        cookies.set(response, result.refreshToken());
        return new AccessTokenResponse(result.accessToken());
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public AccessTokenResponse refresh(@CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        RefreshedTokens result = refreshAccessToken.execute(new RefreshAccessTokenCommand(requireCookie(refreshToken)));
        cookies.set(response, result.refreshToken());
        return new AccessTokenResponse(result.accessToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    public void logout(@CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        logout.execute(new LogoutCommand(requireCookie(refreshToken)));
        cookies.clear(response);
    }

    @PostMapping("/request-password-reset")
    @RateLimit(limit = 3)
    @SecurityRequirements
    public Map<String, Object> requestPasswordReset(@Valid @RequestBody RequestPasswordResetRequest body) {
        // Always 200/{} regardless of whether the email exists - matches the
        // handler's own silent no-op, so an unknown email can't be told apart
        // from a real one by status code or response shape.
        requestPasswordReset.execute(new RequestPasswordResetCommand(body.email()));
        return Map.of();
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        resetPassword.execute(new ResetPasswordCommand(body.token(), body.newPassword()));
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest body) {
        verifyEmail.execute(new VerifyEmailCommand(body.token()));
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ProfileResponse.from(user);
    }

    /** The caller's own active sessions - what the 5-concurrent-session cap is capping. */
    @GetMapping("/sessions")
    public List<SessionDto> sessions(@AuthenticationPrincipal AuthenticatedUser user) {
        return listSessions.execute(new ListSessionsQuery(user.id()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@PathVariable UUID sessionId, @AuthenticationPrincipal AuthenticatedUser user) {
        revokeSession.execute(new RevokeSessionCommand(sessionId, user.id()));
    }

    private static String requireCookie(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return refreshToken;
    }
}
