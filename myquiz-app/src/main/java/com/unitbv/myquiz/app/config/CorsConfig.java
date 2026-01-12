package com.unitbv.myquiz.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for myquiz-app.
 * Allows cross-origin requests from the frontend application.
 */
@Configuration
public class CorsConfig {

    @Value("${FRONTEND_URL:http://localhost:8080}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from frontend
        config.setAllowedOrigins(List.of(frontendUrl, "http://localhost:8080", "http://myquiz-thymeleaf:8080"));

        // Allow all HTTP methods including PUT, DELETE
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        config.setAllowedHeaders(List.of("*"));

        // Allow credentials (important for sessions and auth tokens)
        config.setAllowCredentials(true);

        // Expose authorization headers
        config.setExposedHeaders(List.of("Authorization"));

        // Cache preflight responses for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

