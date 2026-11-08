package com.template.auth.infrastructure.queue;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Durable job queue on Redis Streams. Each logical queue
 * is one stream consumed by a consumer group; failed jobs are parked in a
 * sorted set scored by their retry time and moved back into the stream by
 * QueueRetryScheduler; jobs that exhaust their attempts land in a dead-letter
 * stream for inspection.
 */
@Component
public class RedisStreamQueue {

    static final String ATTEMPT_FIELD = "__attempt";
    static final String ERROR_FIELD = "__error";
    static final String FAILED_AT_FIELD = "__failedAt";
    static final String GROUP = "workers";

    record RetryEnvelope(int attempt, Map<String, String> payload) {
    }

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisStreamQueue(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public static String streamKey(String queue) {
        return "queue:" + queue;
    }

    public static String retryKey(String queue) {
        return "queue:" + queue + ":retry";
    }

    public static String deadLetterKey(String queue) {
        return "queue:" + queue + ":dead";
    }

    public void enqueue(String queue, Map<String, String> payload) {
        add(queue, payload, 1);
    }

    RecordId add(String queue, Map<String, String> payload, int attempt) {
        Map<String, String> fields = new LinkedHashMap<>(payload);
        fields.put(ATTEMPT_FIELD, Integer.toString(attempt));
        return streamOps().add(StreamRecords.newRecord().in(streamKey(queue)).ofMap(fields));
    }

    void scheduleRetry(String queue, Map<String, String> payload, int nextAttempt, Duration delay) {
        String member = json.writeValueAsString(new RetryEnvelope(nextAttempt, payload));
        redis.opsForZSet().add(retryKey(queue), member, Instant.now().plus(delay).toEpochMilli());
    }

    void deadLetter(String queue, Map<String, String> payload, int attempt, Throwable error) {
        Map<String, String> fields = new LinkedHashMap<>(payload);
        fields.put(ATTEMPT_FIELD, Integer.toString(attempt));
        fields.put(ERROR_FIELD, String.valueOf(error));
        fields.put(FAILED_AT_FIELD, Instant.now().toString());
        streamOps().add(StreamRecords.newRecord().in(deadLetterKey(queue)).ofMap(fields));
    }

    /** Moves every retry whose time has come back into the stream; returns how many. */
    public int drainDueRetries(String queue) {
        Set<String> due = redis.opsForZSet().rangeByScore(retryKey(queue), 0, Instant.now().toEpochMilli(), 0, 100);
        if (due == null || due.isEmpty()) {
            return 0;
        }
        int moved = 0;
        for (String member : due) {
            // ZREM is the "claim": with several instances draining the same set,
            // only the one that removed the member re-adds it.
            Long removed = redis.opsForZSet().remove(retryKey(queue), member);
            if (removed != null && removed > 0) {
                RetryEnvelope envelope = json.readValue(member, RetryEnvelope.class);
                add(queue, envelope.payload(), envelope.attempt());
                moved++;
            }
        }
        return moved;
    }

    public long length(String queue) {
        Long size = streamOps().size(streamKey(queue));
        return size == null ? 0 : size;
    }

    public long pending(String queue) {
        try {
            PendingMessagesSummary summary = streamOps().pending(streamKey(queue), GROUP);
            return summary == null ? 0 : summary.getTotalPendingMessages();
        } catch (DataAccessException e) {
            // Stream or group doesn't exist yet.
            return 0;
        }
    }

    public long retryScheduled(String queue) {
        Long size = redis.opsForZSet().zCard(retryKey(queue));
        return size == null ? 0 : size;
    }

    public long deadLettered(String queue) {
        Long size = streamOps().size(deadLetterKey(queue));
        return size == null ? 0 : size;
    }

    StreamOperations<String, String, String> streamOps() {
        return redis.opsForStream();
    }
}
