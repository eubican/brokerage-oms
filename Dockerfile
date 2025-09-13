# syntax=docker/dockerfile:1

# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Gradle wrapper and build files first for better caching
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts /app/
COPY gradle /app/gradle

# Ensure gradlew is executable on Linux (for local linux builds); harmless on Windows-based engines
RUN chmod +x gradlew || true

# Download dependencies
RUN ./gradlew --no-daemon dependencies || true

# Copy source
COPY src /app/src

# Build the application (create a bootable jar)
RUN ./gradlew --no-daemon clean bootJar

# Runtime stage
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.source="https://example.com/oms" \
      org.opencontainers.image.title="OMS" \
      org.opencontainers.image.description="Order Management Service"

WORKDIR /opt/app

# Add a non-root user
RUN useradd -r -u 1001 spring && mkdir -p /opt/app && chown -R spring:spring /opt/app
USER spring

# Copy the jar from the builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Healthcheck hitting Spring Boot actuator if enabled
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Allow overriding Spring profile and JVM options via env
ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE="default" \
    SERVER_PORT=8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$SERVER_PORT -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
