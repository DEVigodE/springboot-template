package com.template.auth.domain.shared.exception;

import com.template.auth.domain.shared.DomainException;

public class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException(Object notificationId) {
        super("Notification \"" + notificationId + "\" not found");
    }
}
