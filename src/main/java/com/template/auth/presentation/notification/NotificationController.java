package com.template.auth.presentation.notification;

import com.template.auth.application.notification.NotificationDto;
import com.template.auth.application.notification.command.MarkNotificationAsReadCommand;
import com.template.auth.application.notification.command.MarkNotificationAsReadHandler;
import com.template.auth.application.notification.query.ListMyNotificationsHandler;
import com.template.auth.application.notification.query.ListMyNotificationsQuery;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.application.shared.security.AuthenticatedUser;
import com.template.auth.presentation.shared.ApiPaths;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST stays the source of truth for notifications; the STOMP push on
 * {@code /user/queue/notifications} (see WebSocketConfig) is a faster-delivery
 * layer on top, never a replacement.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final ListMyNotificationsHandler listMyNotifications;
    private final MarkNotificationAsReadHandler markAsRead;

    public NotificationController(ListMyNotificationsHandler listMyNotifications,
            MarkNotificationAsReadHandler markAsRead) {
        this.listMyNotifications = listMyNotifications;
        this.markAsRead = markAsRead;
    }

    @GetMapping("/me")
    public PaginatedResult<NotificationDto> listMine(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return listMyNotifications.execute(new ListMyNotificationsQuery(user.id(), PaginationParams.of(page, limit)));
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID notificationId, @AuthenticationPrincipal AuthenticatedUser user) {
        markAsRead.execute(new MarkNotificationAsReadCommand(notificationId, user.id()));
    }
}
