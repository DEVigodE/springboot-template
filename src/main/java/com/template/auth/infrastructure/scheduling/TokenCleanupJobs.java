package com.template.auth.infrastructure.scheduling;

import com.template.auth.application.emailverification.EmailVerificationTokenRepository;
import com.template.auth.application.passwordreset.PasswordResetTokenRepository;
import com.template.auth.application.refreshtoken.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly pruning of expired/used/revoked tokens. The three jobs are staggered
 * (02:00, 02:10, 02:20) so they don't hit Postgres in the same instant. Every
 * replica runs them; the deletes are idempotent, so that's harmless - add
 * ShedLock if you'd rather have exactly one runner.
 */
@Component
public class TokenCleanupJobs {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJobs.class);

    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final EmailVerificationTokenRepository emailVerificationTokens;

    public TokenCleanupJobs(RefreshTokenRepository refreshTokens, PasswordResetTokenRepository passwordResetTokens,
            EmailVerificationTokenRepository emailVerificationTokens) {
        this.refreshTokens = refreshTokens;
        this.passwordResetTokens = passwordResetTokens;
        this.emailVerificationTokens = emailVerificationTokens;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupRefreshTokens() {
        long count = refreshTokens.deleteExpiredOrRevoked();
        log.info("Deleted {} expired/revoked refresh tokens", count);
    }

    @Scheduled(cron = "0 10 2 * * *")
    public void cleanupPasswordResetTokens() {
        long count = passwordResetTokens.deleteExpiredOrUsed();
        log.info("Deleted {} expired/used password reset tokens", count);
    }

    @Scheduled(cron = "0 20 2 * * *")
    public void cleanupEmailVerificationTokens() {
        long count = emailVerificationTokens.deleteExpiredOrUsed();
        log.info("Deleted {} expired/used email verification tokens", count);
    }
}
