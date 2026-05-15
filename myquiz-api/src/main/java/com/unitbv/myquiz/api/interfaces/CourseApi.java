package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * API interface for Course operations.
 * This interface defines the contract for course management endpoints.
 */
@Tag(name = "Courses", description = "Course management operations")
public interface CourseApi {

    @Operation(summary = "Get all courses", description = "Retrieve all courses in the system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved courses"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping
    ResponseEntity<List<CourseDto>> getAllCourses();

    @Operation(summary = "Get course by ID", description = "Retrieve a specific course by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved course"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}")
    ResponseEntity<CourseDto> findById(@Parameter(description = "Course ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create new course", description = "Create a new course")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Course created successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping
    ResponseEntity<CourseDto> createCourse(@Parameter(description = "Course data", required = true) @RequestBody CourseDto courseDto);

    @Operation(summary = "Update course", description = "Update an existing course by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Course updated successfully"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PutMapping("/{id}")
    ResponseEntity<Void> updateCourse(@Parameter(description = "Course ID", required = true) @PathVariable Long id,
                                      @Parameter(description = "Course data", required = true) @RequestBody CourseDto courseDto);

    @Operation(summary = "Delete course", description = "Delete a course by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Course deleted successfully"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCourseById(@Parameter(description = "Course ID", required = true) @PathVariable Long id);

    @Operation(summary = "Export course questions as XML", description = "Download Moodle-compatible XML containing questions from all questionBanks in a course")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Course XML exported successfully"), @ApiResponse(responseCode = "403", description = "Missing EXPORT_XML permission"), @ApiResponse(responseCode = "404", description = "Course not found")})
    @GetMapping("/{id}/export-xml")
    ResponseEntity<byte[]> exportCourseXml(@Parameter(description = "Course ID", required = true) @PathVariable Long id);

    @Operation(summary = "Recompute duplicates for course", description = "Clear and recompute duplicate links and duplicate-related errors for all questions in the selected course")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicate recomputation completed"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/recompute-duplicates")
    ResponseEntity<CourseDuplicateRecomputeResultDto> recomputeCourseDuplicates(@Parameter(description = "Course ID", required = true) @PathVariable Long id);
}
