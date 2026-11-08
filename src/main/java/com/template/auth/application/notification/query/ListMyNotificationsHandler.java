package com.template.auth.application.notification.query;

import com.template.auth.application.notification.NotificationDto;
import com.template.auth.application.notification.NotificationRepository;
import com.template.auth.application.shared.PaginatedResult;
import org.springframework.stereotype.Service;

@Service
public class ListMyNotificationsHandler {

    private final NotificationRepository notificationRepository;

    public ListMyNotificationsHandler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public PaginatedResult<NotificationDto> execute(ListMyNotificationsQuery query) {
        return notificationRepository.findAllForUser(query.userId(), query.pagination()).map(NotificationDto::from);
    }
}
