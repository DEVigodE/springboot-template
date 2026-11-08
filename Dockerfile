# syntax=docker/dockerfile:1

FROM eclipse-temurin:27-jdk-alpine AS build
WORKDIR /app

# Dependency layer first: only re-downloads when the pom changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:27-jre-alpine AS runtime
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_ENV=production \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /app/target/*.jar /app/app.jar

USER spring
EXPOSE 3000

# /health/live is unversioned and skips the rate limiter by design (HealthController).
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-3000}/health/live" > /dev/null || exit 1

# `server.shutdown=graceful` + the 10s queue job timeout: give the container
# at least 20s to stop (docker run --stop-timeout 20 / stop_grace_period).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
