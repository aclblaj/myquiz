# Multi-stage Dockerfile for MyQuiz multi-module Maven project

# Stage 1: Build stage
FROM maven:3.9.6-amazoncorretto-21 AS builder

# Set working directory
WORKDIR /app

# Copy parent pom.xml first for better layer caching
COPY pom.xml .

# Copy module pom.xml files
COPY myquiz-api/pom.xml ./myquiz-api/
COPY myquiz-app/pom.xml ./myquiz-app/

# Download dependencies (this layer will be cached if pom.xml files don't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY myquiz-api/src ./myquiz-api/src
COPY myquiz-app/src ./myquiz-app/src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM amazoncorretto:21-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create app user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built WAR file from builder stage
COPY --from=builder /app/myquiz-app/target/myquiz-app-*.war app.war

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/system/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.war"]

# Optional JVM tuning (uncomment and adjust as needed)
# ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "/app/app.war"]
