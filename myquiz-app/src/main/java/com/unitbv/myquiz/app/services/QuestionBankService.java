package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportAuthorSectionDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankInfo;
import com.unitbv.myquiz.api.dto.QuestionDuplicateDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.mapper.QuestionDuplicateMapper;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.specifications.QuestionBankAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuestionBankSpecification;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionBankService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionBankService.class);

    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionRepository questionRepository;
    private final QuestionErrorRepository questionErrorRepository;
    private final QuestionDuplicateRepository questionDuplicateRepository;
    private final AuthorRepository authorRepository;
    private final CourseService courseService;
    private final QuestionMapper questionMapper;
    private final QuestionDuplicateMapper questionDuplicateMapper;

    @Autowired
    public QuestionBankService(QuestionBankRepository questionBankRepository, QuestionBankAuthorRepository questionBankAuthorRepository, QuestionRepository questionRepository, QuestionErrorRepository questionErrorRepository,
                               QuestionDuplicateRepository questionDuplicateRepository, AuthorRepository authorRepository, CourseService courseService, QuestionMapper questionMapper, QuestionDuplicateMapper questionDuplicateMapper) {
        this.questionBankRepository = questionBankRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionRepository = questionRepository;
        this.questionErrorRepository = questionErrorRepository;
        this.questionDuplicateRepository = questionDuplicateRepository;
        this.authorRepository = authorRepository;
        this.courseService = courseService;
        this.questionMapper = questionMapper;
        this.questionDuplicateMapper = questionDuplicateMapper;
    }


    public QuestionBank createQuestionBank(String courseName, String questionBankName, StudyYear studyYear) {
        QuestionBank questionBank;
        Optional<QuestionBank> searchQuestionBank = questionBankRepository.findOne(QuestionBankSpecification.byNameAndCourseAndStudyYear(questionBankName, courseName, studyYear));
        if (searchQuestionBank.isEmpty()) {
            QuestionBank newQuestionBank = new QuestionBank();
            newQuestionBank.setName(questionBankName);
            newQuestionBank.setCourse(courseService.getOrCreateCourseEntity(courseName));
            newQuestionBank.setStudyYear(studyYear);
            questionBank = questionBankRepository.save(newQuestionBank);
        } else {
            questionBank = searchQuestionBank.get();
        }
        return questionBank;
    }


    public void deleteAll() {
        questionBankRepository.deleteAll();
    }


    @Transactional(readOnly = true)
    public List<QuestionBankDto> getAllQuestionBanks() {
        List<QuestionBank> questionBanks = questionBankRepository.findAll();
        questionBanks.sort(Comparator.comparing(QuestionBank::getCourseName));
        return questionBanks.stream().map(questionBank -> {
            QuestionBankDto dto = new QuestionBankDto();
            dto.setId(questionBank.getId());
            dto.setName(questionBank.getName());
            dto.setCourse(questionBank.getCourseName());
            dto.setStudyYear(questionBank.getStudyYear());
            // Fetch questions for this questionBank using QuestionSpecification
            var spec = QuestionSpecification.byFilters(null, null, questionBank.getId(), null);
            List<Question> questions = questionRepository.findAll(spec);
            int mcCount = 0;
            int tfCount = 0;
            for (Question question : questions) {
                if (question.getType() == QuestionType.MULTICHOICE) {
                    mcCount++;
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    tfCount++;
                }
            }
            dto.setMcQuestionsCount(mcCount);
            dto.setTfQuestionsCount(tfCount);
            dto.setNumberOfDuplicates(countDuplicateQuestions(questions));
            // count authors
            List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasQuestionBankId(questionBank.getId()).and(QuestionBankAuthorSpecification.fetchQuestions()));
            dto.setNoAuthors(questionBankAuthors.size());
            return dto;
        }).toList();
    }


    /**
     * Delete a questionBank by ID along with all related data.
     * OPTIMIZED for performance with batch operations and reduced queries.
     * <p>
     * Deletion sequence (following question-bank-sd.md specifications):
     * 1. Verify questionBank exists
     * 2. Fetch all QuestionBankAuthor entries with authors (single query with fetch)
     * 3. Batch delete all Questions for this questionBank (single query)
     * 4. Batch delete all questionBankErrors for this questionBank (single query)
     * 5. Batch delete all QuestionErrors for this questionBank (single query)
     * 6. Delete all QuestionBankAuthor entries (already loaded)
     * 7. Delete the QuestionBank itself
     * 8. Batch check and delete orphaned Authors (optimized with IN clause)
     *
     * @param id QuestionBank ID to delete
     * @throws IllegalArgumentException if questionBank with given ID does not exist
     */
    @Transactional
    public void deleteQuestionBankById(Long id) {
        long startTime = System.currentTimeMillis();
        logger.atInfo().addArgument(id).log("Starting optimized deletion of questionBank with ID: {}");

        ensureQuestionBankExists(id);
        List<QuestionBankAuthor> questionBankAuthors = loadQuestionBankAuthorsForDeletion(id);
        DeletionScope scope = collectDeletionScope(questionBankAuthors);

        if (!scope.questionBankAuthorIds().isEmpty()) {
            deleteQuestionsForQuestionBankAuthors(scope.questionBankAuthorIds());
            deleteErrorsForQuestionBankAuthors(scope.questionBankAuthorIds());
        }

        deleteQuestionBankAuthors(questionBankAuthors);
        questionBankRepository.deleteById(id);
        logger.atInfo().addArgument(id).log("Deleted questionBank with ID: {}");

        cleanupOrphanedAuthors(scope.authorsToCheck());

        long totalTime = System.currentTimeMillis() - startTime;
        logger.atInfo()
              .addArgument(id)
              .addArgument(totalTime)
              .log("Completed optimized deletion of questionBank {} in {}ms");
    }


    @Transactional(readOnly = true)
    public QuestionBankDto getQuestionBankById(Long id) {
        return buildQuestionBankDto(id);
    }

    private QuestionBankDto buildQuestionBankDto(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("QuestionBank ID cannot be null");
        }

        Optional<QuestionBank> questionBank = questionBankRepository.findById(id);
        if (questionBank.isPresent()) {
            QuestionBank q = questionBank.get();
            QuestionBankDto dto = new QuestionBankDto();
            dto.setId(q.getId());
            dto.setName(q.getName());
            dto.setCourse(q.getCourseName());
            dto.setStudyYear(q.getStudyYear());
            // Fetch questions for this questionBank using QuestionSpecification
            var spec = QuestionSpecification.byFilters(null, null, id, null);
            List<Question> questions = questionRepository.findAll(spec);
            // Separate MC and TF questions and convert to DTOs
            List<QuestionDto> questionsMC = new ArrayList<>();
            List<QuestionDto> questionsTF = new ArrayList<>();
            for (Question question : questions) {
                QuestionDto questionDto = questionMapper.toDto(question);
                if (question.getType() == QuestionType.MULTICHOICE) {
                    questionsMC.add(questionDto);
                } else if (question.getType() == QuestionType.TRUEFALSE) {
                    questionsTF.add(questionDto);
                }
            }
            dto.setQuestionsMultichoice(questionsMC);
            dto.setQuestionsTruefalse(questionsTF);
            dto.setMcQuestionsCount(questionsMC.size());
            dto.setTfQuestionsCount(questionsTF.size());
            dto.setNumberOfDuplicates(countDuplicateQuestions(questions));
            // Fetch authors with questions and questionBankErrors eagerly loaded
            List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasQuestionBankId(id).and(QuestionBankAuthorSpecification.fetchQuestions()));
            dto.setNoAuthors(questionBankAuthors.size());
            List<AuthorDto> authorDtos = questionBankAuthors.stream().map(qa -> AuthorDto.builder().id(qa.getAuthor().getId()).name(qa.getAuthor().getName()).initials(qa.getAuthor().getInitials()).build()).toList();
            dto.setAuthors(authorDtos);
            String allSources = questionBankAuthors.stream()
                    .map(QuestionBankAuthor::getSource)
                    .filter(source -> source != null && !source.isBlank())
                    .collect(Collectors.joining(", "));
            if (!allSources.isBlank()) {
                dto.setSourceFile(allSources);
            }
            return dto;
        }
        throw new IllegalArgumentException("QuestionBank not found with ID: " + id);
    }
    public QuestionBank updateQuestionBank(Long id, String course, String name, StudyYear studyYear) {
        Optional<QuestionBank> questionBankOptional = questionBankRepository.findById(id);
        if (questionBankOptional.isPresent()) {
            QuestionBank questionBank = questionBankOptional.get();
            questionBank.setCourse(courseService.getOrCreateCourseEntity(course));
            questionBank.setName(name);
            questionBank.setStudyYear(studyYear);

            // update course
            CourseDto courseDto = new CourseDto();
            courseDto.setCourse(course);
            courseService.createCourse(courseDto);

            return questionBankRepository.save(questionBank);
        }
        return null;
    }


    @Transactional(readOnly = true)
    public List<QuestionBankDto> getQuestionBanksByCourse(String course) {
        Specification<QuestionBank> spec = QuestionBankSpecification.byCourse(course);
        List<QuestionBank> questionBanks = questionBankRepository.findAll(spec);
        return questionBanks.stream().map(questionBank -> {
            QuestionBankDto dto = new QuestionBankDto();
            dto.setId(questionBank.getId());
            dto.setName(questionBank.getName());
            dto.setCourse(questionBank.getCourseName());
            dto.setStudyYear(questionBank.getStudyYear());
            Set<QuestionBankAuthor> qbAuthors = questionBank.getQuestionBankAuthors();
            if (qbAuthors != null && !qbAuthors.isEmpty()) {
                dto.setSourceFile(qbAuthors.iterator().next().getSource());
            }
            List<AuthorDto> authorDtos = qbAuthors == null ? List.of() : qbAuthors.stream()
                    .map(qa -> AuthorDto.builder().id(qa.getAuthor().getId()).name(qa.getAuthor().getName()).initials(qa.getAuthor().getInitials()).build())
                    .toList();
            dto.setAuthors(authorDtos);
            // Use QuestionSpecification for MC and TF questions
            var questionSpec = QuestionSpecification.byFilters(null, null, questionBank.getId(), null);
            List<QuestionDto> mcQuestions = questionRepository.findAll(questionSpec).stream().filter(q -> q.getType() == QuestionType.MULTICHOICE).map(questionMapper::toDto).toList();
            dto.setQuestionsMultichoice(mcQuestions);
            List<QuestionDto> tfQuestions = questionRepository.findAll(questionSpec).stream().filter(q -> q.getType() == QuestionType.TRUEFALSE).map(questionMapper::toDto).toList();
            dto.setQuestionsTruefalse(tfQuestions);
            return dto;
        }).toList();
    }


    @Transactional(readOnly = true)
    public List<QuestionBankDto> getQuestionBanksByCourseId(Long courseId) {
        String courseName = courseService.getCourseName(courseId);
        if (courseName == null || courseName.isEmpty()) {
            return List.of();
        }
        Specification<QuestionBank> spec = QuestionBankSpecification.byCourse(courseName);
        List<QuestionBank> questionBanks = questionBankRepository.findAll(spec);
        return questionBanks.stream().map(q -> {
            QuestionBankDto dto = new QuestionBankDto();
            dto.setId(q.getId());
            dto.setName(q.getName());
            dto.setCourse(q.getCourseName());
            dto.setStudyYear(q.getStudyYear());
            List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(null, null, q.getId(), null));
            dto.setNumberOfDuplicates(countDuplicateQuestions(questions));
            return dto;
        }).toList();
    }


    public List<Question> getQuestionsByQuestionBankId(Long id) {
        var spec = QuestionSpecification.byFilters(null, null, id, null);
        return questionRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public QuestionBankFilterResponseDto filterQuestionBanks(QuestionBankFilterRequestDto filterInput) {
        if (filterInput == null) {
            throw new IllegalArgumentException("Filter input cannot be null");
        }

        PaginationParams pagination = PaginationSupport.normalize(filterInput.getPage(), filterInput.getPageSize());
        int page = pagination.page();
        int pageSize = pagination.pageSize();
        Long courseId = filterInput.getCourseId();

        // Fetch all courses for the dropdown
        List<CourseDto> allCourses = courseService.getAllCourses();

        List<QuestionBank> filtered = courseId == null
                ? questionBankRepository.findAll()
                : questionBankRepository.findAll(QuestionBankSpecification.byCourseId(courseId));
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        if (totalPages > 0 && page > totalPages) {
            page = totalPages;
        }
        int fromIndex = Math.min((page - 1) * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<QuestionBankDto> pageContent = filtered.subList(fromIndex, toIndex).stream().map(questionBank -> {
            QuestionBankDto dto = new QuestionBankDto();
            dto.setId(questionBank.getId());
            dto.setName(questionBank.getName());
            dto.setCourse(questionBank.getCourseName());
            dto.setStudyYear(questionBank.getStudyYear());
            var spec = QuestionSpecification.byFilters(null, null, questionBank.getId(), null);
            List<Question> questions = questionRepository.findAll(spec);
            int mcCount = 0;
            int tfCount = 0;
            for (Question question : questions) {
                if (question.getType() == QuestionType.MULTICHOICE) mcCount++;
                else if (question.getType() == QuestionType.TRUEFALSE) tfCount++;
            }
            dto.setMcQuestionsCount(mcCount);
            dto.setTfQuestionsCount(tfCount);
            dto.setNumberOfDuplicates(countDuplicateQuestions(questions));
            List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(QuestionBankAuthorSpecification.hasQuestionBankId(questionBank.getId()).and(QuestionBankAuthorSpecification.fetchQuestions()));
            dto.setNoAuthors(questionBankAuthors.size());
            return dto;
        }).toList();
        QuestionBankFilterResponseDto result = new QuestionBankFilterResponseDto();
        result.setQuestionBanks(pageContent);
        result.setTotalElements((long) totalElements);
        result.setTotalPages(totalPages);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setCourses(allCourses);
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable("allQuestionBankInfo")
    public List<QuestionBankInfo> getAllQuestionBankInfo() {
        List<QuestionBank> questionBanks = questionBankRepository.findAll();
        return questionBanks.stream().map(questionBank -> new QuestionBankInfo(questionBank.getId(), questionBank.getName(), questionBank.getCourseName())).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "questionBankInfoByCourse", key = "#selectedCourse")
    public List<QuestionBankInfo> getQuestionBankInfoByCourse(String selectedCourse) {
        Specification<QuestionBank> spec = QuestionBankSpecification.byCourse(selectedCourse);
        List<QuestionBank> questionBanks = questionBankRepository.findAll(spec);
        return questionBanks.stream().map(questionBank -> new QuestionBankInfo(questionBank.getId(), questionBank.getName(), questionBank.getCourseName())).toList();
    }

    @Transactional(readOnly = true)
    public QuestionBankDto getQuestionBankBasicById(Long id) {
        Optional<QuestionBank> questionBank = questionBankRepository.findById(id);
        if (questionBank.isEmpty()) {
            return null;
        }
        QuestionBankDto dto = new QuestionBankDto();
        dto.setId(questionBank.get().getId());
        dto.setName(questionBank.get().getName());
        dto.setCourse(questionBank.get().getCourseName());
        dto.setStudyYear(questionBank.get().getStudyYear());
        List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(null, null, id, null));
        dto.setNumberOfDuplicates(countDuplicateQuestions(questions));
        return dto;
    }

    private long countDuplicateQuestions(List<Question> questions) {
        if (questionDuplicateRepository == null || questions == null || questions.isEmpty()) {
            return 0L;
        }

        Set<Long> questionIds = questions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (questionIds.isEmpty()) {
            return 0L;
        }

        return questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(questionIds, questionIds)
                .stream()
                .flatMap(link -> java.util.stream.Stream.of(
                        link.getQuestion() != null ? link.getQuestion().getId() : null,
                        link.getDuplicateQuestion() != null ? link.getDuplicateQuestion().getId() : null
                ))
                .filter(Objects::nonNull)
                .filter(questionIds::contains)
                .distinct()
                .count();
    }

    @Transactional(readOnly = true)
    public QuestionBankExportDto getQuestionBankExtendedById(Long id) {
        QuestionBankExportDto dto = new QuestionBankExportDto();
        dto.setQuestionBank(buildQuestionBankDto(id));
        String questionBankName = dto.getQuestionBank() != null ? dto.getQuestionBank().getName() : null;

        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(
                QuestionBankAuthorSpecification.hasQuestionBankId(id)
                        .and(QuestionBankAuthorSpecification.fetchAuthor())
                        .and(QuestionBankAuthorSpecification.fetchQuestions())
        );

        List<QuestionBankExportAuthorSectionDto> authorSections = questionBankAuthors.stream()
                .sorted(Comparator.comparing(
                        qa -> qa.getAuthor() != null ? qa.getAuthor().getName() : null,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .map(questionBankAuthor -> buildAuthorSection(id, questionBankName, questionBankAuthor))
                .toList();

        dto.setAuthorSections(authorSections);
        return dto;
    }

    private void ensureQuestionBankExists(Long id) {
        if (!questionBankRepository.existsById(id)) {
            logger.atWarn().addArgument(id).log("QuestionBank with ID {} not found - cannot delete");
            throw new IllegalArgumentException("QuestionBank not found with ID: " + id);
        }
    }

    private List<QuestionBankAuthor> loadQuestionBankAuthorsForDeletion(Long questionBankId) {
        long fetchStart = System.currentTimeMillis();
        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(
                QuestionBankAuthorSpecification.hasQuestionBankId(questionBankId)
                        .and(QuestionBankAuthorSpecification.fetchAuthor())
        );
        logger.atInfo()
                .addArgument(questionBankAuthors.size())
                .addArgument(System.currentTimeMillis() - fetchStart)
                .log("Found {} QuestionBankAuthor entries in {}ms");
        return questionBankAuthors;
    }

    private DeletionScope collectDeletionScope(List<QuestionBankAuthor> questionBankAuthors) {
        Set<Author> authorsToCheck = new HashSet<>();
        List<Long> questionBankAuthorIds = new ArrayList<>();
        for (QuestionBankAuthor questionBankAuthor : questionBankAuthors) {
            questionBankAuthorIds.add(questionBankAuthor.getId());
            authorsToCheck.add(questionBankAuthor.getAuthor());
        }
        return new DeletionScope(questionBankAuthorIds, authorsToCheck);
    }

    private void deleteQuestionsForQuestionBankAuthors(List<Long> questionBankAuthorIds) {
        long deleteQuestionsStart = System.currentTimeMillis();
        int totalQuestionsDeleted = 0;
        for (Long questionBankAuthorId : questionBankAuthorIds) {
            Specification<Question> questionSpec = QuestionSpecification.byQuestionBankAuthorId(questionBankAuthorId);
            List<Question> questions = questionRepository.findAll(questionSpec);
            if (!questions.isEmpty()) {
                questionRepository.deleteAll(questions);
                totalQuestionsDeleted += questions.size();
            }
        }
        logger.atInfo()
                .addArgument(totalQuestionsDeleted)
                .addArgument(System.currentTimeMillis() - deleteQuestionsStart)
                .log("Deleted {} questions in {}ms");
    }

    private void deleteErrorsForQuestionBankAuthors(List<Long> questionBankAuthorIds) {
        long deleteErrorsStart = System.currentTimeMillis();
        int totalErrorsDeleted = 0;
        for (Long questionBankAuthorId : questionBankAuthorIds) {
            List<QuestionError> errors = questionErrorRepository.findByQuestionQuestionBankAuthorId(questionBankAuthorId);
            if (!errors.isEmpty()) {
                questionErrorRepository.deleteAll(errors);
                totalErrorsDeleted += errors.size();
            }
        }
        logger.atInfo()
                .addArgument(totalErrorsDeleted)
                .addArgument(System.currentTimeMillis() - deleteErrorsStart)
                .log("Deleted {} errors in {}ms");
    }

    private void deleteQuestionBankAuthors(List<QuestionBankAuthor> questionBankAuthors) {
        long deleteQuestionBankAuthorsStart = System.currentTimeMillis();
        if (!questionBankAuthors.isEmpty()) {
            questionBankAuthorRepository.deleteAll(questionBankAuthors);
            logger.atInfo()
                    .addArgument(questionBankAuthors.size())
                    .addArgument(System.currentTimeMillis() - deleteQuestionBankAuthorsStart)
                    .log("Deleted {} QuestionBankAuthor entries in {}ms");
        }
    }

    private void cleanupOrphanedAuthors(Set<Author> authorsToCheck) {
        long cleanupStart = System.currentTimeMillis();
        if (authorsToCheck.isEmpty()) {
            return;
        }

        List<Author> authorsToDelete = new ArrayList<>();
        for (Author author : authorsToCheck) {
            long remainingContributions = questionBankAuthorRepository.count(
                    QuestionBankAuthorSpecification.hasAuthor(author)
            );
            if (remainingContributions == 0) {
                authorsToDelete.add(author);
            }
        }

        if (!authorsToDelete.isEmpty()) {
            authorRepository.deleteAll(authorsToDelete);
            logger.atInfo()
                    .addArgument(authorsToDelete.size())
                    .log("Deleted {} orphaned authors");
        }
        logger.atInfo()
                .addArgument(System.currentTimeMillis() - cleanupStart)
                .log("Author cleanup completed in {}ms");
    }

    private QuestionBankExportAuthorSectionDto buildAuthorSection(Long questionBankId, String questionBankName, QuestionBankAuthor questionBankAuthor) {
        QuestionBankExportAuthorSectionDto section = new QuestionBankExportAuthorSectionDto();
        Author author = questionBankAuthor.getAuthor();
        section.setAuthor(AuthorDto.builder().id(author.getId()).name(author.getName()).initials(author.getInitials()).build());

        List<Question> authorQuestions = questionRepository.findAll(
                QuestionSpecification.byQuestionBankAuthorId(questionBankAuthor.getId())
        );

        section.setMultipleChoiceQuestions(mapQuestionsByType(authorQuestions, QuestionType.MULTICHOICE));
        section.setTrueFalseQuestions(mapQuestionsByType(authorQuestions, QuestionType.TRUEFALSE));
        section.setErrors(buildAuthorErrors(questionBankId, questionBankName, author));
        section.setDuplicateQuestions(buildAuthorDuplicateQuestions(authorQuestions));
        return section;
    }

    private List<QuestionDto> mapQuestionsByType(List<Question> questions, QuestionType type) {
        return questions.stream()
                .filter(question -> question.getType() == type)
                .sorted(Comparator.comparingInt(Question::getCrtNo))
                .map(questionMapper::toDto)
                .toList();
    }

    private List<QuestionErrorDto> buildAuthorErrors(Long questionBankId, String questionBankName, Author author) {
        return questionErrorRepository
                .findByQuestionQuestionBankAuthorQuestionBankIdAndQuestionQuestionBankAuthorAuthorId(questionBankId, author.getId())
                .stream()
                .filter(error -> !MyUtil.isDuplicateValidationError(error.getDescription()))
                .sorted(Comparator
                        .comparing(QuestionError::getRowNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QuestionError::getId, Comparator.nullsLast(Long::compareTo)))
                .map(error -> mapAuthorErrorDto(error, questionBankId, questionBankName, author.getName()))
                .toList();
    }

    private QuestionErrorDto mapAuthorErrorDto(QuestionError error, Long questionBankId, String questionBankName, String authorName) {
        QuestionErrorDto errorDto = new QuestionErrorDto();
        errorDto.setId(error.getId());
        errorDto.setDescription(error.getDescription());
        errorDto.setRow(error.getRowNumber());
        errorDto.setAuthorName(authorName);
        errorDto.setQuestionBankId(questionBankId);
        errorDto.setQuestionBankName(questionBankName);
        errorDto.setStatus(error.getStatus() != null ? error.getStatus() : ControllerSettings.ERROR_STATUS_OPEN);
        if (error.getQuestion() != null) {
            errorDto.setQuestionId(error.getQuestion().getId());
            if (error.getQuestion().getType() != null) {
                errorDto.setQuestionType(error.getQuestion().getType().name());
            }
        }
        return errorDto;
    }

    private List<QuestionDuplicateDto> buildAuthorDuplicateQuestions(List<Question> authorQuestions) {
        List<Long> authorQuestionIds = authorQuestions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .toList();
        if (authorQuestionIds.isEmpty()) {
            return List.of();
        }

        Map<Long, QuestionDuplicateDto> duplicateMap = new LinkedHashMap<>();
        questionDuplicateRepository
                .findByQuestionIdInOrDuplicateQuestionIdIn(authorQuestionIds, authorQuestionIds)
                .forEach(duplicateLink -> {
                    Question duplicateQuestion = selectQuestionForAuthor(duplicateLink, authorQuestionIds);
                    if (duplicateQuestion == null || duplicateQuestion.getId() == null) {
                        return;
                    }

                    QuestionDuplicateDto candidate = questionDuplicateMapper.toDuplicateDto(duplicateLink, duplicateQuestion);
                    duplicateMap.putIfAbsent(duplicateQuestion.getId(), candidate);
                });

        return duplicateMap.values().stream()
                .sorted(Comparator
                        .comparing(QuestionDuplicateDto::getRow, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QuestionDuplicateDto::getQuestionId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private record DeletionScope(List<Long> questionBankAuthorIds, Set<Author> authorsToCheck) {
    }

    private Question selectQuestionForAuthor(QuestionDuplicate duplicateLink, List<Long> authorQuestionIds) {
        if (duplicateLink == null || duplicateLink.getQuestion() == null || duplicateLink.getDuplicateQuestion() == null) {
            return null;
        }
        Long baseQuestionId = duplicateLink.getQuestion().getId();
        if (baseQuestionId != null && authorQuestionIds.contains(baseQuestionId)) {
            return duplicateLink.getQuestion();
        }
        return duplicateLink.getDuplicateQuestion();
    }
}
