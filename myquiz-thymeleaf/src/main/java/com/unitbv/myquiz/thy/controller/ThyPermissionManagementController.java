package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Thymeleaf controller for permission viewing in admin interface
 */
@Controller
@SessionAttributes(ControllerSettings.ATTR_LOGGED_IN_USER)
@RequestMapping("/admin/permissions")
public class ThyPermissionManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ThyPermissionManagementController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${AUTH_API_URL}")
    private String authApiUrl;

    @Autowired
    public ThyPermissionManagementController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Get admin API base URL by replacing /api/auth with /api/admin
     */
    private String getAdminApiUrl() {
        return authApiUrl.replace("/api/auth", "/api/admin");
    }

    /**
     * List all permissions
     */
    @GetMapping({"", "/"})
    public String listPermissions(Model model) {
        String sessionCheck = sessionService.validateSessionOrRedirect();
        if (sessionCheck != null) return sessionCheck;
        if (!sessionService.hasAdminRole()) {
            return "redirect:/?error=Access+denied";
        }
        if (!sessionService.hasPermission("MODIFY_ROLE") && !sessionService.hasPermission("MODIFY_USER")) {
            return "redirect:/?error=Access+denied";
        }
        logger.info("Fetching permission list");
        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    getAdminApiUrl() + "/permissions", HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            );

            model.addAttribute(ControllerSettings.ATTR_PERMISSIONS, response.getBody());
            return "admin/permission-list";
        } catch (Exception e) {
            logger.error("Error fetching permissions: {}", e.getMessage());
            model.addAttribute(ControllerSettings.ATTR_ERROR, "Failed to load permissions");
            return "admin/permission-list";
        }
    }
}

