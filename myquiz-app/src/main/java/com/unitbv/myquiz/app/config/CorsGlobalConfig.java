package com.unitbv.myquiz.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsGlobalConfig {

    @Value("${FRONTEND_URL:http://localhost:8080}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String envFrontendUrl = System.getenv("FRONTEND_URL");
                String effectiveFrontendUrl = envFrontendUrl != null ? envFrontendUrl : frontendUrl;
                if (effectiveFrontendUrl != null && !effectiveFrontendUrl.isBlank()) {
                    registry.addMapping("/**")
                        .allowedOrigins(effectiveFrontendUrl)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
                } else {
                    registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
                }
            }
        };
    }
}