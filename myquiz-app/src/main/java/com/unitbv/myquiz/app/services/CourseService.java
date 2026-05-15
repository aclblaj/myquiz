package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.CourseSourceDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.app.entities.ArchiveImport;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.mapper.CourseMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.CourseRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final AuthorRepository authorRepository;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionBankRepository questionBankRepository;
    private final CourseMapper courseMapper;
    private final QuestionDuplicationService questionDuplicationService;
    private final QuestionDuplicateRepository questionDuplicateRepository;
    private final QuestionErrorRepository questionErrorRepository;

    @Autowired
    public CourseService(CourseRepository courseRepository, QuestionRepository questionRepository, AuthorRepository authorRepository, QuestionBankAuthorRepository questionBankAuthorRepository,
                         QuestionBankRepository questionBankRepository, CourseMapper courseMapper, QuestionDuplicationService questionDuplicationService, QuestionDuplicateRepository questionDuplicateRepository,
                         QuestionErrorRepository questionErrorRepository) {
        this.courseRepository = courseRepository;
        this.questionRepository = questionRepository;
        this.authorRepository = authorRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionBankRepository = questionBankRepository;
        this.courseMapper = courseMapper;
        this.questionDuplicationService = questionDuplicationService;
        this.questionDuplicateRepository = questionDuplicateRepository;
        this.questionErrorRepository = questionErrorRepository;
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

        courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

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
            throw new RuntimeException("Failed to delete course: " + selectedCourse, e);
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

        Course course = courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

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

        Course course = courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

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

        return createCourse(courseDto);
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

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));

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

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));

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
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
        return getDuplicateStatistics(course.getCourse());
    }

    @Transactional(readOnly = true)
    public DuplicateStatisticsDto getDuplicateStatistics(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        List<Question> allCourseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(courseName, null, null, null));
        long questionsWithDuplicateErrors = allCourseQuestions.stream()
                .filter(q -> q.getQuestionErrors() != null && q.getQuestionErrors().stream()
                        .anyMatch(e -> isDuplicateError(e.getDescription())))
                .count();

        List<Long> questionIds = allCourseQuestions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .toList();

        long duplicateLinks = 0;
        if (!questionIds.isEmpty()) {
            duplicateLinks = questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds).size();
        }

        return new DuplicateStatisticsDto(
                courseName,
                allCourseQuestions.size(),
                (int) questionsWithDuplicateErrors,
                duplicateLinks
        );
    }

    private boolean isDuplicateError(String description) {
        if (description == null) {
            return false;
        }
        return description.startsWith("Reformulate question - Title already exists") ||
                description.startsWith("Reformulate question - Answer already exists");
    }

    @Transactional
    public int clearDuplicatesForCourse(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
        return clearDuplicatesForCourse(course.getCourse());
    }

    @Transactional
    public int clearDuplicatesForCourse(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        List<Question> allCourseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(courseName, null, null, null));
        List<Long> questionIds = allCourseQuestions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .toList();

        if (questionIds.isEmpty()) {
            return 0;
        }

        long deletedDuplicates = questionDuplicateRepository.deleteByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds);
        long deletedTitleErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
        long deletedAnswerErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
        log.atInfo().addArgument(deletedDuplicates).addArgument(deletedTitleErrors + deletedAnswerErrors).addArgument(courseName)
                .log("Cleared {} duplicate links and {} duplicate errors for course '{}'");

        return (int) deletedDuplicates;
    }

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForCourse(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();

        QuestionDuplicationService.DuplicateRecomputeSummary summary = questionDuplicationService.recomputeDuplicatesForCourse(courseName);

        long endedMs = System.currentTimeMillis();
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now();

        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseName(courseName);
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
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForCourse(String courseName, String strategy) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();

        QuestionDuplicationService.DuplicateRecomputeSummary summary = questionDuplicationService.recomputeDuplicatesForCourse(courseName, strategy);

        long endedMs = System.currentTimeMillis();
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now();

        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseName(courseName);
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

    // ---- Question Bank scoped operations ----

    @Transactional
    public CourseDuplicateRecomputeResultDto recomputeDuplicatesForQuestionBank(Long questionBankId, String strategy) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        QuestionBank qb = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found with ID: " + questionBankId));
        String courseName = qb.getCourseName();

        List<Question> qbQuestions = questionRepository.findAll(QuestionSpecification.byFilters(null, null, questionBankId, null));

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();

        QuestionDuplicationService.DuplicateRecomputeSummary summary =
                questionDuplicationService.recomputeDuplicatesForQuestionList(courseName, qbQuestions, strategy);

        long endedMs = System.currentTimeMillis();
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now();

        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseName(courseName);
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
    public DuplicateStatisticsDto getDuplicateStatisticsForQuestionBank(Long questionBankId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        QuestionBank qb = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found with ID: " + questionBankId));

        List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(null, null, questionBankId, null));
        long questionsWithDuplicateErrors = questions.stream()
                .filter(q -> q.getQuestionErrors() != null && q.getQuestionErrors().stream()
                        .anyMatch(e -> isDuplicateError(e.getDescription())))
                .count();

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .toList();

        long duplicateLinks = 0;
        if (!questionIds.isEmpty()) {
            duplicateLinks = questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds).size();
        }

        DuplicateStatisticsDto dto = new DuplicateStatisticsDto(
                qb.getCourseName(),
                questions.size(),
                (int) questionsWithDuplicateErrors,
                duplicateLinks
        );
        return dto;
    }

    @Transactional
    public int clearDuplicatesForQuestionBank(Long questionBankId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(null, null, questionBankId, null));
        List<Long> questionIds = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();

        if (questionIds.isEmpty()) {
            return 0;
        }

        long deleted = questionDuplicateRepository.deleteByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds);
        long deletedTitleErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
        long deletedAnswerErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
        log.atInfo().addArgument(deleted).addArgument(deletedTitleErrors + deletedAnswerErrors).addArgument(questionBankId)
                .log("Cleared {} duplicate links and {} duplicate errors for questionBank '{}'");
        return (int) deleted;
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
        QuestionBank qb = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found with ID: " + questionBankId));
        String courseName = qb.getCourseName();

        List<Question> authorQuestions = questionRepository.findAll(QuestionSpecification.byFilters(courseName, authorId, questionBankId, null));

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
        long startedMs = System.currentTimeMillis();

        QuestionDuplicationService.DuplicateRecomputeSummary summary =
                questionDuplicationService.recomputeDuplicatesForQuestionList(courseName, authorQuestions, strategy);

        long endedMs = System.currentTimeMillis();
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now();

        CourseDuplicateRecomputeResultDto dto = new CourseDuplicateRecomputeResultDto();
        dto.setCourseName(courseName);
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
    public DuplicateStatisticsDto getDuplicateStatisticsForAuthor(Long questionBankId, Long authorId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }
        QuestionBank qb = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found with ID: " + questionBankId));

        List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(qb.getCourseName(), authorId, questionBankId, null));
        long questionsWithDuplicateErrors = questions.stream()
                .filter(q -> q.getQuestionErrors() != null && q.getQuestionErrors().stream()
                        .anyMatch(e -> isDuplicateError(e.getDescription())))
                .count();

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .toList();

        long duplicateLinks = 0;
        if (!questionIds.isEmpty()) {
            duplicateLinks = questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds).size();
        }

        return new DuplicateStatisticsDto(
                qb.getCourseName(),
                questions.size(),
                (int) questionsWithDuplicateErrors,
                duplicateLinks
        );
    }

    @Transactional
    public int clearDuplicatesForAuthor(Long questionBankId, Long authorId) {
        if (questionBankId == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }
        QuestionBank qb = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found with ID: " + questionBankId));

        List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(qb.getCourseName(), authorId, questionBankId, null));
        List<Long> questionIds = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();

        if (questionIds.isEmpty()) {
            return 0;
        }

        long deleted = questionDuplicateRepository.deleteByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds);
        long deletedTitleErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
        long deletedAnswerErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(questionIds, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
        log.atInfo().addArgument(deleted).addArgument(deletedTitleErrors + deletedAnswerErrors).addArgument(authorId).addArgument(questionBankId)
                .log("Cleared {} duplicate links and {} duplicate errors for author '{}' in questionBank '{}'");
        return (int) deleted;
    }
}
