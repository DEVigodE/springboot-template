package com.template.auth.presentation.queue;

import com.template.auth.application.shared.QueueMonitorPort;
import com.template.auth.application.shared.QueueMonitorPort.QueueStats;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only queue dashboard (JSON): queue length, pending (in-flight), retries
 * scheduled and dead-lettered counts per queue. Only registered when
 * app.queue.admin-enabled=true (the `dev` profile sets it); 404 otherwise.
 */
@RestController
@RequestMapping("/admin/queues")
@ConditionalOnProperty(name = "app.queue.admin-enabled", havingValue = "true")
@SecurityRequirements
@Tag(name = "Queues (dev)")
public class QueueAdminController {

    private final QueueMonitorPort queueMonitor;

    public QueueAdminController(QueueMonitorPort queueMonitor) {
        this.queueMonitor = queueMonitor;
    }

    @GetMapping
    public List<QueueStats> stats() {
        return queueMonitor.stats();
    }
}
