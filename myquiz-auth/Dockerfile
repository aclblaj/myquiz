FROM maven:3.9.5-amazoncorretto-21 AS build
WORKDIR /build
# Copy all sources for compilation
COPY myquiz-api myquiz-api
COPY myquiz-app myquiz-app
COPY myquiz-thymeleaf myquiz-thymeleaf
COPY myquiz-iam myquiz-iam
COPY myquiz-auth myquiz-auth
COPY pom.xml pom.xml
# Compile only myquiz-auth and its dependencies
RUN mvn -pl myquiz-auth clean install -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /myquiz-auth
COPY --from=build /build/myquiz-auth/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8082
