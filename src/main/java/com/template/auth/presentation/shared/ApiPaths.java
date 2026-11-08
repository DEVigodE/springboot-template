package com.template.auth.presentation.shared;

/**
 * URI versioning from day one. Everything but /health lives under /v1; bump
 * REFRESH_COOKIE_PATH along with it (the browser only re-attaches the refresh
 * token cookie to paths under it).
 */
public final class ApiPaths {

    public static final String V1 = "/v1";
    public static final String AUTH = V1 + "/auth";

    private ApiPaths() {
    }
}
