package com.template.auth.infrastructure.queue;

import com.template.auth.application.shared.QueueMonitorPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RedisQueueMonitor implements QueueMonitorPort {

    private final RedisStreamQueue queue;
    private final List<RedisStreamWorker> workers;

    public RedisQueueMonitor(RedisStreamQueue queue, List<RedisStreamWorker> workers) {
        this.queue = queue;
        this.workers = workers;
    }

    @Override
    public List<QueueStats> stats() {
        return workers.stream()
                .map(RedisStreamWorker::queueName)
                .map(name -> new QueueStats(name, queue.length(name), queue.pending(name),
                        queue.retryScheduled(name), queue.deadLettered(name)))
                .toList();
    }
}
