package com.unitbv.myquiz.auth.config;

import com.unitbv.myquiz.auth.config.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthorizationManager<RequestAuthorizationContext> adminAndModifyUser = this::hasAdminRoleAndModifyUser;
        AuthorizationManager<RequestAuthorizationContext> adminAndModifyRole = this::hasAdminRoleAndModifyRole;
        AuthorizationManager<RequestAuthorizationContext> adminAndManagePermissions = this::hasAdminRoleAndUserOrRoleModification;

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/login",
                                 "/api/auth/register",
                                 "/api/auth/health",
                                 "/api/auth/username-available",
                                 "/css/**", "/js/**", "/images/**"
                ).permitAll()
                .requestMatchers("/api/admin/users/**").access(adminAndModifyUser)
                .requestMatchers("/api/admin/roles/**").access(adminAndModifyRole)
                .requestMatchers("/api/admin/permissions/**").access(adminAndManagePermissions)
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthorizationDecision hasAdminRoleAndModifyUser(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        boolean granted = auth != null
                && hasAdminRoleAuthority(auth)
                && auth.getAuthorities().stream().anyMatch(a -> "MODIFY_USER".equals(a.getAuthority()));
        return new AuthorizationDecision(granted);
    }

    private AuthorizationDecision hasAdminRoleAndModifyRole(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        boolean granted = auth != null
                && hasAdminRoleAuthority(auth)
                && auth.getAuthorities().stream().anyMatch(a -> "MODIFY_ROLE".equals(a.getAuthority()));
        return new AuthorizationDecision(granted);
    }

    private AuthorizationDecision hasAdminRoleAndUserOrRoleModification(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        boolean granted = auth != null
                && hasAdminRoleAuthority(auth)
                && auth.getAuthorities().stream().anyMatch(a ->
                    "MODIFY_ROLE".equals(a.getAuthority()) || "MODIFY_USER".equals(a.getAuthority()));
        return new AuthorizationDecision(granted);
    }

    private boolean hasAdminRoleAuthority(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_ADMINISTRATOR".equals(a.getAuthority()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
