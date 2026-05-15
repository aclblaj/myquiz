package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.interfaces.DataCleanupApi;
import com.unitbv.myquiz.app.services.DataCleanupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * REST controller for data cleanup operations.
 * Implements comprehensive data management endpoints while preserving user data.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "${FRONTEND_URL}")
@Tag(name = "Data Management", description = "Data cleanup and management operations")
public class DataCleanupController implements DataCleanupApi {
    private static final Logger log = LoggerFactory.getLogger(DataCleanupController.class);
    private static final String PERMISSION_CLEAN_DATABASE = "CLEAN_DATABASE";
    private static final String PERMISSION_BACKUP_DATABASE = "BACKUP_DATABASE";
    private static final String PERMISSION_RESTORE_DATABASE = "RESTORE_DATABASE";

    private final DataCleanupService dataCleanupService;

    @Override
    public ResponseEntity<Void> deleteAllData() {
        if (!hasPermission(PERMISSION_CLEAN_DATABASE)) {
            return ResponseEntity.status(403).build();
        }
        log.info("DELETE /api/data/deleteall - Deleting all questionBanks data");
        try {
            dataCleanupService.deleteAllDataExceptUsersRolesPermissions();
            log.info("All questionBanks data deleted successfully");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting all data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Map<String, Long>> getStatistics() {
        log.info("GET /api/data/statistics - Getting database statistics");
        try {
            Map<String, Long> statistics = dataCleanupService.getDataStatistics();
            log.info("Statistics retrieved successfully: {}", statistics.keySet());
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<byte[]> exportSqlBackup() {
        if (!hasPermission(PERMISSION_BACKUP_DATABASE)) {
            return ResponseEntity.status(403).build();
        }

        log.info("GET /api/data/export-sql - Exporting SQL backup for non-auth data");
        try {
            String sql = dataCleanupService.exportNonAuthDataAsSql();
            String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "myQuestionBanks_backup_" + timestamp + ".sql";

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName).body(sql.getBytes());
        } catch (Exception e) {
            log.error("Error exporting SQL backup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<String> importSqlBackup(MultipartFile file) {
        if (!hasPermission(PERMISSION_RESTORE_DATABASE)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        log.info("POST /api/data/import-sql - Importing SQL backup");
        try {
            dataCleanupService.importNonAuthDataFromSql(file);
            return ResponseEntity.ok("SQL backup imported successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid SQL import request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error importing SQL backup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to import SQL backup");
        }
    }

    private boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority -> permission.equals(authority.getAuthority()));
    }
}

