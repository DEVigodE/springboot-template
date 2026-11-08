package com.template.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end auth flow against a real Postgres and Redis (Testcontainers; the
 * class disables itself when Docker isn't available): register -> login ->
 * refresh rotation -> logout, plus the per-account lockout.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "Senha123";

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:18-alpine")
            .withEnv("POSTGRES_USER", "test")
            .withEnv("POSTGRES_PASSWORD", "test")
            .withEnv("POSTGRES_DB", "test")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.0.6-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/test");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        String redisUrl = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);
        registry.add("app.redis.queue-url", () -> redisUrl);
        registry.add("app.redis.cache-url", () -> redisUrl);
        registry.add("app.env", () -> "test");
        registry.add("app.jwt.secret", () -> "integration-test-secret-with-at-least-32-characters");
        // The lockout test needs more than 5 failed logins per minute from one IP.
        registry.add("app.rate-limit.enabled", () -> "false");
    }

    @Autowired
    private Environment environment;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + environment.getProperty("local.server.port"))
                .build();
    }

    @Test
    @Order(1)
    void registerLoginRefreshLogout() {
        String email = uniqueEmail();

        ResponseEntity<String> registered = post("/v1/auth/register",
                Map.of("name", "Test User", "email", email, "password", PASSWORD), h -> { });
        assertThat(registered.getStatusCode().value()).isEqualTo(201);
        assertThat(registered.getBody()).contains("\"id\"");

        ResponseEntity<String> duplicate = post("/v1/auth/register",
                Map.of("name", "Test User", "email", email, "password", PASSWORD), h -> { });
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(duplicate.getBody()).contains("EMAIL_ALREADY_REGISTERED");

        ResponseEntity<String> wrongPassword = post("/v1/auth/login",
                Map.of("email", email, "password", "WrongPassword1"), h -> { });
        assertThat(wrongPassword.getStatusCode().value()).isEqualTo(401);
        assertThat(wrongPassword.getBody()).contains("INVALID_CREDENTIALS");

        ResponseEntity<String> login = post("/v1/auth/login", Map.of("email", email, "password", PASSWORD),
                h -> { });
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat(login.getBody()).contains("accessToken");
        assertThat(login.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        String refreshCookie = refreshCookie(login.getHeaders());
        String accessToken = login.getBody().replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        ResponseEntity<String> me = get("/v1/users/me", h -> h.setBearerAuth(accessToken));
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(me.getBody()).contains(email).contains("\"emailVerified\":false");

        ResponseEntity<String> forbidden = get("/v1/users", h -> h.setBearerAuth(accessToken));
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);

        ResponseEntity<String> anonymous = get("/v1/users/me", h -> { });
        assertThat(anonymous.getStatusCode().value()).isEqualTo(401);

        ResponseEntity<String> refreshed = post("/v1/auth/refresh", null, h -> h.add(HttpHeaders.COOKIE, refreshCookie));
        assertThat(refreshed.getStatusCode().value()).isEqualTo(200);
        String rotatedCookie = refreshCookie(refreshed.getHeaders());
        assertThat(rotatedCookie).isNotEqualTo(refreshCookie);

        // The old cookie was revoked by rotation - replaying it must fail.
        ResponseEntity<String> replayed = post("/v1/auth/refresh", null, h -> h.add(HttpHeaders.COOKIE, refreshCookie));
        assertThat(replayed.getStatusCode().value()).isEqualTo(401);
        assertThat(replayed.getBody()).contains("INVALID_REFRESH_TOKEN");

        ResponseEntity<String> logout = post("/v1/auth/logout", null, h -> h.add(HttpHeaders.COOKIE, rotatedCookie));
        assertThat(logout.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<String> afterLogout = post("/v1/auth/refresh", null, h -> h.add(HttpHeaders.COOKIE, rotatedCookie));
        assertThat(afterLogout.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @Order(2)
    void locksTheAccountAfterRepeatedFailures() {
        String email = uniqueEmail();
        post("/v1/auth/register", Map.of("name", "Locked User", "email", email, "password", PASSWORD), h -> { });

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> failed = post("/v1/auth/login", Map.of("email", email, "password", "Wrong1234"),
                    h -> { });
            assertThat(failed.getStatusCode().value()).isEqualTo(401);
        }

        ResponseEntity<String> locked = post("/v1/auth/login", Map.of("email", email, "password", PASSWORD), h -> { });
        assertThat(locked.getStatusCode().value()).isEqualTo(423);
        assertThat(locked.getBody()).contains("ACCOUNT_LOCKED");
    }

    @Test
    @Order(3)
    void validationErrorsAreMachineReadable() {
        ResponseEntity<String> response = post("/v1/auth/register",
                Map.of("name", "X", "email", "not-an-email", "password", "weak"), h -> { });

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("VALIDATION_ERROR").contains("\"errors\"");
    }

    @Test
    @Order(4)
    void healthEndpointsAreUnauthenticated() {
        assertThat(get("/health/live", h -> { }).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<String> ready = get("/health", h -> { });
        assertThat(ready.getStatusCode().value()).isEqualTo(200);
        assertThat(ready.getBody()).contains("\"status\":\"ok\"");
    }

    private ResponseEntity<String> post(String path, Object body, Consumer<HttpHeaders> headers) {
        RequestHeadersSpec<?> spec = body == null
                ? client.post().uri(path).headers(headers)
                : client.post().uri(path).contentType(MediaType.APPLICATION_JSON).headers(headers).body(body);
        return exchange(spec);
    }

    private ResponseEntity<String> get(String path, Consumer<HttpHeaders> headers) {
        return exchange(client.get().uri(path).headers(headers));
    }

    private static ResponseEntity<String> exchange(RequestHeadersSpec<?> spec) {
        return spec.exchange((request, response) -> {
            HttpStatusCode status = response.getStatusCode();
            String body = new String(response.getBody().readAllBytes());
            return ResponseEntity.status(status).headers(response.getHeaders()).body(body);
        });
    }

    private static String refreshCookie(HttpHeaders headers) {
        List<String> cookies = headers.getOrEmpty(HttpHeaders.SET_COOKIE);
        return cookies.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .map(c -> c.split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("No refreshToken cookie in response: " + cookies));
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}
