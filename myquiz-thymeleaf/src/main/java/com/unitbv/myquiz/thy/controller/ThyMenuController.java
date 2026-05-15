package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.thy.service.SessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller advice to add menu permissions to all views.
 * Extracts permissions from JWT token and provides them to templates for menu filtering.
 */
@ControllerAdvice
public class ThyMenuController {

    private static final Logger logger = LoggerFactory.getLogger(ThyMenuController.class);

    private final SessionService sessionService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public ThyMenuController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Provides menu permissions to all views for conditional rendering.
     * Extracts permissions from JWT token stored in session.
     *
     * @return Map of permission flags for menu rendering
     */
    @ModelAttribute("menuPermissions")
    public Map<String, Boolean> getMenuPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();

        // Default all permissions to false
        permissions.put("canReadCourse", false);
        permissions.put("canModifyCourse", false);
        permissions.put("canReadQuiz", false);
        permissions.put("canModifyQuiz", false);
        permissions.put("canReadQuestion", false);
        permissions.put("canModifyQuestion", false);
        permissions.put("canReadAuthor", false);
        permissions.put("canModifyAuthor", false);
        permissions.put("canUploadFiles", false);
        permissions.put("canUseAiTools", false);
        permissions.put("canManageUsers", false);
        permissions.put("canManageRoles", false);
        permissions.put("canViewExtendedStatistics", false);
        permissions.put("canCleanDatabase", false);
        permissions.put("canBackupDatabase", false);
        permissions.put("canRestoreDatabase", false);
        permissions.put("canExportXml", false);
        permissions.put("canReadQuestionErrors", false);
        permissions.put("canModifyQuestionErrors", false);

        try {
            String token = sessionService.getJwtToken();
            if (token == null || token.isBlank()) {
                logger.debug("[ThyMenuController] No JWT token in session, returning default permissions");
                return permissions;
            }

            // Extract permissions from JWT
            Set<String> userPermissions = extractPermissions(token);
            // "ADMIN" or "ADMINISTRATOR"
            boolean isAdmin = sessionService.hasAdminRole();

            logger.debug("[ThyMenuController] User has {} permissions", userPermissions.size());

            // Map permissions to menu flags
            permissions.put("canReadCourse", userPermissions.contains("READ_COURSE"));
            permissions.put("canModifyCourse", userPermissions.contains("MODIFY_COURSE"));
            permissions.put("canReadQuiz", userPermissions.contains("READ_QUIZ"));
            permissions.put("canModifyQuiz", userPermissions.contains("MODIFY_QUIZ"));
            permissions.put("canReadQuestion", userPermissions.contains("READ_QUESTION"));
            permissions.put("canModifyQuestion", userPermissions.contains("MODIFY_QUESTION"));
            permissions.put("canReadAuthor", userPermissions.contains("READ_AUTHOR"));
            permissions.put("canModifyAuthor", userPermissions.contains("MODIFY_AUTHOR"));
            permissions.put("canUploadFiles", userPermissions.contains("UPLOAD_FILES"));
            permissions.put("canUseAiTools", userPermissions.contains("AI_TOOLS"));
            permissions.put("canManageUsers", isAdmin && userPermissions.contains("MODIFY_USER"));
            permissions.put("canManageRoles", isAdmin && userPermissions.contains("MODIFY_ROLE"));
            permissions.put("canViewExtendedStatistics", userPermissions.contains("VIEW_EXTENDED_STATISTICS"));
            permissions.put("canCleanDatabase", userPermissions.contains("CLEAN_DATABASE"));
            permissions.put("canBackupDatabase", userPermissions.contains("BACKUP_DATABASE"));
            permissions.put("canRestoreDatabase", userPermissions.contains("RESTORE_DATABASE"));
            permissions.put("canExportXml", userPermissions.contains("EXPORT_XML"));
            permissions.put("canManageQuestionDuplicates", userPermissions.contains("VIEW_QUESTION_DUPLICATES"));
            permissions.put("canReadQuestionErrors", userPermissions.contains("READ_QUESTION"));
            permissions.put("canModifyQuestionErrors", userPermissions.contains("MODIFY_QUESTION"));

        } catch (Exception e) {
            logger.error("[ThyMenuController] Error extracting permissions from JWT: {}", e.getMessage());
        }

        return permissions;
    }

    /**
     * Extract permissions from JWT token
     *
     * @param token JWT token
     * @return Set of permission names
     */
    private Set<String> extractPermissions(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

            @SuppressWarnings("unchecked") List<String> permissionsList = claims.get("permissions", List.class);
            if (permissionsList != null) {
                return new HashSet<>(permissionsList);
            }
            return new HashSet<>();
        } catch (Exception e) {
            logger.error("[ThyMenuController] Failed to extract permissions: {}", e.getMessage());
            return new HashSet<>();
        }
    }
}

