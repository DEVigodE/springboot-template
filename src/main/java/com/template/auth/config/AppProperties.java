package com.template.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed, validated view of every `app.*` setting. Validation runs at boot, so
 * a missing or placeholder secret fails loudly before the app ever serves a
 * request.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank @Pattern(regexp = "development|production|test") String env,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Cookie cookie,
        @Valid @NotNull Redis redis,
        @Valid @NotNull Email email,
        @Valid @NotNull Storage storage,
        @NotBlank @Pattern(regexp = "noop|hibp") String passwordBreachChecker,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Queue queue) {

    public boolean isProduction() {
        return "production".equals(env);
    }

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters long") String secret,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl) {

        // Kept in sync with .env.example's own placeholder - rejected here so a
        // forgotten swap fails at boot instead of shipping a public secret.
        static final String PLACEHOLDER = "troque-por-um-secret-forte-de-pelo-menos-32-caracteres";

        @AssertTrue(message = "JWT_SECRET is still the placeholder value from .env.example - generate a real secret before starting the app")
        public boolean isSecretNotPlaceholder() {
            return !PLACEHOLDER.equals(secret);
        }
    }

    public record Cors(@NotNull List<String> allowedOrigins) {
        public boolean allowsAny() {
            return allowedOrigins.size() == 1 && "*".equals(allowedOrigins.getFirst());
        }
    }

    public record Cookie(boolean secure) {
    }

    public record Redis(@NotBlank String queueUrl, @NotBlank String cacheUrl) {
    }

    public record Email(@NotBlank @Pattern(regexp = "log|smtp") String sender, @NotBlank String from) {
    }

    public record Storage(
            @NotBlank @Pattern(regexp = "local|s3") String type,
            @NotBlank String localDir,
            @Valid @NotNull S3 s3) {

        public record S3(@DefaultValue("") String bucket, @DefaultValue("") String region) {
        }
    }

    public record RateLimit(@DefaultValue("true") boolean enabled) {
    }

    public record Queue(@DefaultValue("false") boolean adminEnabled) {
    }
}
