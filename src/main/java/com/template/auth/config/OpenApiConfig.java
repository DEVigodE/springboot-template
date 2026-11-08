package com.template.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /docs (springdoc.swagger-ui.path), spec at /v3/api-docs. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Spring Boot Auth Template API",
                version = "1.0",
                description = "Auth, RBAC, file uploads, and notifications for the Spring Boot auth template."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
