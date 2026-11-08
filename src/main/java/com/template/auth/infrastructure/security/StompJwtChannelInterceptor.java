package com.template.auth.infrastructure.security;

import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.UserInactiveException;
import com.template.auth.domain.user.UserEntity;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * The WebSocket handshake has no Authorization header the way a normal HTTP
 * request does - the STOMP client sends the access token as a CONNECT header
 * ({@code Authorization: Bearer <token>}) instead, verified here by hand.
 * The resolved principal's name is the user id, which is what
 * {@code convertAndSendToUser} routes on.
 */
@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompJwtChannelInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    public StompJwtChannelInterceptor(JwtDecoder jwtDecoder, UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("Missing bearer token on STOMP CONNECT");
        }

        try {
            Jwt jwt = jwtDecoder.decode(header.substring(BEARER_PREFIX.length()).trim());
            UserEntity user = userRepository.findById(UUID.fromString(jwt.getSubject()))
                    .orElseThrow(() -> new MessageDeliveryException("Unknown user"));
            user.verifyActive();
            accessor.setUser(AuthenticatedUserFactory.authenticationFor(user));
            return message;
        } catch (JwtException | IllegalArgumentException | UserInactiveException e) {
            log.debug("WS handshake rejected: {}", e.getMessage());
            throw new MessageDeliveryException("Invalid access token");
        }
    }
}
