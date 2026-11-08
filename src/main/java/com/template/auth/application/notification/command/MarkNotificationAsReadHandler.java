package com.template.auth.application.notification.command;

import com.template.auth.application.notification.NotificationRepository;
import com.template.auth.domain.notification.NotificationEntity;
import com.template.auth.domain.shared.exception.NotificationNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MarkNotificationAsReadHandler {

    private final NotificationRepository notificationRepository;

    public MarkNotificationAsReadHandler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void execute(MarkNotificationAsReadCommand command) {
        // Same exception for "doesn't exist" and "exists but isn't yours" -
        // deliberately not leaking existence of another user's notification.
        NotificationEntity notification = notificationRepository.findById(command.notificationId())
                .filter(n -> n.belongsTo(command.userId()))
                .orElseThrow(() -> new NotificationNotFoundException(command.notificationId()));

        notification.markAsRead();
        notificationRepository.save(notification);
    }
}
