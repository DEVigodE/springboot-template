package com.template.auth.application.user;

import java.time.Duration;
import java.util.UUID;

public final class UserCacheKeys {

    public static final Duration USER_CACHE_TTL = Duration.ofMinutes(5);

    private UserCacheKeys() {
    }

    public static String user(UUID userId) {
        return "user:" + userId;
    }
}
