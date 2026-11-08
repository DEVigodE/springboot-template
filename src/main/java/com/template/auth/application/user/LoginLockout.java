package com.template.auth.application.user;

import java.time.Duration;
import java.util.Locale;

/**
 * Per-account lockout policy. IP-based rate limiting (@RateLimit on the auth
 * controller) already limits raw request volume; this adds a second layer that
 * survives a distributed attack spread across many IPs targeting one email.
 */
public final class LoginLockout {

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final Duration LOGIN_LOCKOUT_WINDOW = Duration.ofMinutes(15);

    // A rolling 15-minute lock alone lets an attacker keep one account
    // perpetually inaccessible for a handful of cheap requests every 15
    // minutes, by relocking it the instant it expires. After enough relocks
    // within a day, escalate the lock to something that actually costs the
    // attacker time - a legitimate user who just forgot their password never
    // notices, since a password reset clears both counters.
    public static final int LOCKOUTS_BEFORE_ESCALATION = 3;
    public static final Duration LOCKOUT_ESCALATION_TRACKING_WINDOW = Duration.ofHours(24);
    public static final Duration ESCALATED_LOCKOUT_WINDOW = Duration.ofHours(24);

    private LoginLockout() {
    }

    public static String failureKey(String email) {
        return "login:failures:" + email.toLowerCase(Locale.ROOT);
    }

    public static String escalationKey(String email) {
        return "login:lockout-escalations:" + email.toLowerCase(Locale.ROOT);
    }
}
