package com.template.auth.application.auditlog;

import com.template.auth.domain.auditlog.AuditLogEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

@Service
public class AuditLogRecorder implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRecorder.class);

    private final AuditLogRepository auditLogRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AuditLogRecorder(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public record RecordAuditLogInput(UUID actorId, String action, String resourceType, String resourceId,
            Map<String, Object> metadata) {
    }

    public void record(RecordAuditLogInput input) {
        auditLogRepository.create(AuditLogEntity.create(UUID.randomUUID(), input.actorId(), input.action(),
                input.resourceType(), input.resourceId(), input.metadata(), Instant.now()));
    }

    /**
     * Fire-and-forget variant: the common case across write handlers, none of
     * which need to confirm the audit entry landed before responding to the
     * client - it's bookkeeping, not part of the operation's own correctness.
     * Errors are caught and logged here, never surfaced to the caller, so a
     * bookkeeping failure never fails the actual request.
     */
    public void recordAsync(RecordAuditLogInput input) {
        executor.execute(() -> {
            try {
                record(input);
            } catch (RuntimeException e) {
                log.error("Failed to record audit log for action \"{}\"", input.action(), e);
            }
        });
    }

    @Override
    public void destroy() {
        executor.close();
    }
}
