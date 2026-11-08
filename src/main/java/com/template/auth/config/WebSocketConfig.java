package com.template.auth.config;

import com.template.auth.infrastructure.security.StompJwtChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket at {@code /ws}. A client connects with
 * {@code Authorization: Bearer <token>} as a CONNECT header and subscribes to
 * {@code /user/queue/notifications}; it receives each new notification as soon
 * as it is created (on any replica - see RedisNotificationPublisher).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor jwtInterceptor;
    private final AppProperties properties;

    public WebSocketConfig(StompJwtChannelInterceptor jwtInterceptor, AppProperties properties) {
        this.jwtInterceptor = jwtInterceptor;
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(properties.cors().allowsAny()
                        ? new String[] {"*"}
                        : properties.cors().allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker: fine because fan-out across replicas happens
        // through Redis pub/sub before messages ever reach a broker.
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtInterceptor);
    }
}
