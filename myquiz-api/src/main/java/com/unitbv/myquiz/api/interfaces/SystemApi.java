package com.unitbv.myquiz.api.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * API interface for System operations.
 * This interface defines the contract for system health checks and information endpoints.
 */
@Tag(name = "System", description = "System health and information operations")
public interface SystemApi {

    @Operation(summary = "Health check", description = "Check system health status")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "System is healthy"), @ApiResponse(responseCode = "503", description = "System is unhealthy")})
    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> healthCheck();

    @Operation(summary = "System information", description = "Get system information and version details")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved system info")})
    @GetMapping("/info")
    ResponseEntity<Map<String, Object>> getSystemInfo();

    @Operation(summary = "Database status", description = "Check database connectivity and status")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Database is accessible"), @ApiResponse(responseCode = "503", description = "Database is not accessible")})
    @GetMapping("/database")
    ResponseEntity<Map<String, Object>> getDatabaseStatus();

    @Operation(summary = "Get application metrics", description = "Retrieve application performance metrics")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved metrics")})
    @GetMapping("/metrics")
    ResponseEntity<Map<String, Object>> getMetrics();
}
