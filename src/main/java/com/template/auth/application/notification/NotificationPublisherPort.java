package com.template.auth.application.notification;

/**
 * Push-only complement to the notification repository - a client that never
 * connects (or disconnects mid-session) still sees every notification through
 * GET /notifications/me, which stays the source of truth. This is best-effort
 * delivery to whoever happens to be connected right now.
 */
public interface NotificationPublisherPort {

    void publish(NotificationDto notification);
}
