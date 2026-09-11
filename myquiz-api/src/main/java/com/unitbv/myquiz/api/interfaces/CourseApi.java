package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.DuplicateRecomputeHistoryDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * API interface for Course operations.
 * This interface defines the contract for course management endpoints.
 */
@Tag(name = "Courses", description = "Course management operations")
public interface CourseApi {

    @Operation(summary = "Get all courses", description = "Retrieve all courses in the system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved courses"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping({"", "/"})
    ResponseEntity<List<CourseDto>> getAllCourses();

    @Operation(summary = "Get course by ID", description = "Retrieve a specific course by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved course"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}")
    ResponseEntity<CourseDto> findById(@Parameter(description = "Course ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create new course", description = "Create a new course")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Course created successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping
    ResponseEntity<CourseDto> createCourse(@Parameter(description = "Course data", required = true) @Valid @RequestBody CourseDto courseDto);

    @Operation(summary = "Update course", description = "Update an existing course by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Course updated successfully"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PutMapping("/{id}")
    ResponseEntity<Void> updateCourse(@Parameter(description = "Course ID", required = true) @PathVariable Long id,
                                      @Parameter(description = "Course data", required = true) @Valid @RequestBody CourseDto courseDto);

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

    @Operation(summary = "Recompute duplicates with strategy", description = "Recompute duplicates for a course, question bank, or author scope using the selected strategy")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicate recomputation completed"), @ApiResponse(responseCode = "400", description = "Invalid scope or strategy"), @ApiResponse(responseCode = "404", description = "Requested scope not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(ControllerSettings.API_COURSES_RECOMPUTE_WITH_STRATEGY_SUFFIX)
    ResponseEntity<CourseDuplicateRecomputeResultDto> recomputeCourseDuplicatesWithStrategy(
            @Parameter(description = "Course ID; required when questionBankId is omitted", required = false) @RequestParam(value = "courseId", required = false) Long courseId,
            @Parameter(description = "Duplicate detection strategy", required = true) @RequestParam("strategy") String strategy,
            @Parameter(description = "Question bank ID", required = false) @RequestParam(value = "questionBankId", required = false) Long questionBankId,
            @Parameter(description = "Author ID; used with questionBankId", required = false) @RequestParam(value = "authorId", required = false) Long authorId);

    @Operation(summary = "Get duplicate statistics", description = "Retrieve duplicate statistics for a course, question bank, or author scope")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicate statistics retrieved successfully"), @ApiResponse(responseCode = "400", description = "Invalid scope"), @ApiResponse(responseCode = "404", description = "Requested scope not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping(ControllerSettings.API_COURSES_DUPLICATE_STATISTICS_SUFFIX)
    ResponseEntity<DuplicateStatisticsDto> getDuplicateStatistics(
            @Parameter(description = "Course ID; required when questionBankId is omitted", required = false) @RequestParam(value = "courseId", required = false) Long courseId,
            @Parameter(description = "Question bank ID", required = false) @RequestParam(value = "questionBankId", required = false) Long questionBankId,
            @Parameter(description = "Author ID; used with questionBankId", required = false) @RequestParam(value = "authorId", required = false) Long authorId);

    @Operation(summary = "Clear duplicates", description = "Clear duplicate links for a course, question bank, or author scope")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicate links cleared successfully"), @ApiResponse(responseCode = "400", description = "Invalid scope"), @ApiResponse(responseCode = "404", description = "Requested scope not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(ControllerSettings.API_COURSES_CLEAR_DUPLICATES_SUFFIX)
    ResponseEntity<Integer> clearDuplicatesForCourse(
            @Parameter(description = "Course ID; required when questionBankId is omitted", required = false) @RequestParam(value = "courseId", required = false) Long courseId,
            @Parameter(description = "Question bank ID", required = false) @RequestParam(value = "questionBankId", required = false) Long questionBankId,
            @Parameter(description = "Author ID; used with questionBankId", required = false) @RequestParam(value = "authorId", required = false) Long authorId);

    @Operation(summary = "Get duplicate recompute history", description = "Returns all saved duplicate recompute history entries ordered by date descending")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "History retrieved successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping(ControllerSettings.API_COURSES_RECOMPUTE_HISTORY_SUFFIX)
    ResponseEntity<List<DuplicateRecomputeHistoryDto>> getRecomputeHistory();

    @Operation(summary = "Save duplicate recompute history", description = "Persist a completed duplicate recompute result as a history record")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "History entry saved successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping(ControllerSettings.API_COURSES_RECOMPUTE_HISTORY_SUFFIX)
    ResponseEntity<DuplicateRecomputeHistoryDto> saveRecomputeHistory(@Valid @RequestBody DuplicateRecomputeHistoryDto historyDto);

    @Operation(summary = "Delete duplicate recompute history", description = "Delete a duplicate recompute history entry by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "History entry deleted successfully"), @ApiResponse(responseCode = "404", description = "History entry not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @DeleteMapping(ControllerSettings.API_COURSES_RECOMPUTE_HISTORY_SUFFIX + "/{id}")
    ResponseEntity<Void> deleteRecomputeHistoryEntry(@Parameter(description = "History entry ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create default courses", description = "Create all default courses that do not already exist")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Default courses created successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/" + ControllerSettings.API_COURSES_CREATE_DEFAULTS_SUFFIX)
    ResponseEntity<Integer> createDefaultCourses();

    @Operation(summary = "Delete duplicate questions in course", description = "Start background deletion of duplicate questions in a course")
    @ApiResponses(value = {@ApiResponse(responseCode = "202", description = "Duplicate deletion started"), @ApiResponse(responseCode = "404", description = "Course not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/delete-duplicate-questions")
    ResponseEntity<String> deleteDuplicateQuestionsInCourse(@Parameter(description = "Course ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get duplicate deletion status", description = "Return the last background duplicate deletion error for a course")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Last error message returned"), @ApiResponse(responseCode = "204", description = "No error recorded")})
    @GetMapping("/{id}/delete-duplicate-questions/status")
    ResponseEntity<String> getDeleteDuplicateQuestionsStatus(@Parameter(description = "Course ID", required = true) @PathVariable Long id);
}
