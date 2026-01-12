package com.unitbv.myquiz.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtUtil jwtUtil;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        log.debug("[JwtFilter] Authorization header: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug("[JwtFilter] Extracted JWT: {}", jwt);
            try {
                username = jwtUtil.extractUsername(jwt);
                log.debug("[JwtFilter] Extracted username: {}", username);
            } catch (Exception e) {
                log.error("[JwtFilter] Exception extracting username from JWT: {}", e.getMessage(), e);
            }
        } else {
            log.debug("[JwtFilter] No valid Bearer token found in Authorization header.");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtUtil.validateToken(jwt)) {
                    log.debug("[JwtFilter] JWT validated for user: {}", username);
                    // Create authentication token with username from JWT
                    // No need for UserDetailsService - JWT contains all needed information
                    UsernamePasswordAuthenticationToken token =
                            new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(token);
                } else {
                    log.debug("[JwtFilter] JWT validation failed for user: {} (validateToken returned false)", username);
                }
            } catch (Exception e) {
                log.error("[JwtFilter] Exception during JWT validation for user {}: {}", username, e.getMessage(), e);
            }
        } else if (username == null) {
            log.debug("[JwtFilter] Username could not be extracted from JWT.");
        }

        chain.doFilter(request, response);
    }
}
