# Multi-stage Dockerfile for myquiz-thymeleaf (Spring Boot GUI)

# Build stage
FROM maven:3.9.5-amazoncorretto-21 AS builder
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
ENV MVN_HOME=/usr/share/maven
WORKDIR /build
COPY . .
# Install parent module first
RUN mvn install -f pom.xml -DskipTests
RUN mvn -pl myquiz-api clean install -DskipTests
RUN mvn -pl myquiz-thymeleaf clean package -DskipTests

# Runtime stage
FROM amazoncorretto:21-alpine
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
WORKDIR /app
COPY --from=builder /build/myquiz-thymeleaf/target/myquiz-thymeleaf-*.war /app.war
ENTRYPOINT ["java","-jar","/app.war"]
EXPOSE 8081
