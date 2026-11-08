package com.template.auth.infrastructure.queue;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Housekeeping for every registered worker: due retries and stale pending messages. */
@Component
public class QueueRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueRetryScheduler.class);

    private final RedisStreamQueue queue;
    private final List<RedisStreamWorker> workers;

    public QueueRetryScheduler(RedisStreamQueue queue, List<RedisStreamWorker> workers) {
        this.queue = queue;
        this.workers = workers;
    }

    @Scheduled(fixedDelay = 1000)
    public void moveDueRetries() {
        for (RedisStreamWorker worker : workers) {
            try {
                worker.ensureGroupIfNeeded();
                int moved = queue.drainDueRetries(worker.queueName());
                if (moved > 0) {
                    log.debug("[{}] re-queued {} job(s) for retry", worker.queueName(), moved);
                }
            } catch (DataAccessException e) {
                log.warn("[{}] could not drain retries: {}", worker.queueName(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void reclaimStalePending() {
        workers.forEach(RedisStreamWorker::reclaimStalePending);
    }
}
