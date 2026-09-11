package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.DuplicateRecomputeHistoryDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.api.interfaces.CourseApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.DuplicateDeletionTaskService;
import com.unitbv.myquiz.app.services.ExportService;
import jakarta.validation.Valid;
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
public class CourseController implements CourseApi {
    private static final Logger log = LoggerFactory.getLogger(CourseController.class);
    private final CourseService courseService;
    private final ExportService exportService;
    private final DuplicateDeletionTaskService duplicateDeletionTaskService;

    @Override
    public ResponseEntity<List<CourseDto>> getAllCourses() {
        log.info("Getting all courses");
        List<CourseDto> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @Override
    public ResponseEntity<Void> deleteCourseById(@PathVariable Long id) {
        log.info("Deleting course with id: {}", id);
        courseService.deleteCourseById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CourseDto> findById(@PathVariable Long id) {
        log.info("Finding course with id: {}", id);
        CourseDto course = courseService.findById(id);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(course);
    }

    @Override
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto courseDto) {
        log.info("Updating course with id: {}", id);
        if (courseDto != null) {
            courseService.updateCourse(id, courseDto);
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CourseDto courseDto) {
        if (courseDto == null) {
            log.warn("Attempted to create course with null CourseDto");
            return ResponseEntity.badRequest().build();
        }
        log.info("Creating new course: {}", courseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(courseDto));
    }

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
            .anyMatch(authority -> ControllerSettings.PERMISSION_EXPORT_XML.equals(authority.getAuthority()));
    }

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

    @Override
    public ResponseEntity<CourseDuplicateRecomputeResultDto> recomputeCourseDuplicatesWithStrategy(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam("strategy") String strategy,
            @RequestParam(value = "questionBankId", required = false) Long questionBankId,
            @RequestParam(value = "authorId", required = false) Long authorId) {
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

    @Override
    public ResponseEntity<DuplicateStatisticsDto> getDuplicateStatistics(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "questionBankId", required = false) Long questionBankId,
            @RequestParam(value = "authorId", required = false) Long authorId) {
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

    @Override
    public ResponseEntity<Integer> clearDuplicatesForCourse(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "questionBankId", required = false) Long questionBankId,
            @RequestParam(value = "authorId", required = false) Long authorId) {
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

    // ---- Recompute History endpoints ----

    @Override
    public ResponseEntity<List<DuplicateRecomputeHistoryDto>> getRecomputeHistory() {
        try {
            List<DuplicateRecomputeHistoryDto> history = courseService.getRecomputeHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.atError().setCause(e).log("Failed to retrieve recompute history");
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<DuplicateRecomputeHistoryDto> saveRecomputeHistory(
            @Valid @RequestBody DuplicateRecomputeHistoryDto historyDto) {
        if (historyDto == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            CourseDuplicateRecomputeResultDto result = buildResultFromHistoryDto(historyDto);
            DuplicateRecomputeHistoryDto saved = courseService.saveRecomputeHistory(
                    result,
                    historyDto.getStrategy(),
                    historyDto.getCourseId(),
                    historyDto.getQuestionBankId(),
                    historyDto.getAuthorId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.atError().setCause(e).log("Failed to save recompute history entry");
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteRecomputeHistoryEntry(@PathVariable Long id) {
        try {
            courseService.deleteRecomputeHistoryEntry(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(id).addArgument(e.getMessage())
                .log("Could not delete recompute history entry id='{}': {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id)
                .log("Failed to delete recompute history entry id='{}'");
            return ResponseEntity.internalServerError().build();
        }
    }

    private CourseDuplicateRecomputeResultDto buildResultFromHistoryDto(DuplicateRecomputeHistoryDto dto) {
        CourseDuplicateRecomputeResultDto result = new CourseDuplicateRecomputeResultDto();
        result.setCourseId(dto.getCourseId());
        result.setCourseName(dto.getCourseName());
        result.setTotalQuestions(dto.getTotalQuestions());
        result.setMultichoiceQuestions(dto.getMultichoiceQuestions());
        result.setTruefalseQuestions(dto.getTruefalseQuestions());
        result.setDuplicateLinksRemoved(dto.getDuplicateLinksRemoved());
        result.setDuplicateErrorsRemoved(dto.getDuplicateErrorsRemoved());
        result.setDuplicateErrorsCreated(dto.getDuplicateErrorsCreated());
        result.setStartedAt(dto.getStartedAt());
        result.setEndedAt(dto.getEndedAt());
        result.setDurationMs(dto.getDurationMs());
        return result;
    }

    @Override
    public ResponseEntity<Integer> createDefaultCourses() {
        try {
            int createdCount = courseService.createDefaultCourses();
            log.info("Created {} default courses", createdCount);
            return ResponseEntity.ok(createdCount);
        } catch (Exception e) {
            log.atError().setCause(e).log("Failed to create default courses");
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<String> deleteDuplicateQuestionsInCourse(@PathVariable Long id) {
        log.info("Starting background deletion of duplicate questions for course id: {}", id);
        try {
            duplicateDeletionTaskService.startDeleteDuplicateQuestionsInCourseAsync(id);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Duplicate questions deletion started for course id: " + id);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(id).addArgument(e.getMessage())
                .log("Could not start duplicate questions deletion for course {}: {}");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id)
                .log("Failed to start duplicate questions deletion for course {}");
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<String> getDeleteDuplicateQuestionsStatus(@PathVariable Long id) {
        String lastError = duplicateDeletionTaskService.getDeleteExactDuplicatesLastError(id);
        return lastError != null ? ResponseEntity.ok(lastError) : ResponseEntity.noContent().build();
    }
}
