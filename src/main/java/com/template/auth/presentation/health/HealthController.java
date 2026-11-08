package com.template.auth.presentation.health;

import com.template.auth.application.health.CheckHealthHandler;
import com.template.auth.application.health.CheckHealthHandler.HealthCheckResult;
import com.template.auth.presentation.shared.ratelimit.SkipRateLimit;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Infrastructure-level probes (load balancers, uptime monitors) at a stable,
 * unversioned path. Exempt from rate limiting: a probe hitting this every few
 * seconds must never compete with real traffic for the budget - otherwise the
 * orchestrator could see a 429 and restart an otherwise-healthy pod.
 */
@RestController
@RequestMapping("/health")
@SkipRateLimit
@SecurityRequirements
@Tag(name = "Health")
public class HealthController {

    private final CheckHealthHandler checkHealth;

    public HealthController(CheckHealthHandler checkHealth) {
        this.checkHealth = checkHealth;
    }

    /** Readiness: are dependencies reachable? Failing this should pull the pod out of rotation, not restart it. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> check() {
        HealthCheckResult result = checkHealth.execute();

        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, Object> up = new LinkedHashMap<>();
        Map<String, Object> down = new LinkedHashMap<>();
        component("database", result.database(), details, up, down);
        component("redis", result.redis(), details, up, down);
        component("queue", result.queue(), details, up, down);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", result.healthy() ? "ok" : "error");
        body.put("info", up);
        body.put("error", down);
        body.put("details", details);

        return ResponseEntity.status(result.healthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /** Liveness: is the process responding? No dependency checks on purpose. */
    @GetMapping("/live")
    public Map<String, String> live() {
        return Map.of("status", "ok");
    }

    private static void component(String name, boolean isUp, Map<String, Object> details, Map<String, Object> up,
            Map<String, Object> down) {
        Map<String, String> status = Map.of("status", isUp ? "up" : "down");
        details.put(name, status);
        (isUp ? up : down).put(name, status);
    }
}
