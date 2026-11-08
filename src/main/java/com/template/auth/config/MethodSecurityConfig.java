package com.template.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;

/**
 * Turns on {@code @PreAuthorize} and the annotation templates that let
 * {@code @RequirePermission("users:read")} expand to
 * {@code hasAuthority('users:read')} (see presentation.shared.security).
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }
}
