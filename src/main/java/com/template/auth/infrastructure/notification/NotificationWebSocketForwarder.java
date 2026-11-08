package com.template.auth.infrastructure.notification;

import com.template.auth.application.notification.NotificationDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis subscriber side: forwards each published notification to the owning
 * user's STOMP sessions on this instance ({@code /user/queue/notifications}).
 * The payload is relayed as the JSON string it arrived as.
 */
@Component
public class NotificationWebSocketForwarder {

    public static final String USER_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final JsonMapper json;

    public NotificationWebSocketForwarder(SimpMessagingTemplate messagingTemplate, JsonMapper json) {
        this.messagingTemplate = messagingTemplate;
        this.json = json;
    }

    /** Invoked by the RedisMessageListenerContainer (see WebSocketConfig). */
    public void handleMessage(String message) {
        NotificationDto notification = json.readValue(message, NotificationDto.class);
        messagingTemplate.convertAndSendToUser(notification.userId().toString(), USER_DESTINATION, message);
    }
}
