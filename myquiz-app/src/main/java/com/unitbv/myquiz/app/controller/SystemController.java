package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.interfaces.SystemApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for System operation endpoints.
 * Provides health check, system information, and status monitoring endpoints.
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "${FRONTEND_URL:*}")
@RequiredArgsConstructor
public class SystemController implements SystemApi {
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    private final Optional<DataSource> dataSource;
    private final Optional<BuildProperties> buildProperties;

    @Override
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check endpoint called");
        Map<String, Object> response = new HashMap<>();

        try {
            response.put(ControllerSettings.STATUS_KEY, ControllerSettings.UP);
            response.put(ControllerSettings.SERVICE_KEY, "myquiz-app");
            response.put(ControllerSettings.TIMESTAMP_KEY, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            response.put("application", "myquiz");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during health check", e);
            response.put(ControllerSettings.STATUS_KEY, ControllerSettings.DOWN);
            response.put(ControllerSettings.ERROR_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        logger.debug("System info endpoint called");
        Map<String, Object> response = new HashMap<>();

        try {
            response.put(ControllerSettings.TIMESTAMP_KEY, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            response.put(ControllerSettings.SERVICE_KEY, "myquiz-app");
            response.put("javaVersion", System.getProperty("java.version"));
            response.put("osName", System.getProperty("os.name"));
            response.put("osVersion", System.getProperty("os.version"));
            response.put("osArch", System.getProperty("os.arch"));

            // Add build information if available
            buildProperties.ifPresent(props -> {
                response.put("appVersion", props.getVersion());
                response.put("appName", props.getName());
            });

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving system info", e);
            response.put(ControllerSettings.ERROR_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        logger.debug("Database status endpoint called");
        Map<String, Object> response = new HashMap<>();

        try {
            if (dataSource.isEmpty()) {
                logger.debug("DataSource not configured");
                response.put(ControllerSettings.STATUS_KEY, "UNAVAILABLE");
                response.put("message", "DataSource not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }

            // Try to get a connection to verify database connectivity
            try (var connection = dataSource.get().getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    response.put(ControllerSettings.STATUS_KEY, ControllerSettings.UP);
                    response.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
                    response.put("databaseVersion", connection.getMetaData().getDatabaseMajorVersion() + "." + connection.getMetaData().getDatabaseMinorVersion());
                    response.put(ControllerSettings.TIMESTAMP_KEY, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    return ResponseEntity.ok(response);
                }
            }

            response.put(ControllerSettings.STATUS_KEY, ControllerSettings.DOWN);
            response.put(ControllerSettings.ERROR_KEY, "Failed to establish database connection");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);

        } catch (Exception e) {
            logger.error("Error checking database status", e);
            response.put(ControllerSettings.STATUS_KEY, ControllerSettings.DOWN);
            response.put(ControllerSettings.ERROR_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> getMetrics() {
        logger.debug("Metrics endpoint called");
        Map<String, Object> response = new HashMap<>();

        try {
            // JVM Memory metrics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            Map<String, Object> memoryMetrics = new HashMap<>();

            // Heap memory
            var heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            memoryMetrics.put("heapUsed", heapMemoryUsage.getUsed());
            memoryMetrics.put("heapMax", heapMemoryUsage.getMax());
            memoryMetrics.put("heapCommitted", heapMemoryUsage.getCommitted());
            memoryMetrics.put("heapUsagePercent", (heapMemoryUsage.getUsed() * 100.0) / heapMemoryUsage.getMax());

            // Non-heap memory
            var nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();
            memoryMetrics.put("nonHeapUsed", nonHeapMemoryUsage.getUsed());
            memoryMetrics.put("nonHeapCommitted", nonHeapMemoryUsage.getCommitted());

            response.put("memory", memoryMetrics);
            response.put(ControllerSettings.TIMESTAMP_KEY, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            response.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
            response.put("processorCount", Runtime.getRuntime().availableProcessors());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving metrics", e);
            response.put(ControllerSettings.ERROR_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}




















