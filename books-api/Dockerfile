# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom first for layer caching
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q 2>/dev/null || mvn dependency:go-offline -q || true

COPY src ./src
RUN mvn package -DskipTests -q

# --- Runtime stage (minimal attack surface) ---
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Non-root user for least privilege
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Expose only the app port
EXPOSE 8080

# No debug ports, no agent; JVM flags harden memory
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
  "-jar", "app.jar"]
