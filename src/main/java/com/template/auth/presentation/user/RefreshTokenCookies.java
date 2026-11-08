package com.template.auth.presentation.user;

import com.template.auth.config.AppProperties;
import com.template.auth.presentation.shared.ApiPaths;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh token travels only in an httpOnly, SameSite=Strict cookie scoped
 * to /v1/auth - JS never sees it and the browser only sends it to the refresh
 * and logout routes. Bump the path along with the API version.
 */
@Component
public class RefreshTokenCookies {

    public static final String NAME = "refreshToken";

    private final AppProperties properties;

    public RefreshTokenCookies(AppProperties properties) {
        this.properties = properties;
    }

    public void set(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(NAME, refreshToken)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite("Strict")
                .path(ApiPaths.AUTH)
                .maxAge(properties.jwt().refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        // Must match the attributes set() uses - otherwise the browser never
        // matches the cookie it's being told to clear.
        ResponseCookie cookie = ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite("Strict")
                .path(ApiPaths.AUTH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
