package com.template.auth.infrastructure.shared;

import com.template.auth.application.shared.PasswordBreachCheckerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter backed by HaveIBeenPwned's Pwned Passwords API, using k-anonymity -
 * only the first 5 hex chars of the password's SHA-1 ever leave this process,
 * never the password or its full hash. The endpoint is free and keyless.
 */
@Component
@ConditionalOnProperty(name = "app.password-breach-checker", havingValue = "hibp")
public class HibpPasswordBreachChecker implements PasswordBreachCheckerPort {

    private static final Logger log = LoggerFactory.getLogger(HibpPasswordBreachChecker.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    @Override
    public boolean isBreached(String password) {
        try {
            // SHA-1 is what the range API is keyed on; the full digest is never sent.
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            String hash = HexFormat.of().formatHex(sha1.digest(password.getBytes(StandardCharsets.UTF_8)))
                    .toUpperCase(Locale.ROOT);
            String prefix = hash.substring(0, 5);
            String suffix = hash.substring(5);

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Add-Padding", "true")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("HIBP returned " + response.statusCode());
            }

            return response.body().lines().anyMatch(line -> {
                int colon = line.indexOf(':');
                String lineSuffix = colon < 0 ? line : line.substring(0, colon);
                return lineSuffix.trim().equals(suffix) && !line.endsWith(":0");
            });
        } catch (Exception e) {
            // Fails open: a breach-list outage must not block registration/reset
            // outright - the complexity rule is already a real baseline, this is
            // defense in depth on top of it.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("HIBP check failed, allowing the password through: {}", e.getMessage());
            return false;
        }
    }
}
