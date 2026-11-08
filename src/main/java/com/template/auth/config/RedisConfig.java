package com.template.auth.config;

import com.template.auth.infrastructure.notification.NotificationWebSocketForwarder;
import com.template.auth.infrastructure.notification.RedisNotificationPublisher;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Two Redis instances, on purpose - they never share a process or a logical
 * DB. A spike on the e-mail queue must not compete for CPU/memory with the
 * login-lockout counter (a security datum), and either side can move to
 * Redis Cluster later without the other (cluster mode only exposes DB 0, so
 * splitting by logical DB wouldn't be an option).
 *
 * The queue connection is {@code @Primary}: it's what Streams, pub/sub and
 * anything injecting a plain RedisConnectionFactory/StringRedisTemplate get.
 * Cache-side beans are addressed by qualifier.
 */
@Configuration
public class RedisConfig {

    // Every command fails fast on an outage instead of hanging the caller.
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    @Primary
    public RedisConnectionFactory queueRedisConnectionFactory(AppProperties properties) {
        return connectionFactory(properties.redis().queueUrl());
    }

    @Bean
    public RedisConnectionFactory cacheRedisConnectionFactory(AppProperties properties) {
        return connectionFactory(properties.redis().cacheUrl());
    }

    @Bean
    @Primary
    public StringRedisTemplate queueRedisTemplate(
            @Qualifier("queueRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public StringRedisTemplate cacheRedisTemplate(
            @Qualifier("cacheRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /** Subscribes this instance to new-notification events and forwards them to its STOMP sessions. */
    @Bean
    public RedisMessageListenerContainer notificationListenerContainer(
            @Qualifier("queueRedisConnectionFactory") RedisConnectionFactory connectionFactory,
            NotificationWebSocketForwarder forwarder) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListenerAdapter(forwarder, "handleMessage"),
                new ChannelTopic(RedisNotificationPublisher.CHANNEL));
        // Started (and re-started) by NotificationSubscriptionKeeper so a Redis
        // outage at boot degrades the WS push instead of taking the API down.
        container.setAutoStartup(false);
        return container;
    }

    private static LettuceConnectionFactory connectionFactory(String url) {
        RedisConfiguration configuration = LettuceConnectionFactory.createRedisConfiguration(url);
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(COMMAND_TIMEOUT)
                .build();
        return new LettuceConnectionFactory(configuration, clientConfiguration);
    }
}
