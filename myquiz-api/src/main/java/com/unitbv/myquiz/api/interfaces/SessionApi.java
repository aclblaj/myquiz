package com.unitbv.myquiz.api.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API interface for Session operations.
 * This interface defines the contract for session management endpoints.
 * Note: HttpSession is handled as an implementation detail in the controller.
 */
@Tag(name = "Session", description = "Session management operations")
public interface SessionApi {

    @Operation(summary = "Set selected author", description = "Store selected author ID in session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author ID set successfully")
    })
    @PostMapping("/api/session/author")
    ResponseEntity<Void> setSelectedAuthor(
            @Parameter(description = "Author ID", required = true)
            @RequestParam("authorId") Long authorId);

    @Operation(summary = "Get selected author", description = "Retrieve selected author ID from session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved author ID")
    })
    @GetMapping("/api/session/author")
    ResponseEntity<Long> getSelectedAuthor();

    @Operation(summary = "Set selected course", description = "Store selected course in session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course set successfully")
    })
    @PostMapping("/api/session/course")
    ResponseEntity<Void> setSelectedCourse(
            @Parameter(description = "Course name", required = true)
            @RequestParam("course") String course);

    @Operation(summary = "Get selected course", description = "Retrieve selected course from session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved course")
    })
    @GetMapping("/api/session/course")
    ResponseEntity<String> getSelectedCourse();
}

