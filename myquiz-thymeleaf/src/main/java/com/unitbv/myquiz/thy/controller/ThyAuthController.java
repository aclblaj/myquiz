package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Thymeleaf controller for authentication operations.
 * Handles login, registration, and logout functionality.
 */
@Controller
@SessionAttributes(ControllerSettings.ATTR_LOGGED_IN_USER)
@RequestMapping("/auth")
public class ThyAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ThyAuthController.class);

    private final RestTemplate restTemplate;

    @Value("${AUTH_API_URL}")
    private String authApiUrl;

    @Autowired
    public ThyAuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        logger.info("Rendering login form");
        model.addAttribute(ControllerSettings.ATTR_LOGIN_ERROR, false);
        return ControllerSettings.VIEW_LOGIN;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        logger.info("Login attempt for username: {}", username);
        Map<String, String> req = new HashMap<>();
        req.put("identifier", username);
        req.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(req, headers);
        try {
            @SuppressWarnings("unchecked") ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(authApiUrl + "/login", entity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            Map<String, Object> body = response.getBody();
            boolean success = false;
            String jwtToken = null;
            if (body != null && body.get("token") instanceof String token && !token.isEmpty()) {
                success = true;
                jwtToken = token;
            }
            if (response.getStatusCode() == HttpStatus.OK && success) {
                model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, username);
                session.setAttribute(ControllerSettings.ATTR_JWT_TOKEN, jwtToken);
                logger.debug("Login success: loggedInUser set to {} in model, jwtToken set to {} in session", username, jwtToken);
                return ControllerSettings.REDIRECT_HOME;
            } else if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warn("Login failed: user account is not activated");
                model.addAttribute(ControllerSettings.ATTR_LOGIN_ERROR, true);
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Your account is pending activation by an administrator. Please contact support.");
                model.addAttribute(ControllerSettings.ATTR_USERNAME, username);
                return ControllerSettings.VIEW_LOGIN;
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                logger.warn("Login failed: invalid credentials or user not found");
                model.addAttribute(ControllerSettings.ATTR_LOGIN_ERROR, true);
                model.addAttribute(ControllerSettings.ATTR_USERNAME, username);
                return ControllerSettings.VIEW_LOGIN;
            } else {
                logger.warn("Login failed: JWT token not received from backend");
                model.addAttribute(ControllerSettings.ATTR_LOGIN_ERROR, true);
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Login failed: No token received. Please try again or contact support.");
                model.addAttribute(ControllerSettings.ATTR_USERNAME, username);
                return ControllerSettings.VIEW_LOGIN;
            }
        } catch (Exception e) {
            logger.error("Login error: {}", e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_LOGIN_ERROR, true);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Login error: " + e.getMessage());
            model.addAttribute(ControllerSettings.ATTR_USERNAME, username);
            return ControllerSettings.VIEW_LOGIN;
        }
    }

    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String username, Model model) {
        logger.info("Rendering register form for username: {}", username);
        model.addAttribute(ControllerSettings.ATTR_REGISTER_ERROR, false);
        if (username != null) {
            model.addAttribute(ControllerSettings.ATTR_USERNAME, username);
        }
        return ControllerSettings.VIEW_REGISTER;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String email, @RequestParam String password, Model model, HttpSession session) {
        logger.info("Register attempt for username: {}, email: {}", username, email);
        Map<String, String> req = new HashMap<>();
        req.put(ControllerSettings.ATTR_USERNAME, username);
        req.put("email", email);
        req.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(req, headers);
        try {
            @SuppressWarnings("unchecked") ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    authApiUrl + "/register", entity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            Map<String, Object> body = response.getBody();

            if (response.getStatusCode() == HttpStatus.CREATED && body != null) {
                // Registration successful - user created but inactive
                String message = body.get("message") != null ? body.get("message").toString() : "Registration successful. Your account is pending activation by an administrator.";
                model.addAttribute(ControllerSettings.ATTR_REGISTER_ERROR, false);
                model.addAttribute(ControllerSettings.ATTR_REGISTRATION_SUCCESS, true);
                model.addAttribute(ControllerSettings.ATTR_REGISTRATION_MESSAGE, message);
                return ControllerSettings.VIEW_REGISTER;
            } else {
                String errorMsg = body != null && body.get("message") != null ? body.get("message").toString() : ControllerSettings.DEFAULT_REGISTRATION_ERROR;
                model.addAttribute(ControllerSettings.ATTR_REGISTER_ERROR, true);
                model.addAttribute(ControllerSettings.ATTR_REGISTER_ERROR_MSG, errorMsg);
                return ControllerSettings.VIEW_REGISTER;
            }
        } catch (Exception e) {
            model.addAttribute(ControllerSettings.ATTR_REGISTER_ERROR, true);
            model.addAttribute(ControllerSettings.ATTR_REGISTER_ERROR_MSG, ControllerSettings.DEFAULT_INTERNAL_ERROR);
            return ControllerSettings.VIEW_REGISTER;
        }
    }

    @GetMapping("/logout")
    public String logout(SessionStatus status) {
        logger.info("User logged out");
        status.setComplete();
        return ControllerSettings.REDIRECT_HOME;
    }
}
