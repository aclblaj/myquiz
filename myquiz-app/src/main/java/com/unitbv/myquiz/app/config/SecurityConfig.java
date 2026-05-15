package com.unitbv.myquiz.app.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})  // Enable CORS with default CorsConfigurationSource bean
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                // Let framework error dispatch through so the real error is returned/logged.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/error",
                    "/error/**",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/users/find/**",
                    "/api/system/health",
                    "/api/upload/**",  // Allow upload endpoints for internal service communication
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // All other API endpoints require authentication (JWT token)
                .requestMatchers("/api/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
