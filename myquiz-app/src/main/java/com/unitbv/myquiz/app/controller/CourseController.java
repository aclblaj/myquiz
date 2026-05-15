package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.api.interfaces.CourseApi;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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
    private static final String PERMISSION_EXPORT_XML = "EXPORT_XML";
    private final CourseService courseService;
    private final ExportService exportService;

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

    @GetMapping("/{id}/export-xml")
    @Override
    public ResponseEntity<byte[]> exportCourseXml(@PathVariable Long id) {
        if (!hasExportXmlPermission()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CourseDto course = courseService.findById(id);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }

        String courseName = course.getCourse();
        String xml = exportService.generateCourseXml(courseName);
        String safeCourseName = courseName != null ? courseName.replaceAll("[^a-zA-Z0-9]", "_") : "course";
        String filename = safeCourseName + "_all_questionBanks.xml";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .body(xml.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasExportXmlPermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
            .anyMatch(authority -> PERMISSION_EXPORT_XML.equals(authority.getAuthority()));
    }

    @PostMapping("/{id}/recompute-duplicates")
    @Override
    public ResponseEntity<CourseDuplicateRecomputeResultDto> recomputeCourseDuplicates(@PathVariable Long id) {
        try {
            CourseDuplicateRecomputeResultDto result = courseService.recomputeDuplicatesForCourse(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(id).addArgument(e.getMessage())
                .log("Could not recompute duplicates for course {}: {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id)
                .log("Failed to recompute duplicates for course {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/recompute-with-strategy")
    public ResponseEntity<CourseDuplicateRecomputeResultDto> recomputeCourseDuplicatesWithStrategy(
            @RequestParam(required = false) Long courseId,
            @RequestParam String strategy,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId) {
        try {
            CourseDuplicateRecomputeResultDto result;
            if (questionBankId != null && authorId != null) {
                result = courseService.recomputeDuplicatesForAuthor(questionBankId, authorId, strategy);
            } else if (questionBankId != null) {
                result = courseService.recomputeDuplicatesForQuestionBank(questionBankId, strategy);
            } else {
                if (courseId == null) {
                    return ResponseEntity.badRequest().build();
                }
                result = courseService.recomputeDuplicatesForCourseWithStrategy(courseId, strategy);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(courseId).addArgument(e.getMessage())
                .log("Could not recompute duplicates for courseId '{}': {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId)
                .log("Failed to recompute duplicates for courseId '{}'");
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/duplicate-statistics")
    public ResponseEntity<DuplicateStatisticsDto> getDuplicateStatistics(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId) {
        try {
            DuplicateStatisticsDto statistics;
            if (questionBankId != null && authorId != null) {
                statistics = courseService.getDuplicateStatisticsForAuthor(questionBankId, authorId);
            } else if (questionBankId != null) {
                statistics = courseService.getDuplicateStatisticsForQuestionBank(questionBankId);
            } else {
                if (courseId == null) {
                    return ResponseEntity.badRequest().build();
                }
                statistics = courseService.getDuplicateStatistics(courseId);
            }
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(courseId).addArgument(e.getMessage())
                .log("Could not get statistics for courseId '{}': {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId)
                .log("Failed to get statistics for courseId '{}'");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/clear-duplicates")
    public ResponseEntity<Integer> clearDuplicatesForCourse(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId) {
        try {
            int clearedCount;
            if (questionBankId != null && authorId != null) {
                clearedCount = courseService.clearDuplicatesForAuthor(questionBankId, authorId);
            } else if (questionBankId != null) {
                clearedCount = courseService.clearDuplicatesForQuestionBank(questionBankId);
            } else {
                if (courseId == null) {
                    return ResponseEntity.badRequest().build();
                }
                clearedCount = courseService.clearDuplicatesForCourse(courseId);
            }
            return ResponseEntity.ok(clearedCount);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(courseId).addArgument(e.getMessage())
                .log("Could not clear duplicates for courseId '{}': {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId)
                .log("Failed to clear duplicates for courseId '{}'");
            return ResponseEntity.internalServerError().build();
        }
    }

}
