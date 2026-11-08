package com.template.auth.application.notification.query;

import com.template.auth.application.shared.PaginationParams;
import java.util.UUID;

public record ListMyNotificationsQuery(UUID userId, PaginationParams pagination) {
}
