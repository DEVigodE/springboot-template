package com.template.auth.presentation.shared.ratelimit;

import com.template.auth.application.shared.RateLimiterPort;
import com.template.auth.config.AppProperties;
import com.template.auth.presentation.shared.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Per-client-IP rate limiting. The IP is
 * request.getRemoteAddr(): raw socket address unless
 * server.forward-headers-strategy=framework makes Spring honor X-Forwarded-For
 * behind a trusted proxy. A route-level @RateLimit replaces the global budget
 * for that route; @SkipRateLimit exempts it.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    static final int DEFAULT_LIMIT = 100;
    static final Duration DEFAULT_WINDOW = Duration.ofSeconds(60);

    private final RateLimiterPort rateLimiter;
    private final AppProperties properties;
    private final JsonMapper json;

    public RateLimitInterceptor(RateLimiterPort rateLimiter, AppProperties properties, JsonMapper json) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!properties.rateLimit().enabled() || !(handler instanceof HandlerMethod method)) {
            return true;
        }
        if (method.hasMethodAnnotation(SkipRateLimit.class)
                || method.getBeanType().isAnnotationPresent(SkipRateLimit.class)) {
            return true;
        }

        RateLimit override = method.getMethodAnnotation(RateLimit.class);
        if (override == null) {
            override = method.getBeanType().getAnnotation(RateLimit.class);
        }

        int limit = override != null ? override.limit() : DEFAULT_LIMIT;
        Duration window = override != null ? Duration.ofSeconds(override.windowSeconds()) : DEFAULT_WINDOW;
        String bucket = override != null
                ? method.getBeanType().getSimpleName() + "." + method.getMethod().getName()
                : "global";
        String key = "ratelimit:" + bucket + ":" + request.getRemoteAddr();

        if (rateLimiter.tryAcquire(key, limit, window)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json.writeValueAsString(ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS", "Too many requests")));
        return false;
    }
}
