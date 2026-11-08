package com.template.auth.application.notification;

import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.notification.NotificationEntity;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    NotificationEntity create(NotificationEntity notification);

    Optional<NotificationEntity> findById(UUID id);

    PaginatedResult<NotificationEntity> findAllForUser(UUID userId, PaginationParams params);

    NotificationEntity save(NotificationEntity notification);
}
