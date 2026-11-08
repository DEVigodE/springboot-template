package com.template.auth.application.shared;

import java.util.List;

/** Read-only queue statistics for the dev-only admin view. */
public interface QueueMonitorPort {

    record QueueStats(String name, long length, long pending, long retryScheduled, long deadLettered) {
    }

    List<QueueStats> stats();
}
