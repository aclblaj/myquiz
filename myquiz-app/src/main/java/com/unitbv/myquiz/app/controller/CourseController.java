package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.interfaces.CourseApi;
import com.unitbv.myquiz.app.services.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Course management operations.
 * Provides endpoints for CRUD operations on courses.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Course management operations - Manage academic courses")
public class CourseController implements CourseApi {
    private static final Logger log = LoggerFactory.getLogger(CourseController.class);
    private final CourseService courseService;

    @GetMapping({"/", ""})
    @Operation(
        summary = "Get All Courses",
        description = "Retrieve a list of all available courses"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<List<CourseDto>> getAllCourses() {
        log.info("Getting all courses");
        List<CourseDto> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @DeleteMapping({"/{id}"})
    @Operation(
        summary = "Delete Course",
        description = "Delete a course by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @Override
    public ResponseEntity<Void> deleteCourseById(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id) {
        log.info("Deleting course with id: {}", id);
        courseService.deleteCourseById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/{id}"})
    @Operation(
        summary = "Get Course by ID",
        description = "Retrieve a specific course by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Course found"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @Override
    public ResponseEntity<CourseDto> findById(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id) {
        log.info("Finding course with id: {}", id);
        CourseDto course = courseService.findById(id);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(course);
    }

    @PutMapping({"/{id}"})
    @Operation(
        summary = "Update Course",
        description = "Update an existing course"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Course updated successfully"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @Override
    public ResponseEntity<Void> updateCourse(
            @Parameter(description = "Course ID", required = true) @PathVariable Long id,
            @RequestBody CourseDto courseDto) {
        log.info("Updating course with id: {}", id);
        if (courseDto != null) {
            courseService.updateCourse(id, courseDto);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/", ""})
    @Operation(
        summary = "Create Course",
        description = "Create a new course"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Course created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid course data")
    })
    @Override
    public ResponseEntity<CourseDto> createCourse(@RequestBody CourseDto courseDto) {
        if (courseDto == null) {
            log.warn("Attempted to create course with null CourseDto");
            return ResponseEntity.badRequest().build();
        }
        log.info("Creating new course: {}", courseDto);
        return ResponseEntity.ok(courseService.createCourse(courseDto));
    }

}
