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
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.entities.QuestionError;
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
import java.util.List;
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
    @Autowired(required = false)
    private QuestionDuplicateRepository questionDuplicateRepository;
    private final AuthorRepository authorRepository;
    private final CourseService courseService;
    private final QuestionMapper questionMapper;

    @Autowired
    public QuestionBankService(QuestionBankRepository questionBankRepository, QuestionBankAuthorRepository questionBankAuthorRepository, QuestionRepository questionRepository, QuestionErrorRepository questionErrorRepository,
                               AuthorRepository authorRepository, CourseService courseService, QuestionMapper questionMapper) {
        this.questionBankRepository = questionBankRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionRepository = questionRepository;
        this.questionErrorRepository = questionErrorRepository;
        this.authorRepository = authorRepository;
        this.courseService = courseService;
        this.questionMapper = questionMapper;
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

        // Step 1: Verify questionBank exists (fast check)
        if (!questionBankRepository.existsById(id)) {
            logger.atWarn().addArgument(id).log("QuestionBank with ID {} not found - cannot delete");
            throw new IllegalArgumentException("QuestionBank not found with ID: " + id);
        }

        // Step 2: Fetch all QuestionBankAuthor entries with authors in ONE query (eager fetch)
        long fetchStart = System.currentTimeMillis();
        List<QuestionBankAuthor> questionBankAuthors = questionBankAuthorRepository.findAll(
            QuestionBankAuthorSpecification.hasQuestionBankId(id)
                .and(QuestionBankAuthorSpecification.fetchAuthor())
        );
        logger.atInfo()
              .addArgument(questionBankAuthors.size())
              .addArgument(System.currentTimeMillis() - fetchStart)
              .log("Found {} QuestionBankAuthor entries in {}ms");

        // Collect authors for orphan check (no extra queries)
        Set<Author> authorsToCheck = new HashSet<>();
        List<Long> questionBankAuthorIds = new ArrayList<>();
        for (QuestionBankAuthor questionBankAuthor : questionBankAuthors) {
            questionBankAuthorIds.add(questionBankAuthor.getId());
            authorsToCheck.add(questionBankAuthor.getAuthor());
        }

        if (!questionBankAuthorIds.isEmpty()) {
            // Step 3: Batch delete all Questions for ALL questionBankAuthors (single query per QuestionBankAuthor)
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

            // Step 4: Batch delete all QuestionErrors for ALL questionBankAuthors (single query per QuestionBankAuthor)
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

        // Step 5: Delete all QuestionBankAuthor entries (already loaded, batch delete)
        long deletequestionBankAuthorsStart = System.currentTimeMillis();
        if (!questionBankAuthors.isEmpty()) {
            questionBankAuthorRepository.deleteAll(questionBankAuthors);
            logger.atInfo()
                  .addArgument(questionBankAuthors.size())
                  .addArgument(System.currentTimeMillis() - deletequestionBankAuthorsStart)
                  .log("Deleted {} QuestionBankAuthor entries in {}ms");
        }

        // Step 6: Delete the QuestionBank itself
        questionBankRepository.deleteById(id);
        logger.atInfo().addArgument(id).log("Deleted questionBank with ID: {}");

        // Step 7: Optimized orphaned Author cleanup
        long cleanupStart = System.currentTimeMillis();
        if (!authorsToCheck.isEmpty()) {
            List<Author> authorsToDelete = new ArrayList<>();

            // Batch check all authors in one pass
            for (Author author : authorsToCheck) {
                Long remainingContributions = questionBankAuthorRepository.count(
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

        long totalTime = System.currentTimeMillis() - startTime;
        logger.atInfo()
              .addArgument(id)
              .addArgument(totalTime)
              .log("Completed optimized deletion of questionBank {} in {}ms");
    }


    @Transactional(readOnly = true)
    public QuestionBankDto getQuestionBankById(Long id) {
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


    public int getCompareTo(QuestionBankDto q1, QuestionBankDto q2) {
        if (q1 == null || q2 == null) {
            return 0;
        }
        return q1.getCourse().compareTo(q2.getCourse());
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
            if (questionBank.getQuestionBankAuthors() != null && !questionBank.getQuestionBankAuthors().isEmpty()) {
                dto.setSourceFile(questionBank.getQuestionBankAuthors().iterator().next().getSource());
            }
            List<AuthorDto> authorDtos = questionBank.getQuestionBankAuthors().stream().map(qa -> AuthorDto.builder().id(qa.getAuthor().getId()).name(qa.getAuthor().getName()).initials(qa.getAuthor().getInitials()).build()).toList();
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

        int page = filterInput.getPage() != null ? filterInput.getPage() : 1;
        int pageSize = filterInput.getPageSize() != null ? filterInput.getPageSize() : 10;
        // Guard against invalid paging values
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
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
        dto.setQuestionBank(getQuestionBankById(id));

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
                .map(questionBankAuthor -> {
                    QuestionBankExportAuthorSectionDto section = new QuestionBankExportAuthorSectionDto();
                    Author author = questionBankAuthor.getAuthor();
                    section.setAuthor(AuthorDto.builder().id(author.getId()).name(author.getName()).initials(author.getInitials()).build());

                    List<Question> authorQuestions = questionRepository.findAll(
                            QuestionSpecification.byQuestionBankAuthorId(questionBankAuthor.getId())
                    );

                    section.setMultipleChoiceQuestions(authorQuestions.stream()
                            .filter(question -> question.getType() == QuestionType.MULTICHOICE)
                            .sorted(Comparator.comparingInt(Question::getCrtNo))
                            .map(questionMapper::toDto)
                            .toList());

                    section.setTrueFalseQuestions(authorQuestions.stream()
                            .filter(question -> question.getType() == QuestionType.TRUEFALSE)
                            .sorted(Comparator.comparingInt(Question::getCrtNo))
                            .map(questionMapper::toDto)
                            .toList());

                    section.setErrors(questionErrorRepository
                            .findByQuestionQuestionBankAuthorQuestionBankIdAndQuestionQuestionBankAuthorAuthorId(id, author.getId())
                            .stream()
                            .sorted(Comparator
                                    .comparing(QuestionError::getRowNumber, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(QuestionError::getId, Comparator.nullsLast(Long::compareTo)))
                            .map(error -> {
                                QuestionErrorDto errorDto = new QuestionErrorDto();
                                errorDto.setId(error.getId());
                                errorDto.setDescription(error.getDescription());
                                errorDto.setRow(error.getRowNumber());
                                errorDto.setAuthorName(author.getName());
                                errorDto.setQuestionBankId(id);
                                errorDto.setQuestionBankName(dto.getQuestionBank() != null ? dto.getQuestionBank().getName() : null);
                                errorDto.setStatus(error.getStatus() != null ? error.getStatus() : ControllerSettings.ERROR_STATUS_OPEN);
                                if (error.getQuestion() != null) {
                                    errorDto.setQuestionId(error.getQuestion().getId());
                                    if (error.getQuestion().getType() != null) {
                                        errorDto.setQuestionType(error.getQuestion().getType().name());
                                    }
                                }
                                return errorDto;
                            })
                            .toList());

                    List<Long> authorQuestionIds = authorQuestions.stream()
                            .map(Question::getId)
                            .filter(questionId -> questionId != null)
                            .toList();
                    if (questionDuplicateRepository == null || authorQuestionIds.isEmpty()) {
                        section.setDuplicateQuestions(List.of());
                    } else {
                        section.setDuplicateQuestions(questionDuplicateRepository
                                .findByQuestionIdInOrDuplicateQuestionIdIn(authorQuestionIds, authorQuestionIds)
                                .stream()
                                .map(duplicate -> authorQuestionIds.contains(duplicate.getQuestion().getId())
                                        ? duplicate.getQuestion()
                                        : duplicate.getDuplicateQuestion())
                                .distinct()
                                .sorted(Comparator.comparingInt(Question::getCrtNo))
                                .map(question -> {
                                    QuestionDto duplicateDto = new QuestionDto();
                                    duplicateDto.setId(question.getId());
                                    duplicateDto.setTitle(question.getTitle());
                                    duplicateDto.setText(question.getText());
                                    duplicateDto.setRow(question.getCrtNo());
                                    return duplicateDto;
                                })
                                .toList());
                    }

                    return section;
                })
                .toList();

        dto.setAuthorSections(authorSections);
        return dto;
    }
}

