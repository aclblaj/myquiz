package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.DuplicateRecomputeHistoryDto;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.DuplicateRecomputeHistory;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.DuplicateRecomputeHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Orchestrates long-running duplicate-deletion operations on background threads,
 * decoupled from {@link CourseService}'s course/duplicate domain logic.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Validate the target course synchronously so callers get an immediate error for unknown courses.</li>
 *     <li>Run the actual deletion on a background {@link Thread} and return without waiting for it to finish.</li>
 *     <li>Persist a {@link DuplicateRecomputeHistory} entry with statistics once the background task completes.</li>
 *     <li>Track background-thread failures in a concise, thread-safe field (since the HTTP response has
 *     already been sent by the time such a failure happens) via {@link #getDeleteExactDuplicatesLastError(Long)}.</li>
 * </ul>
 */
@Service
public class DuplicateDeletionTaskService {
    private static final Logger log = LoggerFactory.getLogger(DuplicateDeletionTaskService.class);
    private static final String MSG_COURSE_NOT_FOUND_WITH_ID = "Course not found with ID: ";
    private static final String STRATEGY_DELETE_EXACT_DUPLICATES = "DELETE_EXACT_DUPLICATES";

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository;
    private final Executor deleteExactDuplicatesTaskExecutor;

    // Self-reference to invoke this bean's own @Transactional methods from background threads through the Spring proxy
    private final DuplicateDeletionTaskService self;

    // Field scope: concise, thread-safe record of the last background "delete exact duplicates" error per course.
    // Populated only when the background thread fails, since the HTTP response has already returned by then.
    private final Map<Long, String> deleteExactDuplicatesLastError = new ConcurrentHashMap<>();

    @Autowired
    public DuplicateDeletionTaskService(CourseRepository courseRepository, CourseService courseService,
                                        DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository,
                                        @Qualifier("deleteExactDuplicatesTaskExecutor") Executor deleteExactDuplicatesTaskExecutor,
                                        @Lazy DuplicateDeletionTaskService self) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.duplicateRecomputeHistoryRepository = duplicateRecomputeHistoryRepository;
        this.deleteExactDuplicatesTaskExecutor = deleteExactDuplicatesTaskExecutor;
        this.self = self != null ? self : this;
    }

    private DuplicateDeletionTaskService getSelf() {
        return self != null ? self : this;
    }

    /**
     * Starts the "delete exact duplicate questions" operation for a course on a background thread.
     * Validates the course synchronously (so callers get an immediate 404 for unknown courses),
     * then returns without waiting for the deletion to finish.
     *
     * @param courseId The course ID
     */
    @Transactional(readOnly = true)
    public void startDeleteDuplicateQuestionsInCourseAsync(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + courseId));
        String courseName = course.getCourse();

        // Clear any stale error from a previous run before starting a new one
        deleteExactDuplicatesLastError.remove(courseId);

        try {
            deleteExactDuplicatesTaskExecutor.execute(() -> runDeleteExactDuplicatesInBackground(courseId, courseName));
            log.atInfo().addArgument(courseId).addArgument(courseName)
                    .log("Queued background delete-exact-duplicates task for course id={} ('{}')");
        } catch (RejectedExecutionException e) {
            deleteExactDuplicatesLastError.put(courseId,
                    "Delete-exact-duplicates task executor saturated, please retry later");
            log.atWarn().setCause(e).addArgument(courseId)
                    .log("Delete-exact-duplicates task executor saturated for course id={}");
            throw e;
        }
    }

    private void runDeleteExactDuplicatesInBackground(Long courseId, String courseName) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();
        try {
            int totalQuestionsBefore = courseService.loadScopedQuestionsCount(courseName);
            int deletedCount = courseService.deleteDuplicateQuestionsInCourse(courseId);

            long endedMs = System.currentTimeMillis();
            OffsetDateTime endedAt = OffsetDateTime.now();

            getSelf().saveDeleteExactDuplicatesHistory(courseId, courseName, totalQuestionsBefore, deletedCount,
                    startedAt, endedAt, endedMs - startedMs);

            log.atInfo().addArgument(deletedCount).addArgument(courseName)
                    .log("Background delete-exact-duplicates task finished: deleted {} duplicate question(s) from course '{}'");
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            deleteExactDuplicatesLastError.put(courseId, message);
            log.atError().setCause(e).addArgument(courseId).addArgument(message)
                    .log("Background delete-exact-duplicates task failed for course id={}: {}");
        }
    }

    /**
     * Returns the last recorded error message from a background "delete exact duplicates" run
     * for the given course, or {@code null} if the last run (if any) succeeded.
     */
    public String getDeleteExactDuplicatesLastError(Long courseId) {
        return courseId != null ? deleteExactDuplicatesLastError.get(courseId) : null;
    }

    @Transactional
    public DuplicateRecomputeHistoryDto saveDeleteExactDuplicatesHistory(Long courseId, String courseName, int totalQuestions,
                                                                          int deletedCount, OffsetDateTime startedAt,
                                                                          OffsetDateTime endedAt, long durationMs) {
        DuplicateRecomputeHistory entity = new DuplicateRecomputeHistory();
        entity.setCourseId(courseId);
        entity.setCourseName(courseName);
        entity.setStrategy(STRATEGY_DELETE_EXACT_DUPLICATES);
        entity.setTotalQuestions(totalQuestions);
        // duplicateLinksRemoved is reused here to hold the number of deleted duplicate questions,
        // since this strategy removes whole questions rather than duplicate links.
        entity.setDuplicateLinksRemoved(deletedCount);
        entity.setStartedAt(startedAt);
        entity.setEndedAt(endedAt);
        entity.setDurationMs(durationMs);
        DuplicateRecomputeHistory saved = duplicateRecomputeHistoryRepository.save(entity);
        log.atInfo().addArgument(saved.getId()).addArgument(courseName)
                .log("Saved delete-exact-duplicates history entry id={} for course '{}'");
        return mapHistoryToDto(saved);
    }

    private DuplicateRecomputeHistoryDto mapHistoryToDto(DuplicateRecomputeHistory entity) {
        DuplicateRecomputeHistoryDto dto = new DuplicateRecomputeHistoryDto();
        dto.setId(entity.getId());
        dto.setCourseId(entity.getCourseId());
        dto.setCourseName(entity.getCourseName());
        dto.setQuestionBankId(entity.getQuestionBankId());
        dto.setAuthorId(entity.getAuthorId());
        dto.setStrategy(entity.getStrategy());
        dto.setTotalQuestions(entity.getTotalQuestions());
        dto.setMultichoiceQuestions(entity.getMultichoiceQuestions());
        dto.setTruefalseQuestions(entity.getTruefalseQuestions());
        dto.setDuplicateLinksRemoved(entity.getDuplicateLinksRemoved());
        dto.setDuplicateErrorsRemoved(entity.getDuplicateErrorsRemoved());
        dto.setDuplicateErrorsCreated(entity.getDuplicateErrorsCreated());
        dto.setStartedAt(entity.getStartedAt());
        dto.setEndedAt(entity.getEndedAt());
        dto.setDurationMs(entity.getDurationMs());
        dto.setSavedAt(entity.getSavedAt());
        return dto;
    }
}
