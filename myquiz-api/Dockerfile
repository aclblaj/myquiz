# Multi-stage Dockerfile for myquiz-api (Spring Boot Backend)

# Build stage
FROM maven:3.9.5-amazoncorretto-21 AS builder
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
ENV MVN_HOME=/usr/share/maven
WORKDIR /build
COPY . .
WORKDIR /build
RUN mvn -pl myquiz-api clean install -DskipTests

# Runtime stage
FROM amazoncorretto:21-alpine
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
WORKDIR /app
COPY --from=builder /build/myquiz-api/target/myquiz-api-*.jar app.jar
# No ENTRYPOINT, not standalone
