package com.template.auth.domain.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationEntity {

    private final UUID id;
    private final UUID userId;
    private final String type;
    private final String title;
    private final String message;
    private Instant readAt;
    private final Instant createdAt;

    private NotificationEntity(UUID id, UUID userId, String type, String title, String message, Instant readAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public static NotificationEntity create(UUID id, UUID userId, String type, String title, String message,
            Instant createdAt) {
        return new NotificationEntity(id, userId, type, title, message, null, createdAt);
    }

    public static NotificationEntity reconstitute(UUID id, UUID userId, String type, String title, String message,
            Instant readAt, Instant createdAt) {
        return new NotificationEntity(id, userId, type, title, message, readAt, createdAt);
    }

    public void markAsRead() {
        this.readAt = Instant.now();
    }

    public boolean belongsTo(UUID userId) {
        return this.userId.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
