package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseInfo;
import com.unitbv.myquiz.api.dto.QuestionBankInfo;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.mapper.QuestionDtoEnricher;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    private static final String EXCEL_FILE_EXTENSION = ".xlsx";
    private final QuestionBankRepository questionBankRepository;
    private final AuthorService authorService;
    private final QuestionRepository questionRepository;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final AuthorRepository authorRepository;
    private final QuestionMapper questionMapper;
    private final QuestionDtoEnricher questionDtoEnricher;
    private final ExcelParsingService excelParsingService;
    private final QuestionDuplicationService questionDuplicationService;
    private final TextProcessingService textProcessingService;

    @Autowired
    public QuestionService(
            AuthorService authorService, QuestionRepository questionRepository,
            QuestionBankAuthorRepository questionBankAuthorRepository, QuestionBankRepository questionBankRepository, CourseRepository courseRepository, CourseService courseService,
            AuthorRepository authorRepository, QuestionMapper questionMapper, QuestionDtoEnricher questionDtoEnricher, ExcelParsingService excelParsingService,
            QuestionDuplicationService questionDuplicationService,
            TextProcessingService textProcessingService
    ) {
        this.authorService = authorService;
        this.questionRepository = questionRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionBankRepository = questionBankRepository;
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.authorRepository = authorRepository;
        this.questionMapper = questionMapper;
        this.questionDtoEnricher = questionDtoEnricher;
        this.excelParsingService = excelParsingService;
        this.questionDuplicationService = questionDuplicationService;
        this.textProcessingService = textProcessingService;
    }

    /**
     * Recursively parse Excel files from a folder structure.
     * <p>
     * This method traverses a directory tree, processing Excel files (.xlsx) and tracking invalid files.
     * It follows the upload-sd.md specifications for archive upload (Section 2.4).
     * <p>
     * Directory Structure Expected:
     * - Root folder containing author subfolders or direct Excel files
     * - Each author subfolder contains Excel files submitted by that author
     * - File names are used to extract author information
     *
     * @param questionBank The questionBank to associate questions with
     * @param folder       The root folder or file to process
     * @param noFilesInput Starting count of processed files
     * @return Total number of Excel files successfully processed
     * @throws IllegalArgumentException if questionBank is null
     */
    public int parseExcelFilesFromFolder(QuestionBank questionBank, File folder, int noFilesInput) {
        // Delegate to ExcelParsingService (convert File to Path)
        return excelParsingService.parseExcelFilesFromFolder(
                questionBank,
                folder.toPath(),
                noFilesInput
        );
    }

    /**
     * Parse an Excel file and extract questions from all sheets.
     * <p>
     * Processes the first sheet as multichoice questions and optionally
     * the second sheet as true/false questions. Creates a QuestionBankAuthor
     * association and tracks all errors during parsing.
     * <p>
     * Additionally checks for duplicate questions within the same course
     * and creates duplication associations if found.
     *
     * @param questionBank The questionBank to associate questions with (must not be null)
     * @param author       The author who submitted the file (must not be null)
     * @param filePath     Path to the Excel file (.xlsx format)
     * @return Status message indicating success or error details
     * @throws IllegalArgumentException if questionBank or author is null
     */
    public String parseFileSheets(QuestionBank questionBank, Author author, String filePath) {
        // Input validation
        if (questionBank == null) {
            throw new IllegalArgumentException("QuestionBank cannot be null");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            logger.atError().addArgument(filePath).log("File does not exist: {}");
            return "Error: File does not exist";
        }
        if (!file.canRead()) {
            logger.atError().addArgument(filePath).log("File is not readable: {}");
            return "Error: File is not readable";
        }
        if (!filePath.toLowerCase().endsWith(EXCEL_FILE_EXTENSION)) {
            logger.atError().addArgument(filePath).log("Invalid file type: {}");
            return "Error: Only .xlsx files are supported";
        }

        // Delegate parsing flow to the dedicated parser service.
        return excelParsingService.parseFileSheets(
                questionBank,
                author,
                file.toPath()
        );
    }

    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    public Question findQuestionById(Long id) {
        return questionRepository.findOne(QuestionSpecification.byId(id)).orElse(null);
    }

    @CacheEvict(
            value = {
                    "questions",
                    "allQuestions",
                    "questionsByQuestionBank",
                    "questionsByAuthor",
                    "questionsByAuthorName"
            }, allEntries = true
    )
    @Transactional
    public boolean deleteQuestion(Long id) {
        Question question = findQuestionById(id);
        if (question != null) {
            questionDuplicationService.removeAllDuplicateAssociationsForQuestion(id);
            questionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Question> findAllQuestions() {
        return (List<Question>) questionRepository.findAll();
    }

    public QuestionBank saveQuestionBank(QuestionBank questionBank) {
        return questionBankRepository.save(questionBank);
    }

    public List<Question> getQuestionBankQuestionsForAuthor(Long id) {
        // Refactored: Use Specification-based filtering for all question queries
        Specification<Question> spec = QuestionSpecification.byFilters(
                null,
                id,
                null,
                null
        );
        return questionRepository.findAll(spec);
    }

    public List<Question> getQuestionsForAuthorId(Long authorId, String course) {
        Specification<Question> spec = QuestionSpecification.byFilters(
                course,
                authorId,
                null,
                null
        );
        return questionRepository.findAll(spec);
    }

    public List<Question> getQuestionsForAuthorName(String authorName) {
        Specification<Question> spec = QuestionSpecification.hasAuthorName(authorName);
        return questionRepository.findAll(spec);
    }

    @Cacheable(value = "questionsByQuestionBank", key = "#questionBankId")
    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestionsByQuestionBankId(Long questionBankId) {
        Specification<Question> spec = QuestionSpecification.byFilters(
                null,
                null,
                questionBankId,
                null
        );
        List<Question> questions = questionRepository.findAll(spec);
        return convertQuestionsToEnrichedDtos(questions);
    }

    /**
     * Helper method to convert Question entities to DTOs with error enrichment.
     * Consolidates the repeated pattern of mapping to DTO and enriching with errors.
     *
     * @param questions Questions to convert
     * @return Converted and enriched DTOs
     */
    private List<QuestionDto> convertQuestionsToEnrichedDtos(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        List<QuestionDto> dtos = questions.stream().map(questionMapper::toDto).toList();
        questionDtoEnricher.enrichListWithErrors(
                dtos,
                questions
        );
        return dtos;
    }

    @Cacheable(value = "allQuestions")
    @Transactional(readOnly = true)
    public List<QuestionDto> getAllQuestions() {
        List<Question> questions = findAllQuestions();
        List<QuestionDto> dtos = questions.stream().map(questionMapper::toDto).toList();
        questionDtoEnricher.enrichListWithErrors(
                dtos,
                questions
        );
        return dtos;
    }

    // REST API operation implementations

    @Cacheable(value = "questions", key = "#id")
    @Transactional(readOnly = true)
    public QuestionDto getQuestionById(Long id) {
        Question question = findQuestionById(id);
        if (question != null) {
            QuestionDto dto = questionMapper.toDto(question);
            questionDtoEnricher.enrichWithErrors(
                    dto,
                    question
            );
            return dto;
        }
        return null;
    }

    @CacheEvict(
            value = {
                    "questions",
                    "allQuestions",
                    "questionsByQuestionBank",
                    "questionsByAuthor",
                    "questionsByAuthorName"
            }, allEntries = true
    )
    @Transactional
    public QuestionDto createQuestion(QuestionDto questionDto) {
        Question question = questionMapper.toEntity(questionDto);
        question.setAnswerReferenceText(textProcessingService.sanitizeReferenceForSave(questionDto.getAnswerReferenceText()));

        assignQuestionBankAuthorForQuestion(
                questionDto,
                question,
                null
        );

        Question savedQuestion = saveQuestion(question);
        return questionMapper.toDto(savedQuestion);
    }

    private void assignQuestionBankAuthorForQuestion(QuestionDto questionDto, Question question, QuestionBankAuthor fallbackQuestionBankAuthor) {
        Author author = resolveAuthor(
                questionDto,
                fallbackQuestionBankAuthor
        );
        QuestionBank questionBank = resolveQuestionBank(questionDto);
        if (author != null) {
            question.setQuestionBankAuthor(createQuestionBankAuthor(
                    questionBank,
                    author
            ));
        }
    }

    private QuestionBankAuthor createQuestionBankAuthor(QuestionBank questionBank, Author author) {
        Optional<QuestionBankAuthor> optQuestionBankAuthor = questionBankAuthorRepository.findOne(QuestionBankAuthorSpecification.byQuestionBankAndAuthor(
                questionBank.getId(),
                author.getId()
        ));
        QuestionBankAuthor questionBankAuthor;
        if (optQuestionBankAuthor.isPresent()) {
            questionBankAuthor = optQuestionBankAuthor.get();
        } else {
            questionBankAuthor = new QuestionBankAuthor();
            questionBankAuthor.setQuestionBank(questionBank);
            questionBankAuthor.setAuthor(author);
            questionBankAuthorRepository.save(questionBankAuthor);
        }
        return questionBankAuthor;
    }

    private Author createAuthor(QuestionDto questionDto) {
        String authorName = questionDto.getAuthorName();
        if (authorName == null || authorName.isBlank()) {
            return null;
        }
        AuthorDto authorDto = authorService.getAuthorByName(authorName);
        if (authorDto == null) {
            authorDto = new AuthorDto();
            authorDto.setName(authorName);
            authorDto.setInitials(authorService.extractInitials(authorName));
            authorDto = authorService.saveAuthorDto(authorDto);
        }
        // Return entity from repository
        return authorRepository.findById(authorDto.getId()).orElse(null);
    }

    private Author resolveAuthor(QuestionDto questionDto, QuestionBankAuthor fallbackQuestionBankAuthor) {
        Author createdOrFound = createAuthor(questionDto);
        if (createdOrFound != null) {
            return createdOrFound;
        }
        if (fallbackQuestionBankAuthor != null) {
            return fallbackQuestionBankAuthor.getAuthor();
        }
        return null;
    }

    private QuestionBank createQuestionBank(QuestionDto questionDto, String course) {
        if (questionDto.getQuestionBankId() != null) {
            return questionBankRepository.findById(questionDto.getQuestionBankId())
                                         .orElseThrow(() -> new IllegalArgumentException("QuestionBank not found for id: " + questionDto.getQuestionBankId()));
        }

        Specification<QuestionBank> spec = QuestionBankSpecification.byCourse(course);
        List<QuestionBank> questionBanks = questionBankRepository.findAll(spec);
        QuestionBank questionBank = questionBanks.stream().filter(q -> q.getName() != null && questionDto.getQuestionBankName() != null && q.getName()
                                                                                                                                            .equalsIgnoreCase(questionDto.getQuestionBankName()))
                                                 .findFirst().orElse(questionBanks.isEmpty() ? null : questionBanks.getFirst());
        if (questionBank == null) {
            questionBank = new QuestionBank();
            questionBank.setCourse(courseService.getOrCreateCourseEntity(course));
            questionBank.setName(questionDto.getQuestionBankName());
            saveQuestionBank(questionBank);
        }
        return questionBank;
    }

    private QuestionBank resolveQuestionBank(QuestionDto questionDto) {
        String course = createCourse(questionDto);
        return createQuestionBank(
                questionDto,
                course
        );
    }

    private String createCourse(QuestionDto questionDto) {
        String course = questionDto.getCourse();
        Specification<Course> spec = CourseSpecification.byCourseName(course);
        List<Course> courseDtos = courseRepository.findAll(spec);
        if (courseDtos.isEmpty() && (course != null && course.equals("Unknown Course"))) {
            CourseDto courseDto = new CourseDto();
            courseDto.setCourse(course);
            courseService.createCourse(courseDto);
        }
        return course;
    }

    @CacheEvict(
            value = {
                    "questions",
                    "allQuestions",
                    "questionsByQuestionBank",
                    "questionsByAuthor",
                    "questionsByAuthorName"
            }, allEntries = true
    )
    @Transactional
    public QuestionDto updateQuestion(QuestionDto questionDto) {
        if (questionDto.getId() == null) {
            return null;
        }
        Question existingQuestion = findQuestionById(questionDto.getId());
        if (existingQuestion == null) {
            return null;
        }

        applyEditableFields(
                existingQuestion,
                questionDto
        );
        assignQuestionBankAuthorForQuestion(
                questionDto,
                existingQuestion,
                existingQuestion.getQuestionBankAuthor()
        );

        Question savedQuestion = saveQuestion(existingQuestion);
        return questionMapper.toDto(savedQuestion);
    }

    private void applyEditableFields(Question existingQuestion, QuestionDto questionDto) {
        existingQuestion.setTitle(questionDto.getTitle());
        existingQuestion.setText(questionDto.getText());
        existingQuestion.setAnswerReferenceText(textProcessingService.sanitizeReferenceForSave(questionDto.getAnswerReferenceText()));
        existingQuestion.setChapter(questionDto.getChapter());
        if (questionDto.getType() != null) {
            existingQuestion.setType(questionDto.getType());
        }
        if (questionDto.getRow() != null) {
            existingQuestion.setCrtNo(questionDto.getRow());
        }

        existingQuestion.setResponse1(questionDto.getResponse1());
        existingQuestion.setResponse2(questionDto.getResponse2());
        existingQuestion.setResponse3(questionDto.getResponse3());
        existingQuestion.setResponse4(questionDto.getResponse4());
        existingQuestion.setWeightResponse1(questionDto.getWeightResponse1());
        existingQuestion.setWeightResponse2(questionDto.getWeightResponse2());
        existingQuestion.setWeightResponse3(questionDto.getWeightResponse3());
        existingQuestion.setWeightResponse4(questionDto.getWeightResponse4());
        existingQuestion.setWeightTrue(questionDto.getWeightTrue());
        existingQuestion.setWeightFalse(questionDto.getWeightFalse());
    }

    @Transactional(readOnly = true)
    public QuestionFilterResponseDto getQuestionsFiltered(String course, Long authorId, Integer page, Integer pageSize, Long questionBankId, QuestionType questionType) {
        String normalizedCourse = (course != null && !course.trim().isEmpty()) ? course.trim() : null;

        // Guard against invalid page/pageSize
        int validPage = (page != null && page > 0) ? page : 1;
        int validPageSize = (pageSize != null && pageSize > 0) ? pageSize : 10;

        // Convert 1-based page number to 0-based page index for Spring Data
        int pageIndex = validPage - 1;
        Pageable pageable = PageRequest.of(
                pageIndex,
                validPageSize
        );
        Specification<Question> spec = QuestionSpecification.byFilters(
                normalizedCourse,
                authorId,
                questionBankId,
                questionType
        );

        long startTime = System.currentTimeMillis();
        Page<Question> questions = questionRepository.findAll(
                spec,
                pageable
        );
        long queryTime = System.currentTimeMillis() - startTime;

        logger.atInfo().log(
                "Fetched page {} ({} questions of {} total) in {}ms for filters: course='{}', authorId={}, questionBankId={}, type={}",
                validPage,
                questions.getNumberOfElements(),
                questions.getTotalElements(),
                queryTime,
                normalizedCourse,
                authorId,
                questionBankId,
                questionType
        );

        List<QuestionDto> questionDtos = getQuestionDtosSortedByRow(questions);
        QuestionFilterResponseDto dto = QuestionFilterResponseDto.builder()
                .questions(questionDtos)
                // Return 1-based page number for frontend
                .page(questions.getNumber() + 1)
                .totalPages(questions.getTotalPages())
                .selectedCourse(normalizedCourse)
                .selectedAuthorId(authorId)
                .selectedQuestionBankId(questionBankId)
                .totalElements(questions.getTotalElements())
                .build();
        populateQuestionFilterMetadata(
                dto,
                normalizedCourse
        );
        return dto;
    }

    private void populateQuestionFilterMetadata(QuestionFilterResponseDto dto, String selectedCourse) {
        // Always populate all courses for dropdown
        List<CourseInfo> allCourses = courseService.getAllCourses().stream().map(CourseInfo::from).toList();
        dto.setAllCourses(allCourses);

        if (selectedCourse != null) {
            // Populate authors and question banks for selected course
            List<AuthorInfo> authors = authorService.getAuthorsByCourse(selectedCourse);
            dto.setAuthors(authors != null ? authors : new ArrayList<>());

            Specification<QuestionBank> questionBankSpecification = QuestionBankSpecification.byCourse(selectedCourse);
            List<QuestionBankInfo> questionBankInfoList = questionBankRepository.findAll(questionBankSpecification).stream().map(this::toQuestionBankInfo).sorted(Comparator.comparing(
                    QuestionBankInfo::getName,
                    Comparator.nullsLast(String::compareToIgnoreCase)
            )).toList();
            dto.setQuestionBanks(questionBankInfoList);
            return;
        }

        // No course selected: populate all authors and question banks
        List<AuthorInfo> allAuthors = authorService.getAllAuthorsBasic();
        dto.setAuthors(allAuthors != null ? allAuthors : new ArrayList<>());

        List<QuestionBankInfo> allQuestionBanks = questionBankRepository.findAll().stream().map(this::toQuestionBankInfo).sorted(Comparator.comparing(
                QuestionBankInfo::getCourse,
                Comparator.nullsLast(String::compareToIgnoreCase)
        ).thenComparing(
                QuestionBankInfo::getName,
                Comparator.nullsLast(String::compareToIgnoreCase)
        )).toList();
        dto.setQuestionBanks(allQuestionBanks);
    }

    private QuestionBankInfo toQuestionBankInfo(QuestionBank questionBank) {
        return new QuestionBankInfo(
                questionBank.getId(),
                questionBank.getName(),
                questionBank.getCourseName()
        );
    }

    private List<QuestionDto> getQuestionDtosSortedByRow(Page<Question> questions) {
        List<Question> entities = questions.getContent().stream().sorted((q1, q2) -> {
            if (q1.getCrtNo() == 0) return 1;
            if (q2.getCrtNo() == 0) return -1;
            return Integer.compare(
                    q1.getCrtNo(),
                    q2.getCrtNo()
            );
        }).toList();
        List<QuestionDto> dtos = entities.stream().map(questionMapper::toDto).toList();
        questionDtoEnricher.enrichListWithErrors(
                dtos,
                entities
        );
        return dtos;
    }

    /**
     * Get duplicates for a specific question.
     * This method retrieves all questions marked as duplicates of the given question.
     *
     * @param questionId The question ID to get duplicates for
     * @return Question DTO containing its linked duplicates, or null if question not found
     */
    @Transactional(readOnly = true)
    public QuestionDto getQuestionWithDuplicates(Long questionId) {
        if (questionId == null) {
            return null;
        }

        try {
            return questionDuplicationService.getQuestionDuplicates(questionId);
        }
        catch (Exception e) {
            logger.atError().setCause(e).addArgument(questionId).log(
                    "Error retrieving duplicates for question {}: {}",
                    e.getMessage()
            );
            return null;
        }
    }

    /**
     * Remove a question that is marked as a duplicate.
     * This deletes the question and all its duplication associations.
     *
     * @param questionId The ID of the question to delete
     * @return true if deleted successfully, false otherwise
     */
    @CacheEvict(
            value = {
                    "questions",
                    "allQuestions",
                    "questionsByQuestionBank",
                    "questionsByAuthor",
                    "questionsByAuthorName"
            }, allEntries = true
    )
    @Transactional
    public boolean removeDuplicateQuestion(Long questionId) {
        Question question = findQuestionById(questionId);
        if (question == null) {
            return false;
        }

        questionDuplicationService.removeAllDuplicateAssociationsForQuestion(questionId);
        questionRepository.deleteById(questionId);
        return true;
    }

    /**
     * Remove specific duplication links for a question.
     * This keeps the questions but removes the duplication associations.
     *
     * @param questionId   The primary question ID
     * @param duplicateIds List of duplicate question IDs to unlink
     * @return true if operation successful
     */
    @CacheEvict(
            value = {
                    "questions",
                    "allQuestions",
                    "questionsByQuestionBank",
                    "questionsByAuthor",
                    "questionsByAuthorName"
            }, allEntries = true
    )
    @Transactional
    public boolean removeDuplicationLinks(Long questionId, List<Long> duplicateIds) {
        if (questionId == null || duplicateIds == null || duplicateIds.isEmpty()) {
            return false;
        }

        try {
            questionDuplicationService.removeDuplicateAssociations(questionId);
            logger.atInfo().addArgument(duplicateIds.size()).addArgument(questionId)
                    .log("Removed {} duplication links for question {}");
            return true;
        }
        catch (Exception e) {
            logger.atError().setCause(e).log("Error removing duplication links for question {}: {}", questionId, e.getMessage());
            return false;
        }
    }

    /**
     * Get all duplicates for questions in a course.
     * Returns a filtered list of questions that have been marked as duplicates.
     *
     * @param course The course name
     * @return List of questions with duplication errors
     */
    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestionsWithDuplicates(String course) {
        if (course == null || course.trim().isEmpty()) {
            return List.of();
        }

        try {
            List<Question> courseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                    course,
                    null,
                    null,
                    null
            ));

            // Filter to only questions with duplication errors
            List<Question> questionsWithDuplicates = courseQuestions.stream().filter(this::hasDuplicateError).toList();

            logger.atInfo().addArgument(course).addArgument(questionsWithDuplicates.size()).log("Found {} questions with duplicates in course '{}'");

            List<QuestionDto> dtos = questionsWithDuplicates.stream().map(questionMapper::toDto).toList();
            questionDtoEnricher.enrichListWithErrors(
                    dtos,
                    questionsWithDuplicates
            );
            return dtos;
        }
        catch (Exception e) {
            logger.atError().setCause(e).addArgument(course).log(
                    "Error retrieving questions with duplicates for course '{}': {}",
                    e.getMessage()
            );
            return List.of();
        }
    }

    /**
     * Check if a question has duplication-related errors.
     *
     * @param question The question to check
     * @return true if question has any duplication errors
     */
    private boolean hasDuplicateError(Question question) {
        return questionDuplicationService.hasDuplicateError(question);
    }

}

