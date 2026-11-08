package com.template.auth.application.notification.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.template.auth.application.notification.NotificationRepository;
import com.template.auth.domain.notification.NotificationEntity;
import com.template.auth.domain.shared.exception.NotificationNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarkNotificationAsReadHandlerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private MarkNotificationAsReadHandler handler;

    @Test
    void marksTheOwnersNotificationAsRead() {
        UUID owner = UUID.randomUUID();
        NotificationEntity notification = NotificationEntity.create(UUID.randomUUID(), owner, "t", "title", "msg",
                Instant.now());
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        handler.execute(new MarkNotificationAsReadCommand(notification.getId(), owner));

        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void someoneElsesNotificationLooksLikeItDoesNotExist() {
        NotificationEntity notification = NotificationEntity.create(UUID.randomUUID(), UUID.randomUUID(), "t",
                "title", "msg", Instant.now());
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> handler.execute(new MarkNotificationAsReadCommand(notification.getId(),
                UUID.randomUUID())))
                .isInstanceOf(NotificationNotFoundException.class);

        verify(notificationRepository, never()).save(any());
    }
}
