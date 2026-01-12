package com.unitbv.myquiz.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Centralized OpenAPI Configuration for MyQuiz Application
 *
 * This configuration provides:
 * - API documentation for all REST endpoints
 * - Interactive Swagger UI interface
 * - Standardized API responses and error handling
 * - Security documentation
 * - API versioning support
 *
 * Access the API documentation at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON spec at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.host:localhost}")
    private String serverHost;

    /**
     * Main OpenAPI configuration bean
     * Defines the complete API specification for MyQuiz application
     */
    @Bean
    public OpenAPI myQuizOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .tags(createTags())
                .components(createComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * Creates comprehensive API information
     */
    private Info createApiInfo() {
        return new Info()
                .title("MyQuiz API")
                .description("""
                    # MyQuiz REST API Documentation
                    
                    MyQuiz is a comprehensive quiz management system that provides:
                    
                    ## Features
                    - **Question Management**: Create, edit, and delete quiz questions
                    - **Quiz Management**: Organize questions into quizzes by course and author
                    - **Author Management**: Track question authors and their contributions
                    - **AI Integration**: Generate and improve questions using Ollama AI
                    - **Course Management**: Organize content by academic courses
                    - **Error Tracking**: Monitor and resolve content issues
                    
                    ## API Endpoints
                    This API provides RESTful endpoints for all major functionality:
                    - Questions CRUD operations
                    - AI-powered question generation and correction
                    - Quiz and course management
                    - Author tracking and statistics
                    
                    ## Authentication
                    Currently, the API operates without authentication for development purposes.
                    Production deployments should implement proper security measures.
                    
                    ## Response Format
                    All API responses follow standard HTTP status codes:
                    - 200: Success
                    - 201: Created
                    - 400: Bad Request
                    - 404: Not Found
                    - 500: Internal Server Error
                    """)
                .version("1.0.0")
                .contact(createContact())
                .license(createLicense());
    }

    /**
     * Creates contact information for API maintainers
     */
    private Contact createContact() {
        return new Contact()
                .name("MyQuiz Development Team")
                .email("support@myquiz.unitbv.com")
                .url("https://github.com/unitbv/myquiz");
    }

    /**
     * Creates license information
     */
    private License createLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    /**
     * Creates server configurations for different environments
     */
    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("http://" + serverHost + ":" + serverPort)
                        .description("Development Server"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development"),
                new Server()
                        .url("https://myquiz.unitbv.com")
                        .description("Production Server")
        );
    }

    /**
     * Creates API tags for grouping endpoints
     */
    private List<Tag> createTags() {
        return List.of(
                new Tag()
                        .name("Questions")
                        .description("Question management operations - Create, read, update, and delete quiz questions"),
                new Tag()
                        .name("AI Integration")
                        .description("Ollama AI integration for question generation and improvement"),
                new Tag()
                        .name("Quizzes")
                        .description("Quiz management operations - Organize questions into cohesive quizzes"),
                new Tag()
                        .name("Authors")
                        .description("Author management and tracking operations"),
                new Tag()
                        .name("Courses")
                        .description("Course management and organization operations"),
                new Tag()
                        .name("Health Check")
                        .description("System health and status endpoints")
        );
    }

    /**
     * Creates reusable components including security schemes and response schemas
     */
    private Components createComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", createSecurityScheme())
                .addSchemas("ErrorResponse", createErrorResponseSchema())
                .addSchemas("SuccessResponse", createSuccessResponseSchema());
    }

    /**
     * Creates security scheme for future authentication implementation
     */
    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token authentication (not currently implemented)");
    }

    /**
     * Creates error response schema for consistent error handling
     */
    private io.swagger.v3.oas.models.media.Schema<?> createErrorResponseSchema() {
        return new io.swagger.v3.oas.models.media.Schema<>()
                .type("object")
                .addProperty("timestamp", new io.swagger.v3.oas.models.media.Schema<>().type("string").format("date-time"))
                .addProperty("status", new io.swagger.v3.oas.models.media.Schema<>().type("integer"))
                .addProperty("error", new io.swagger.v3.oas.models.media.Schema<>().type("string"))
                .addProperty("message", new io.swagger.v3.oas.models.media.Schema<>().type("string"))
                .addProperty("path", new io.swagger.v3.oas.models.media.Schema<>().type("string"));
    }

    /**
     * Creates success response schema for consistent success responses
     */
    private io.swagger.v3.oas.models.media.Schema<?> createSuccessResponseSchema() {
        return new io.swagger.v3.oas.models.media.Schema<>()
                .type("object")
                .addProperty("message", new io.swagger.v3.oas.models.media.Schema<>().type("string"))
                .addProperty("timestamp", new io.swagger.v3.oas.models.media.Schema<>().type("string").format("date-time"))
                .addProperty("data", new io.swagger.v3.oas.models.media.Schema<>().type("object"));
    }
}
