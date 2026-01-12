package com.unitbv.myquiz.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.SessionTrackingMode;
import java.util.Collections;

/**
 * Session Configuration for MyQuiz Application
 *
 * This configuration disables URL-based session tracking to prevent
 * JSESSIONID from appearing in URLs (e.g., /quiz/;jsessionid=...).
 *
 * Benefits:
 * - Cleaner URLs without session IDs
 * - Better security (session IDs not exposed in URLs)
 * - Prevents routing issues with Spring MVC
 * - Forces cookie-based session management
 *
 * Sessions will only be tracked via cookies, which is the modern
 * and recommended approach for web applications.
 */
@Configuration
public class SessionConfig {

    /**
     * Configures servlet context to use cookie-only session tracking
     *
     * @return ServletContextInitializer that disables URL session tracking
     */
    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            // Disable URL-based session tracking, use cookies only
            servletContext.setSessionTrackingModes(
                Collections.singleton(SessionTrackingMode.COOKIE)
            );
        };
    }
}

