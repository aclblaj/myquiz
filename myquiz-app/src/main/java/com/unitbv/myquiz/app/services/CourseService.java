package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.CourseSourceDto;
import com.unitbv.myquiz.api.dto.DuplicateRecomputeHistoryDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.api.types.DefaultCourse;
import com.unitbv.myquiz.app.entities.ArchiveImport;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.DuplicateRecomputeHistory;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.mapper.CourseMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.DuplicateRecomputeHistoryRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.CourseSpecification;
import com.unitbv.myquiz.app.specifications.QuestionBankAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuestionBankSpecification;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);
    private static final String MSG_COURSE_NOT_FOUND_WITH_ID = "Course not found with ID: ";

    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final AuthorRepository authorRepository;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionBankRepository questionBankRepository;
    private final CourseMapper courseMapper;
    private final QuestionDuplicationService questionDuplicationService;
    private final QuestionDuplicateRepository questionDuplicateRepository;
    private final QuestionErrorRepository questionErrorRepository;
    private final DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository;

    @Autowired
    public CourseService(CourseRepository courseRepository, QuestionRepository questionRepository, AuthorRepository authorRepository, QuestionBankAuthorRepository questionBankAuthorRepository,
                         QuestionBankRepository questionBankRepository, CourseMapper courseMapper, QuestionDuplicationService questionDuplicationService, QuestionDuplicateRepository questionDuplicateRepository,
                         QuestionErrorRepository questionErrorRepository, DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository) {
        this.courseRepository = courseRepository;
        this.questionRepository = questionRepository;
        this.authorRepository = authorRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionBankRepository = questionBankRepository;
        this.courseMapper = courseMapper;
        this.questionDuplicationService = questionDuplicationService;
        this.questionDuplicateRepository = questionDuplicateRepository;
        this.questionErrorRepository = questionErrorRepository;
        this.duplicateRecomputeHistoryRepository = duplicateRecomputeHistoryRepository;
    }


    public List<CourseDto> getAllCourses() {
        List<CourseDto> courseDtos = courseRepository.findAll().stream()
                .map(course -> {
                    CourseDto dto = courseMapper.toDto(course);
                    long count = questionBankRepository.count(QuestionBankSpecification.byCourseId(course.getId()));
                    dto.setQuestionBankCount((int) count);
                    return dto;
                })
                .toList();
        log.atInfo().addArgument(courseDtos.size()).log("Found {} courses");
        return courseDtos;
    }


    @Transactional
    @CacheEvict(value = "courseNames", allEntries = true)
    public void deleteCourseById(Long id) {
        // Input validation
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + id));

        List<QuestionBank> questionBanks = questionBankRepository.findAll(QuestionBankSpecification.byCourseId(id));
        log.atInfo().addArgument(questionBanks.size()).addArgument(id).log("Deleting {} questionBanks for course id={}");
        for (QuestionBank questionBank : questionBanks) {
            deleteQuestionBankData(questionBank.getId());
        }

        courseRepository.deleteById(id);
        log.atInfo().addArgument(id).log("Deleted course with ID: {}");
    }


    @Transactional
    @CacheEvict(value = "courseNames", allEntries = true)
    public void deleteCourse(String selectedCourse) {
        // Input validation
        if (selectedCourse == null || selectedCourse.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        try {
            Specification<QuestionBank> questionBankSpecification = QuestionBankSpecification.byCourse(selectedCourse);
            List<QuestionBank> questionBanks = questionBankRepository.findAll(questionBankSpecification);

            if (questionBanks.isEmpty()) {
                log.atWarn().addArgument(selectedCourse).log("No questionBanks found for course {}");
            } else {
                log.atInfo().addArgument(questionBanks.size()).addArgument(selectedCourse).log("Deleting {} questionBanks for course {}");

                for (QuestionBank questionBank : questionBanks) {
                    deleteQuestionBankData(questionBank.getId());
                }
            }

            // Delete course entity
            Specification<Course> courseSpec = CourseSpecification.byCourseName(selectedCourse);
            List<Course> courses = courseRepository.findAll(courseSpec);
            if (!courses.isEmpty()) {
                courseRepository.deleteAll(courses);
                log.atInfo().addArgument(selectedCourse).log("Deleted course: {}");
            } else {
                log.atWarn().addArgument(selectedCourse).log("Course entity {} not found");
            }
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(selectedCourse).log("Error deleting course: {}");
            throw new CourseDeletionException("Failed to delete course: " + selectedCourse, e);
        }
    }

    /**
     * Helper method to delete all data associated with a QuestionBank.
     * Extracts complex deletion logic from deleteCourse.
     */
    private void deleteQuestionBankData(Long questionBankId) {
        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(
                QuestionBankAuthorSpecification.hasQuestionBankId(questionBankId).and(QuestionBankAuthorSpecification.fetchAuthor()));

        for (QuestionBankAuthor questionBankAuthor : questionBankAuthors) {
            Long questionBankAuthorId = questionBankAuthor.getId();

            // Delete questions
            Specification<Question> questionSpec = QuestionSpecification.byQuestionBankAuthorId(questionBankAuthorId);
            List<Question> questions = questionRepository.findAll(questionSpec);
            for (Question question : questions) {
                questionDuplicationService.removeAllDuplicateAssociationsForQuestion(question.getId());
            }
            questionRepository.deleteAll(questions);

            // Delete orphaned authors
            Author author = questionBankAuthor.getAuthor();
            if (author != null) {
                long countAuthor = questionBankAuthorRepository.count(QuestionBankAuthorSpecification.hasAuthor(author));
                if (countAuthor == 1) {
                    authorRepository.delete(author);
                }
            }

            questionBankAuthorRepository.delete(questionBankAuthor);
        }

        questionBankRepository.deleteById(questionBankId);
    }


    @Transactional(readOnly = true)
    public CourseDto findById(Long id) {
        // Input validation
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + id));

        CourseDto dto = courseMapper.toDto(course);
        dto.setSources(extractCourseSources(course));
        return dto;
    }

    private List<CourseSourceDto> extractCourseSources(Course course) {
        List<CourseSourceDto> sources = new ArrayList<>();
        if (course.getQuestionBanks() == null || course.getQuestionBanks().isEmpty()) {
            return sources;
        }

        for (QuestionBank questionBank : course.getQuestionBanks()) {
            // Query database directly for counts to avoid lazy loading issues
            int mcQuestionsCount = (int) questionRepository.count(
                    QuestionSpecification.byQuestionBankId(questionBank.getId())
                            .and(QuestionSpecification.byQuestionType(com.unitbv.myquiz.api.types.QuestionType.MULTICHOICE))
            );
            int tfQuestionsCount = (int) questionRepository.count(
                    QuestionSpecification.byQuestionBankId(questionBank.getId())
                            .and(QuestionSpecification.byQuestionType(com.unitbv.myquiz.api.types.QuestionType.TRUEFALSE))
            );
            int totalQuestionsCount = mcQuestionsCount + tfQuestionsCount;

            // Query database for authors count to avoid lazy loading issues
            int authorsCount = (int) questionBankAuthorRepository.count(
                    QuestionBankAuthorSpecification.hasQuestionBankId(questionBank.getId())
            );

            if (questionBank.getArchiveImports() == null || questionBank.getArchiveImports().isEmpty()) {
                sources.add(new CourseSourceDto(
                        questionBank.getId(),
                        questionBank.getName(),
                        mcQuestionsCount,
                        tfQuestionsCount,
                        totalQuestionsCount,
                        authorsCount,
                        "-"
                ));
                continue;
            }

            for (ArchiveImport archiveImport : questionBank.getArchiveImports()) {
                sources.add(new CourseSourceDto(
                        questionBank.getId(),
                        questionBank.getName(),
                        mcQuestionsCount,
                        tfQuestionsCount,
                        totalQuestionsCount,
                        authorsCount,
                        archiveImport.getFileName()
                ));
            }
        }
        return sources;
    }


    @Transactional
    @CacheEvict(value = "courseNames", allEntries = true)
    public void updateCourse(Long id, CourseDto courseDto) {
        // Input validation
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (courseDto == null) {
            throw new IllegalArgumentException("CourseDto cannot be null");
        }

        Course course = courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + id));

        course.setCourse(courseDto.getCourse());
        course.setDescription(courseDto.getDescription());
        course.setUniversityYear(courseDto.getUniversityYear());
        course.setSemester(courseDto.getSemester());
        courseRepository.save(course);

        log.atInfo().addArgument(id).log("Course {} updated");
    }


    @Transactional
    @CacheEvict(value = "courseNames", allEntries = true)
    public CourseDto createCourse(CourseDto courseDto) {
        // Input validation
        if (courseDto == null) {
            throw new IllegalArgumentException("CourseDto cannot be null");
        }
        if (courseDto.getCourse() == null || courseDto.getCourse().isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        Specification<Course> spec = CourseSpecification.byCourseName(courseDto.getCourse());
        List<Course> found = courseRepository.findAll(spec);

        if (!found.isEmpty()) {
            log.atWarn().addArgument(courseDto.getCourse()).log("Course {} already exists, returning existing");
            return courseMapper.toDto(found.getFirst());
        }

        log.atInfo().addArgument(courseDto.getCourse()).log("Creating new course: {}");

        Course course = courseMapper.toEntity(courseDto);
        courseRepository.save(course);

        log.atInfo().addArgument(course.getId()).log("Course created with ID: {}");

        return courseMapper.toDto(course);
    }


    public CourseDto createCourseIfNotExists(CourseDto courseDto) {
        // Input validation
        if (courseDto == null) {
            throw new IllegalArgumentException("CourseDto cannot be null");
        }
        if (courseDto.getCourse() == null || courseDto.getCourse().isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        Specification<Course> spec = CourseSpecification.byCourseName(courseDto.getCourse());
        List<Course> found = courseRepository.findAll(spec);

        if (!found.isEmpty()) {
            Course course = found.getFirst();
            log.atInfo().addArgument(course.getId()).log("Course already exists with ID: {}");
            return courseMapper.toDto(course);
        }

        return createCourseInternal(courseDto);
    }


    @Cacheable("courseNames")
    public List<String> getAllCourseNames() {
        return courseRepository.findAll().stream().map(Course::getCourse).toList();
    }


    public String getCourseName(Long courseId) {
        // Input validation
        if (courseId == null) {
            log.atWarn().log("Course ID is null");
            return "";
        }

        return courseRepository.findById(courseId).map(Course::getCourse).orElse("");
    }

    @Transactional
    @CacheEvict(value = "courseNames", allEntries = true)
    public Course getOrCreateCourseEntity(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        return courseRepository.findByCourseIgnoreCase(courseName).orElseGet(() -> {
            Course course = new Course();
            course.setCourse(courseName);
            return courseRepository.save(course);
        });
    }

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForCourse(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + courseId));

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();

        QuestionDuplicationService.DuplicateRecomputeSummary summary = questionDuplicationService.recomputeDuplicatesForCourse(course.getCourse());

        long endedMs = System.currentTimeMillis();
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now();

        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getCourse());
        dto.setStartedAt(startedAt);
        dto.setEndedAt(endedAt);
        dto.setDurationMs(endedMs - startedMs);
        dto.setTotalQuestions(summary.totalQuestions());
        dto.setMultichoiceQuestions(summary.multichoiceQuestions());
        dto.setTruefalseQuestions(summary.truefalseQuestions());
        dto.setDuplicateLinksRemoved(summary.duplicateLinksRemoved());
        dto.setDuplicateErrorsRemoved(summary.duplicateErrorsRemoved());
        dto.setDuplicateErrorsCreated(summary.duplicateErrorsCreated());
        return dto;
    }

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForCourseWithStrategy(Long courseId, String strategy) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + courseId));

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();

        QuestionDuplicationService.DuplicateRecomputeSummary summary = questionDuplicationService.recomputeDuplicatesForCourse(course.getCourse(), strategy);

        long endedMs = System.currentTimeMillis();
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now();

        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getCourse());
        dto.setStartedAt(startedAt);
        dto.setEndedAt(endedAt);
        dto.setDurationMs(endedMs - startedMs);
        dto.setTotalQuestions(summary.totalQuestions());
        dto.setMultichoiceQuestions(summary.multichoiceQuestions());
        dto.setTruefalseQuestions(summary.truefalseQuestions());
        dto.setDuplicateLinksRemoved(summary.duplicateLinksRemoved());
        dto.setDuplicateErrorsRemoved(summary.duplicateErrorsRemoved());
        dto.setDuplicateErrorsCreated(summary.duplicateErrorsCreated());
        return dto;
    }

    @Transactional(readOnly = true)
    public DuplicateStatisticsDto getDuplicateStatistics(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + courseId));
        return buildDuplicateStatistics(course.getCourse(), loadScopedQuestions(course.getCourse(), null, null));
    }

    @Transactional(readOnly = true)
    public DuplicateStatisticsDto getDuplicateStatistics(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        return buildDuplicateStatistics(courseName, loadScopedQuestions(courseName, null, null));
    }

    private boolean isDuplicateError(String description) {
        return MyUtil.isDuplicateValidationError(description)
                || (description != null && (
                description.startsWith("Reformulate question - Title already exists") ||
                description.startsWith("Reformulate question - Answer already exists")
        ));
    }

    @Transactional
    public int clearDuplicatesForCourse(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_COURSE_NOT_FOUND_WITH_ID + courseId));
        DuplicateClearSummary summary = clearDuplicatesForQuestionIds(extractQuestionIds(loadScopedQuestions(course.getCourse(), null, null)));
        log.atInfo().addArgument(summary.duplicateLinksRemoved()).addArgument(summary.duplicateErrorsRemoved()).addArgument(course.getCourse())
                .log("Cleared {} duplicate links and {} duplicate errors for course '{}'");
        return summary.duplicateLinksRemoved();
    }

    @Transactional
    public int clearDuplicatesForCourse(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        DuplicateClearSummary summary = clearDuplicatesForQuestionIds(extractQuestionIds(loadScopedQuestions(courseName, null, null)));
        log.atInfo().addArgument(summary.duplicateLinksRemoved()).addArgument(summary.duplicateErrorsRemoved()).addArgument(courseName)
                .log("Cleared {} duplicate links and {} duplicate errors for course '{}'");

        return summary.duplicateLinksRemoved();
    }

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForCourse(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        return executeRecompute(courseName, null,
                () -> questionDuplicationService.recomputeDuplicatesForCourse(courseName));
    }

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForCourse(String courseName, String strategy) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        return executeRecompute(courseName, null,
                () -> questionDuplicationService.recomputeDuplicatesForCourse(courseName, strategy));
    }

    // ---- Question Bank scoped operations ----

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForQuestionBank(Long questionBankId, String strategy) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        QuestionBank qb = getQuestionBankOrThrow(questionBankId);
        String courseName = qb.getCourseName();
        List<Question> qbQuestions = loadScopedQuestions(null, null, questionBankId);

        return executeRecompute(courseName, qb.getCourse() != null ? qb.getCourse().getId() : null,
                () -> questionDuplicationService.recomputeDuplicatesForQuestionList(courseName, qbQuestions, strategy));
    }

    @Transactional(readOnly = true)
    public DuplicateStatisticsDto getDuplicateStatisticsForQuestionBank(Long questionBankId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        QuestionBank qb = getQuestionBankOrThrow(questionBankId);
        return buildDuplicateStatistics(qb.getCourseName(), loadScopedQuestions(null, null, questionBankId));
    }

    @Transactional
    public int clearDuplicatesForQuestionBank(Long questionBankId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        DuplicateClearSummary summary = clearDuplicatesForQuestionIds(extractQuestionIds(loadScopedQuestions(null, null, questionBankId)));
        log.atInfo().addArgument(summary.duplicateLinksRemoved()).addArgument(summary.duplicateErrorsRemoved()).addArgument(questionBankId)
                .log("Cleared {} duplicate links and {} duplicate errors for questionBank '{}'");
        return summary.duplicateLinksRemoved();
    }

    // ---- Author scoped operations (within a question bank) ----

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForAuthor(Long questionBankId, Long authorId, String strategy) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }
        QuestionBank qb = getQuestionBankOrThrow(questionBankId);
        String courseName = qb.getCourseName();
        List<Question> authorQuestions = loadScopedQuestions(courseName, authorId, questionBankId);

        return executeRecompute(courseName, qb.getCourse() != null ? qb.getCourse().getId() : null,
                () -> questionDuplicationService.recomputeDuplicatesForQuestionList(courseName, authorQuestions, strategy));
    }

    @Transactional(readOnly = true)
    public DuplicateStatisticsDto getDuplicateStatisticsForAuthor(Long questionBankId, Long authorId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }
        QuestionBank qb = getQuestionBankOrThrow(questionBankId);
        return buildDuplicateStatistics(qb.getCourseName(), loadScopedQuestions(qb.getCourseName(), authorId, questionBankId));
    }

    @Transactional
    public int clearDuplicatesForAuthor(Long questionBankId, Long authorId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }
        QuestionBank qb = getQuestionBankOrThrow(questionBankId);
        DuplicateClearSummary summary = clearDuplicatesForQuestionIds(extractQuestionIds(loadScopedQuestions(qb.getCourseName(), authorId, questionBankId)));
        log.atInfo().addArgument(summary.duplicateLinksRemoved()).addArgument(summary.duplicateErrorsRemoved()).addArgument(authorId).addArgument(questionBankId)
                .log("Cleared {} duplicate links and {} duplicate errors for author '{}' in questionBank '{}'");
        return summary.duplicateLinksRemoved();
    }

    private QuestionBank getQuestionBankOrThrow(Long questionBankId) {
        return questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found with ID: " + questionBankId));
    }

    private List<Question> loadScopedQuestions(String courseName, Long authorId, Long questionBankId) {
        return questionRepository.findAll(QuestionSpecification.byFilters(courseName, authorId, questionBankId, null));
    }

    private List<Long> extractQuestionIds(List<Question> questions) {
        return questions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private long countQuestionsWithDuplicateErrors(List<Question> questions) {
        return questions.stream()
                .filter(q -> q.getQuestionErrors() != null && q.getQuestionErrors().stream()
                        .anyMatch(e -> isDuplicateError(e.getDescription())))
                .count();
    }

    private long countDuplicateLinks(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return 0;
        }
        return questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds).size();
    }

    private DuplicateStatisticsDto buildDuplicateStatistics(String courseName, List<Question> questions) {
        List<Long> questionIds = extractQuestionIds(questions);
        return new DuplicateStatisticsDto(
                courseName,
                questions.size(),
                (int) countQuestionsWithDuplicateErrors(questions),
                countDuplicateLinks(questionIds)
        );
    }

    private CourseDto createCourseInternal(CourseDto courseDto) {
        log.atInfo().addArgument(courseDto.getCourse()).log("Creating new course: {}");

        Course course = courseMapper.toEntity(courseDto);
        courseRepository.save(course);

        log.atInfo().addArgument(course.getId()).log("Course created with ID: {}");
        return courseMapper.toDto(course);
    }

    private DuplicateClearSummary clearDuplicatesForQuestionIds(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return new DuplicateClearSummary(0, 0);
        }

        int deletedDuplicates = (int) questionDuplicateRepository.deleteByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds);
        int deletedErrors = Math.toIntExact(
                questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS)
                        + questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS)
        );
        return new DuplicateClearSummary(deletedDuplicates, deletedErrors);
    }

    private CourseDuplicateRecomputeResultDto executeRecompute(
            String courseName,
            Long courseId,
            Supplier<QuestionDuplicationService.DuplicateRecomputeSummary> recomputeAction
    ) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();
        QuestionDuplicationService.DuplicateRecomputeSummary summary = recomputeAction.get();
        long durationMs = System.currentTimeMillis() - startedMs;
        OffsetDateTime endedAt = OffsetDateTime.now();
        return buildRecomputeResult(courseName, courseId, startedAt, endedAt, durationMs, summary);
    }

    private CourseDuplicateRecomputeResultDto buildRecomputeResult(
            String courseName,
            Long courseId,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            long durationMs,
            QuestionDuplicationService.DuplicateRecomputeSummary summary
    ) {
        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseId(courseId);
        dto.setCourseName(courseName);
        dto.setStartedAt(startedAt);
        dto.setEndedAt(endedAt);
        dto.setDurationMs(durationMs);
        dto.setTotalQuestions(summary.totalQuestions());
        dto.setMultichoiceQuestions(summary.multichoiceQuestions());
        dto.setTruefalseQuestions(summary.truefalseQuestions());
        dto.setDuplicateLinksRemoved(summary.duplicateLinksRemoved());
        dto.setDuplicateErrorsRemoved(summary.duplicateErrorsRemoved());
        dto.setDuplicateErrorsCreated(summary.duplicateErrorsCreated());
        return dto;
    }

    private record DuplicateClearSummary(int duplicateLinksRemoved, int duplicateErrorsRemoved) {
    }

    private static class CourseDeletionException extends RuntimeException {
        CourseDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ---- Recompute History ----

    /**
     * Saves a duplicate recompute result as a history entry.
     */
    @Transactional
    public DuplicateRecomputeHistoryDto saveRecomputeHistory(CourseDuplicateRecomputeResultDto result,
                                                              String strategy,
                                                              Long courseId,
                                                              Long questionBankId,
                                                              Long authorId) {
        DuplicateRecomputeHistory entity = new DuplicateRecomputeHistory();
        entity.setCourseId(result.getCourseId() != null ? result.getCourseId() : courseId);
        entity.setCourseName(result.getCourseName());
        entity.setQuestionBankId(questionBankId);
        entity.setAuthorId(authorId);
        entity.setStrategy(strategy);
        entity.setTotalQuestions(result.getTotalQuestions());
        entity.setMultichoiceQuestions(result.getMultichoiceQuestions());
        entity.setTruefalseQuestions(result.getTruefalseQuestions());
        entity.setDuplicateLinksRemoved(result.getDuplicateLinksRemoved());
        entity.setDuplicateErrorsRemoved(result.getDuplicateErrorsRemoved());
        entity.setDuplicateErrorsCreated(result.getDuplicateErrorsCreated());
        entity.setStartedAt(result.getStartedAt());
        entity.setEndedAt(result.getEndedAt());
        entity.setDurationMs(result.getDurationMs());
        DuplicateRecomputeHistory saved = duplicateRecomputeHistoryRepository.save(entity);
        log.atInfo().addArgument(saved.getId()).addArgument(saved.getCourseName()).log("Saved recompute history entry id={} for course '{}'");
        return mapHistoryToDto(saved);
    }

    /**
     * Returns all recompute history entries ordered by savedAt descending.
     */
    @Transactional(readOnly = true)
    public List<DuplicateRecomputeHistoryDto> getRecomputeHistory() {
        return duplicateRecomputeHistoryRepository.findAllByOrderBySavedAtDesc()
                .stream().map(this::mapHistoryToDto).toList();
    }

    /**
     * Deletes a recompute history entry by ID.
     */
    @Transactional
    public void deleteRecomputeHistoryEntry(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("History entry ID cannot be null");
        }
        if (!duplicateRecomputeHistoryRepository.existsById(id)) {
            throw new IllegalArgumentException("History entry not found with ID: " + id);
        }
        duplicateRecomputeHistoryRepository.deleteById(id);
        log.atInfo().addArgument(id).log("Deleted recompute history entry id={}");
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

    @Transactional
    @CacheEvict(value = "courseNames", allEntries = true)
    public int createDefaultCourses() {
        int createdCount = 0;
        for (DefaultCourse defaultCourse : DefaultCourse.getAllCourses()) {
            CourseDto courseDto = new CourseDto();
            courseDto.setCourse(defaultCourse.getCourseName());
            if (courseDto.getCourse() != null && !courseDto.getCourse().isBlank()) {
                Specification<Course> spec = CourseSpecification.byCourseName(defaultCourse.getCourseName());
                List<Course> found = courseRepository.findAll(spec);
                if (found.isEmpty()) {
                    CourseDto created = createCourseInternal(courseDto);
                    if (created != null) {
                        createdCount++;
                    }
                }
            }
        }
        log.atInfo().addArgument(createdCount).log("Created {} default courses");
        return createdCount;
    }

}
