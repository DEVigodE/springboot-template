package com.template.auth.infrastructure.queue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;

/**
 * Base class for a consumer-group worker on one queue. Every message is
 * acknowledged exactly once: on success, or after it was either parked for a
 * retry (exponential backoff, up to MAX_ATTEMPTS) or dead-lettered. Messages
 * left pending by a crashed instance are reclaimed by QueueRetryScheduler.
 */
public abstract class RedisStreamWorker
        implements SmartLifecycle, StreamListener<String, MapRecord<String, String, String>> {

    static final int MAX_ATTEMPTS = 3;
    static final Duration BASE_BACKOFF = Duration.ofSeconds(2);
    static final Duration STALE_PENDING_AFTER = Duration.ofSeconds(60);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redis;
    private final RedisStreamQueue queue;
    private final String queueName;
    private final QueueJobPolicy policy;
    private final String consumerName;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private volatile boolean running;
    private volatile boolean groupReady;

    protected RedisStreamWorker(RedisConnectionFactory connectionFactory, StringRedisTemplate redis,
            RedisStreamQueue queue, String queueName) {
        this.connectionFactory = connectionFactory;
        this.redis = redis;
        this.queue = queue;
        this.queueName = queueName;
        this.policy = new QueueJobPolicy(queueName);
        this.consumerName = hostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** The actual job. Throw to fail the attempt. */
    protected abstract void process(Map<String, String> payload) throws Exception;

    public String queueName() {
        return queueName;
    }

    public QueueJobPolicy policy() {
        return policy;
    }

    @Override
    public void start() {
        // A Redis outage at boot must not keep the API down: the group is
        // created as soon as Redis is reachable (see ensureGroupIfNeeded,
        // re-driven by QueueRetryScheduler).
        ensureGroupIfNeeded();

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .batchSize(10)
                        .errorHandler(t -> {
                            // During shutdown the connection is closed under the poller; not worth a warning.
                            if (running) {
                                log.warn("[{}] stream poll error: {}", queueName, t.getMessage());
                            }
                        })
                        .build();

        container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.register(
                StreamReadRequest.builder(StreamOffset.create(streamKey(), ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(RedisStreamQueue.GROUP, consumerName))
                        .autoAcknowledge(false)
                        // A transient Redis error must not silently kill the worker.
                        .cancelOnError(t -> false)
                        .build(),
                this);
        container.start();
        running = true;
        log.info("[{}] worker {} started", queueName, consumerName);
    }

    @Override
    public void stop() {
        running = false;
        if (container != null) {
            container.stop();
        }
        policy.close();
        log.info("[{}] worker {} stopped", queueName, consumerName);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        handle(record);
    }

    /** Re-processes messages another (dead) consumer left pending for too long. */
    public void reclaimStalePending() {
        try {
            PendingMessages pending = queue.streamOps().pending(streamKey(), RedisStreamQueue.GROUP,
                    Range.unbounded(), 100);
            for (PendingMessage message : pending) {
                if (message.getElapsedTimeSinceLastDelivery().compareTo(STALE_PENDING_AFTER) < 0) {
                    continue;
                }
                List<MapRecord<String, String, String>> claimed = queue.streamOps().claim(streamKey(),
                        RedisStreamQueue.GROUP, consumerName, STALE_PENDING_AFTER, message.getId());
                claimed.forEach(this::handle);
            }
        } catch (DataAccessException e) {
            log.debug("[{}] could not inspect pending messages: {}", queueName, e.getMessage());
        }
    }

    private void handle(MapRecord<String, String, String> record) {
        Map<String, String> payload = new HashMap<>(record.getValue());
        int attempt = parseAttempt(payload.remove(RedisStreamQueue.ATTEMPT_FIELD));

        try {
            policy.execute(() -> process(payload));
        } catch (Exception e) {
            onFailure(payload, attempt, e);
        } finally {
            queue.streamOps().acknowledge(streamKey(), RedisStreamQueue.GROUP, record.getId());
        }
    }

    private void onFailure(Map<String, String> payload, int attempt, Exception error) {
        if (attempt < MAX_ATTEMPTS) {
            Duration delay = BASE_BACKOFF.multipliedBy(1L << (attempt - 1));
            log.warn("[{}] job failed (attempt {}/{}), retrying in {}: {}", queueName, attempt, MAX_ATTEMPTS, delay,
                    error.toString());
            queue.scheduleRetry(queueName, payload, attempt + 1, delay);
        } else {
            // Retries exhausted: this is the one that deserves a real alert.
            log.error("[{}] job failed permanently after {} attempts, dead-lettered", queueName, attempt, error);
            queue.deadLetter(queueName, payload, attempt, error);
        }
    }

    /** Idempotent: creates the consumer group (and the stream) once Redis answers; no-op afterwards. */
    public void ensureGroupIfNeeded() {
        if (groupReady) {
            return;
        }
        try {
            queue.streamOps().createGroup(streamKey(), ReadOffset.from("0"), RedisStreamQueue.GROUP);
            groupReady = true;
            log.info("[{}] consumer group {} ready", queueName, RedisStreamQueue.GROUP);
        } catch (RedisSystemException e) {
            // Lettuce reports "BUSYGROUP Consumer Group name already exists" on the
            // root cause; Spring wraps it as a generic "Error in execution".
            String rootMessage = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
            if (rootMessage.contains("BUSYGROUP")) {
                groupReady = true;
            } else {
                log.warn("[{}] could not create consumer group: {}", queueName, rootMessage);
            }
        } catch (DataAccessException e) {
            log.warn("[{}] Redis unavailable, consumer group not created yet: {}", queueName, e.getMessage());
        }
    }

    private String streamKey() {
        return RedisStreamQueue.streamKey(queueName);
    }

    private static int parseAttempt(String raw) {
        try {
            return raw == null ? 1 : Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "worker";
        }
    }

    StringRedisTemplate redis() {
        return redis;
    }
}
