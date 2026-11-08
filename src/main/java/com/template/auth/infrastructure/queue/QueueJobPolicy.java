package com.template.auth.infrastructure.queue;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Timeout + circuit breaker for queue workers.
 * Deliberately excludes retry: the queue itself re-schedules failed
 * jobs with backoff (see RedisStreamWorker), so a retry layer here would
 * multiply attempts instead of adding one. One instance per worker - the
 * breaker holds state across calls and must be shared.
 */
public final class QueueJobPolicy implements AutoCloseable {

    /**
     * Upper bound on how long a single job can run - and therefore on how long
     * graceful shutdown may have to wait for the in-flight job. The container's
     * stop grace period must exceed it (see Dockerfile / README).
     */
    public static final Duration JOB_TIMEOUT = Duration.ofSeconds(10);
    private static final int CONSECUTIVE_FAILURES_TO_OPEN = 5;
    private static final Duration HALF_OPEN_AFTER = Duration.ofSeconds(10);

    @FunctionalInterface
    public interface Job {
        void run() throws Exception;
    }

    private final CircuitBreaker breaker;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public QueueJobPolicy(String name) {
        this.breaker = CircuitBreaker.of(name, CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(CONSECUTIVE_FAILURES_TO_OPEN)
                .minimumNumberOfCalls(CONSECUTIVE_FAILURES_TO_OPEN)
                .failureRateThreshold(100f)
                .waitDurationInOpenState(HALF_OPEN_AFTER)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build());
    }

    public void execute(Job job) throws Exception {
        Callable<Void> timed = () -> {
            Future<?> future = executor.submit(() -> {
                job.run();
                return null;
            });
            try {
                future.get(JOB_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Job exceeded " + JOB_TIMEOUT);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                throw new IllegalStateException(cause);
            }
            return null;
        };

        breaker.decorateCallable(timed).call();
    }

    public CircuitBreaker.State state() {
        return breaker.getState();
    }

    @Override
    public void close() {
        executor.close();
    }
}
