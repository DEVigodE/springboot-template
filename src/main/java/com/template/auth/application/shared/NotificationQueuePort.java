package com.template.auth.application.shared;

import java.util.UUID;

/** Durable, asynchronous notification creation (the worker runs CreateNotificationHandler). */
public interface NotificationQueuePort {

    record NotificationJob(UUID userId, String type, String title, String message) {
    }

    void enqueue(NotificationJob job);
}
