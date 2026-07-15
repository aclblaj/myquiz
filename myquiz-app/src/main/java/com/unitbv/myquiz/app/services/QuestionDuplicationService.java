package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.DuplicateComparisonStrategy;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.mapper.QuestionDtoEnricher;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Service
public class QuestionDuplicationService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionDuplicationService.class);
    private static final String DEFAULT_SIMILARITY_ALGORITHM = DuplicateComparisonStrategy.STRING_EQUALITY.getAlgorithmName();
    private static final String MSG_NO_COURSE_FOR_DUPLICATE_CHECK = "No course provided for duplicate checking";
    private static final int DUPLICATE_CHECK_BATCH_SIZE = 250;
    private static final int DUPLICATE_CAUSE_MAX_LENGTH = 1000;
    private static final String CAUSE_DEFAULT_DUPLICATE = "Duplicate detected";
    private static final String CAUSE_DEFAULT_SUFFIX = "duplicate detected";
    private static final String CAUSE_FIELD_TITLE = "Title";
    private static final String CAUSE_FIELD_TEXT = "Text";
    private static final String CAUSE_FIELD_ANSWER = "Answer";
    private static final String CAUSE_SUBSTRING_TEMPLATE = "%s: '%s' found as substring into '%s'";
    private static final String CAUSE_EXACT_TEMPLATE = "%s: '%s' matches exactly '%s'";
    private static final String CAUSE_SIMILAR_TEMPLATE = "%s: '%s' considered similar to '%s'";
    private static final String CAUSE_PART_DELIMITER = "; ";
    private static final String CAUSE_PART_SPLIT_REGEX = "\\s*;\\s*";
    private static final int CAUSE_PRIORITY_EXACT = 0;
    private static final int CAUSE_PRIORITY_SUBSTRING = 1;
    private static final int CAUSE_PRIORITY_SIMILAR = 2;
    private static final int CAUSE_PRIORITY_GENERIC = 3;
    public static final int NUM_ANSWERS = 4;

    private final QuestionRepository questionRepository;
    private final QuestionErrorRepository questionErrorRepository;
    private final QuestionDuplicateRepository questionDuplicateRepository;
    private final QuestionMapper questionMapper;
    private final QuestionDtoEnricher questionDtoEnricher;
    private final Map<String, AbstractQuestionSimilarityStrategy> similarityStrategies;
    private final AbstractQuestionSimilarityStrategy defaultSimilarityStrategy;
    private final Executor duplicateQuestionCheckTaskExecutor;

    @Autowired
    public QuestionDuplicationService(
            QuestionRepository questionRepository, QuestionErrorRepository questionErrorRepository, QuestionDuplicateRepository questionDuplicateRepository, QuestionMapper questionMapper,
            QuestionDtoEnricher questionDtoEnricher, List<AbstractQuestionSimilarityStrategy> similarityStrategies,
            @Value("${myquiz.duplicates.similarity.algorithm:string-equality}") String defaultSimilarityAlgorithm,
            @Qualifier("duplicateQuestionCheckTaskExecutor") Executor duplicateQuestionCheckTaskExecutor
    ) {
        this.questionErrorRepository = questionErrorRepository;
        this.questionRepository = questionRepository;
        this.questionDuplicateRepository = questionDuplicateRepository;
        this.questionMapper = questionMapper;
        this.questionDtoEnricher = questionDtoEnricher;
        this.similarityStrategies = buildStrategyMap(similarityStrategies);
        this.defaultSimilarityStrategy = resolveStrategy(defaultSimilarityAlgorithm);
        this.duplicateQuestionCheckTaskExecutor = duplicateQuestionCheckTaskExecutor;
    }

    public QuestionDuplicationService(
            QuestionRepository questionRepository, QuestionErrorRepository questionErrorRepository, QuestionDuplicateRepository questionDuplicateRepository,
            QuestionMapper questionMapper, QuestionDtoEnricher questionDtoEnricher
    ) {
        this(
                questionRepository,
                questionErrorRepository,
                questionDuplicateRepository,
                questionMapper,
                questionDtoEnricher,
                List.of(
                        new StringEqualityQuestionSimilarityStrategy(),
                        new LevenshteinQuestionSimilarityStrategy(),
                        new JaroWinklerQuestionSimilarityStrategy()
                ),
                DEFAULT_SIMILARITY_ALGORITHM,
                Runnable::run
        );
    }

    private static QuestionError buildQuestionErrorForQuestion(Question question, String description) {
        QuestionError questionError = new QuestionError();
        questionError.setQuestion(question);
        questionError.setRowNumber(question.getCrtNo());
        questionError.setDescription(getDescriptionWithTitle(
                question,
                description
        ));
        return questionError;
    }

    public static String getDescriptionWithTitle(Question question, String description) {
        return description + " (" + question.getTitle() + ")";
    }

    private static boolean hasAllMultichoiceAnswers(Question question) {
        return question.getResponse1() != null && question.getResponse2() != null && question.getResponse3() != null && question.getResponse4() != null;
    }

    private static boolean hasTrueFalseAnswer(Question question) {
        return question.getResponse1() != null && !question.getResponse1().isBlank();
    }

    private Map<String, AbstractQuestionSimilarityStrategy> buildStrategyMap(List<AbstractQuestionSimilarityStrategy> strategies) {
        Map<String, AbstractQuestionSimilarityStrategy> strategyMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (strategies != null) {
            for (AbstractQuestionSimilarityStrategy strategy : strategies) {
                if (strategy != null) {
                    strategyMap.put(
                            strategy.getAlgorithmName(),
                            strategy
                    );
                }
            }
        }
        strategyMap.computeIfAbsent(
                DEFAULT_SIMILARITY_ALGORITHM,
                key -> new StringEqualityQuestionSimilarityStrategy()
        );
        if (!strategyMap.containsKey(DuplicateComparisonStrategy.JARO_WINKLER.getAlgorithmName())) {
            strategyMap.put(
                    DuplicateComparisonStrategy.JARO_WINKLER.getAlgorithmName(),
                    new JaroWinklerQuestionSimilarityStrategy()
            );
        }
        return strategyMap;
    }

    private AbstractQuestionSimilarityStrategy resolveStrategy(String algorithmName) {
        AbstractQuestionSimilarityStrategy strategy = similarityStrategies.get(algorithmName);
        if (strategy != null) {
            return strategy;
        }
        logger.atWarn().addArgument(algorithmName).addArgument(DEFAULT_SIMILARITY_ALGORITHM)
              .log("Unknown duplicate similarity algorithm '{}', falling back to {}");
        return similarityStrategies.get(DEFAULT_SIMILARITY_ALGORITHM);
    }

    /**
     * Check for duplicate questions between author's questions and all existing questions in the database
     * for the same course.
     * <p>
     * This method validates that questions submitted by authors don't overlap with existing questions
     * in the database for the same course. It checks both question titles and answers for duplicates.
     *
     * @param authors List of authors whose questions should be validated
     * @param course  Course name to filter questions
     * @return List of QuestionError objects representing duplicate violations
     */
    @Transactional
    public List<QuestionError> checkDuplicateQuestionsForAuthors(List<Author> authors, String course) {
        if (authors == null || authors.isEmpty()) {
            logger.atWarn().log("No authors provided for duplicate checking");
            return new ArrayList<>();
        }
        if (course == null || course.isBlank()) {
            logger.atWarn().log(MSG_NO_COURSE_FOR_DUPLICATE_CHECK);
            return new ArrayList<>();
        }

        List<QuestionError> allErrors = new ArrayList<>();
        Set<String> persistedPairs = new HashSet<>();
        List<Question> allCourseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                course,
                null,
                null,
                null
        ));
        List<Long> courseQuestionIds = allCourseQuestions.stream().map(Question::getId).filter(Objects::nonNull).toList();

        DuplicateCleanupSummary cleanupSummary = clearDuplicateStateForQuestions(courseQuestionIds);
        logger.atInfo()
              .addArgument(course)
              .addArgument(cleanupSummary.duplicateLinksRemoved())
              .addArgument(cleanupSummary.duplicateErrorsRemoved())
              .log("Pre-check duplicate cleanup for course '{}': removed links={}, removed errors={}");

        logger.atInfo()
              .addArgument(course)
              .addArgument(allCourseQuestions.size())
              .log("Checking duplicates for course '{}' against {} existing questions");

        for (Author author : authors) {
            List<Question> authorQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                    course,
                    author.getId(),
                    null,
                    null
            ));
            List<QuestionError> questionErrors = detectDuplicates(
                    authorQuestions,
                    allCourseQuestions,
                    persistedPairs
            );
            allErrors.addAll(questionErrors);

            logger.atInfo().addArgument(author.getName()).addArgument(authorQuestions.size()).addArgument(questionErrors.size()).log("{} - no. questions: {}, no. duplicates found: {}");
        }
        logger.atInfo().log(
                "Duplicate validation completed, found {} duplications",
                allErrors.size()
        );
        return allErrors;
    }

    /**
     * Check for duplicate questions in a course, comparing newly uploaded questions against all existing questions.
     * This method is specifically designed for use during file upload processing.
     *
     * @param allCourseQuestions All existing questions in the course
     * @param course             The course name
     * @param uploadedQuestions  The newly uploaded questions to check
     * @return List of QuestionError objects for detected duplicates
     */
    @Transactional
    public List<QuestionError> checkDuplicatesInCourse(List<Question> allCourseQuestions, String course, List<Question> uploadedQuestions) {
        return checkDuplicatesInCourseInternal(
                allCourseQuestions,
                course,
                uploadedQuestions
        );
    }

    private List<QuestionError> checkDuplicatesInCourseInternal(List<Question> allCourseQuestions, String course, List<Question> uploadedQuestions) {
        if (uploadedQuestions == null || uploadedQuestions.isEmpty()) {
            logger.atWarn().log("No uploaded questions provided for duplicate checking");
            return new ArrayList<>();
        }
        if (course == null || course.isBlank()) {
            logger.atWarn().log(MSG_NO_COURSE_FOR_DUPLICATE_CHECK);
            return new ArrayList<>();
        }

        List<Question> safeAllCourseQuestions = allCourseQuestions == null ? List.of() : allCourseQuestions;
        List<Long> courseQuestionIds = safeAllCourseQuestions.stream().map(Question::getId).filter(Objects::nonNull).toList();
        DuplicateCleanupSummary cleanupSummary = clearDuplicateStateForQuestions(courseQuestionIds);
        logger.atInfo().addArgument(course).addArgument(cleanupSummary.duplicateLinksRemoved()).addArgument(cleanupSummary.duplicateErrorsRemoved())
              .log("Pre-check duplicate cleanup for course '{}': removed links={}, removed errors={}");

        logger.atInfo().addArgument(uploadedQuestions.size()).addArgument(course).addArgument(safeAllCourseQuestions.size())
              .log("Checking {} uploaded questions for duplicates in course '{}' against {} existing questions");

        List<QuestionError> questionErrors = detectDuplicates(
                uploadedQuestions,
                safeAllCourseQuestions
        );

        logger.atInfo().addArgument(questionErrors.size()).addArgument(course).log("Found {} duplicate violations in course '{}'");

        return questionErrors;
    }

    /**
     * Validate duplicates for all questions uploaded in a specific questionBank.
     * Loads both uploaded and existing course questions from DB after parse/save.
     */
    @Transactional
    public List<QuestionError> checkDuplicatesForQuestionBank(String course, Long questionBankId) {
        if (course == null || course.isBlank()) {
            logger.atWarn().log(MSG_NO_COURSE_FOR_DUPLICATE_CHECK);
            return new ArrayList<>();
        }
        if (questionBankId == null) {
            logger.atWarn().log("No questionBankId provided for duplicate checking");
            return new ArrayList<>();
        }

        List<Question> allCourseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                course,
                null,
                null,
                null
        ));
        List<Question> uploadedQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                course,
                null,
                questionBankId,
                null
        ));

        return checkDuplicatesInCourseInternal(
                allCourseQuestions,
                course,
                uploadedQuestions
        );
    }

    /**
     * Detect duplicates by comparing author's questions against existing questions in the database.
     * Optimized for performance using caching and batch operations.
     * Now preserves question titles instead of marking them as SKIPPED_DUE_TO_ERROR.
     * Note: QuestionError records are persisted after duplicate analysis is complete.
     *
     * @param authorQuestions    Questions to validate
     * @param allCourseQuestions All questions from the course used as duplicate candidates
     * @return List of errors for duplicate violations
     */
    private List<QuestionError> detectDuplicates(List<Question> authorQuestions, List<Question> allCourseQuestions) {
        return detectDuplicates(authorQuestions, allCourseQuestions, new HashSet<>());
    }

    private List<QuestionError> detectDuplicates(
            List<Question> authorQuestions,
            List<Question> allCourseQuestions,
            Set<String> persistedPairs
    ) {
        return detectDuplicatesInternal(
                authorQuestions,
                allCourseQuestions,
                true,
                false,
                persistedPairs,
                defaultSimilarityStrategy
        );
    }

    private List<QuestionError> detectDuplicatesForRecompute(
            List<Question> authorQuestions,
            List<Question> allCourseQuestions,
            Set<String> persistedPairs,
            AbstractQuestionSimilarityStrategy similarityStrategy
    ) {
        return detectDuplicatesInternal(
                authorQuestions,
                allCourseQuestions,
                false,
                true,
                persistedPairs,
                similarityStrategy
        );
    }

    private List<QuestionError> detectDuplicatesInternal(
            List<Question> authorQuestions, List<Question> allCourseQuestions, boolean checkExistingLinks, boolean ignoreExistingErrorPrefixes, Set<String> persistedPairs,
            AbstractQuestionSimilarityStrategy similarityStrategy
    ) {
        List<QuestionError> questionErrors = new ArrayList<>();
        if (authorQuestions == null || authorQuestions.isEmpty()) {
            return questionErrors;
        }

        Map<Long, Set<String>> existingErrorPrefixesByQuestionId = buildExistingErrorPrefixMap(
                authorQuestions,
                ignoreExistingErrorPrefixes
        );

        DuplicateExecutionPlan executionPlan = buildDuplicateExecutionPlan(authorQuestions.size());
        int processed = 0;

        logger.atInfo().addArgument(executionPlan.total()).addArgument(DUPLICATE_CHECK_BATCH_SIZE).addArgument(executionPlan.totalBatches())
              .log("Submitting duplicate checks for {} questions in sequential batches of {} ({} batches)");

        for (int start = 0, batchIndex = 1; start < executionPlan.total(); start += DUPLICATE_CHECK_BATCH_SIZE, batchIndex++) {
            int end = Math.min(
                    start + DUPLICATE_CHECK_BATCH_SIZE,
                    executionPlan.total()
            );
            List<Question> batch = authorQuestions.subList(
                    start,
                    end
            );
            Map<String, String> batchDetectedPairCauses = new LinkedHashMap<>();

            logger.atDebug().addArgument(batchIndex).addArgument(executionPlan.totalBatches()).addArgument(batch.size())
                  .log("Starting duplicate-check batch {}/{} with {} questions");

            List<DuplicateCheckTask> tasks = buildDuplicateCheckTasks(
                    batch,
                    existingErrorPrefixesByQuestionId,
                    allCourseQuestions,
                    similarityStrategy
            );

            processed = processBatchTasks(
                    tasks,
                    allCourseQuestions,
                    similarityStrategy,
                    processed,
                    executionPlan,
                    questionErrors,
                    batchDetectedPairCauses
            );

            int persistedInBatch = persistDuplicateLinks(
                    batchDetectedPairCauses,
                    persistedPairs,
                    checkExistingLinks
            );

            logger.atDebug().addArgument(batchIndex).addArgument(executionPlan.totalBatches()).addArgument(batchDetectedPairCauses.size()).addArgument(persistedInBatch)
                  .log("Finished duplicate-check batch {}/{}; detected pair keys={}, persisted links={}");

            if (batchIndex < executionPlan.totalBatches()) {
                logger.atDebug().addArgument(batchIndex + 1).addArgument(executionPlan.totalBatches())
                      .log("Starting next batch ({}/{}) after previous batch persistence completed");
            }
        }

        return questionErrors;
    }

    private DuplicateExecutionPlan buildDuplicateExecutionPlan(int total) {
        int progressStep = Math.max(
                1,
                total / 5
        );
        int totalBatches = (total + DUPLICATE_CHECK_BATCH_SIZE - 1) / DUPLICATE_CHECK_BATCH_SIZE;
        return new DuplicateExecutionPlan(
                total,
                progressStep,
                totalBatches
        );
    }

    private List<DuplicateCheckTask> buildDuplicateCheckTasks(
            List<Question> batch,
            Map<Long, Set<String>> existingErrorPrefixesByQuestionId,
            List<Question> allCourseQuestions,
            AbstractQuestionSimilarityStrategy similarityStrategy
    ) {
        return batch.stream().map(question -> {
            Set<String> existingErrorPrefixes = question == null ? Set.of() : existingErrorPrefixesByQuestionId.getOrDefault(
                    question.getId(),
                    Set.of()
            );
            CompletableFuture<QuestionDuplicateCheckResult> future = submitQuestionDuplicateCheck(
                    question,
                    allCourseQuestions,
                    similarityStrategy,
                    existingErrorPrefixes
            );
            return new DuplicateCheckTask(
                    question,
                    existingErrorPrefixes,
                    future
            );
        }).toList();
    }

    private int processBatchTasks(
            List<DuplicateCheckTask> tasks,
            List<Question> allCourseQuestions,
            AbstractQuestionSimilarityStrategy similarityStrategy,
            int processed,
            DuplicateExecutionPlan executionPlan,
            List<QuestionError> questionErrors,
            Map<String, String> batchDetectedPairCauses
    ) {
        for (DuplicateCheckTask task : tasks) {
            processed++;
            QuestionDuplicateCheckResult result = resolveTaskResult(
                    task,
                    allCourseQuestions,
                    similarityStrategy,
                    processed,
                    executionPlan.total()
            );

            if (result != null && result.question() != null && result.analysis() != null) {
                batchDetectedPairCauses.putAll(result.detectedPairCauses());
                addDuplicateErrors(
                        questionErrors,
                        result
                );
            }

            if (processed % executionPlan.progressStep() == 0 || processed == executionPlan.total()) {
                logger.atInfo().addArgument(processed).addArgument(executionPlan.total()).addArgument(questionErrors.size())
                      .log("Duplicate checks progress: {}/{} questions processed, {} errors collected");
            }
        }
        return processed;
    }

    private void addDuplicateErrors(List<QuestionError> questionErrors, QuestionDuplicateCheckResult result) {
        Question question = result.question();
        DuplicateAnalysis analysis = result.analysis();
        Set<String> existingErrorPrefixes = result.existingErrorPrefixes();

        if (analysis.hasMissingAnswer() && !existingErrorPrefixes.contains(MyUtil.MISSING_ANSWER)) {
            questionErrors.add(createQuestionError(
                    question,
                    MyUtil.MISSING_ANSWER
            ));
        }
        if (analysis.hasAnswerDuplicate() && !existingErrorPrefixes.contains(MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS)) {
            questionErrors.add(createQuestionError(
                    question,
                    MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS
            ));
        }
        if (analysis.hasTitleDuplicate() && !existingErrorPrefixes.contains(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS)) {
            questionErrors.add(createQuestionError(
                    question,
                    MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS
            ));
        }
    }

    private QuestionDuplicateCheckResult resolveTaskResult(
            DuplicateCheckTask task,
            List<Question> allCourseQuestions,
            AbstractQuestionSimilarityStrategy similarityStrategy,
            int processed,
            int total
    ) {
        try {
            return task.future().join();
        }
        catch (RuntimeException e) {
            logger.atWarn().setCause(e).addArgument(task.question() == null ? null : task.question().getId()).addArgument(processed).addArgument(total)
                  .log("Async duplicate-check task failed for question id={} at progress {}/{} - retrying on caller thread");
            return analyzeSingleQuestion(
                    task.question(),
                    allCourseQuestions,
                    similarityStrategy,
                    task.existingErrorPrefixes()
            );
        }
    }

    private CompletableFuture<QuestionDuplicateCheckResult> submitQuestionDuplicateCheck(
            Question question,
            List<Question> allCourseQuestions,
            AbstractQuestionSimilarityStrategy similarityStrategy,
            Set<String> existingErrorPrefixes
    ) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> analyzeSingleQuestion(
                            question,
                            allCourseQuestions,
                            similarityStrategy,
                            existingErrorPrefixes
                    ),
                    duplicateQuestionCheckTaskExecutor
            );
        }
        catch (RejectedExecutionException e) {
            logger.atInfo().setCause(e).addArgument(question == null ? null : question.getId())
                  .log("Duplicate-check task executor saturated for question id={} - running analysis on caller thread");
            return CompletableFuture.completedFuture(analyzeSingleQuestion(
                    question,
                    allCourseQuestions,
                    similarityStrategy,
                    existingErrorPrefixes
            ));
        }
    }

    private QuestionDuplicateCheckResult analyzeSingleQuestion(
            Question question, List<Question> allCourseQuestions, AbstractQuestionSimilarityStrategy similarityStrategy,
            Set<String> existingErrorPrefixes
    ) {
        if (question == null || MyUtil.SKIPPED_DUE_TO_ERROR.equals(question.getTitle())) {
            return null;
        }

        try {
            logger.atDebug().addArgument(Thread.currentThread().getName()).addArgument(question.getId()).log("Thread '{}' started duplicate analysis for question id={}");
            // Keep cache task-local to avoid shared mutable state across worker threads.
            LazyNormalizedQuestionCache candidateCache = new LazyNormalizedQuestionCache();
            Map<String, String> questionDetectedPairCauses = new LinkedHashMap<>();
            DuplicateAnalysis analysis = analyzeQuestionWithCache(
                    question,
                    allCourseQuestions,
                    candidateCache,
                    similarityStrategy,
                    questionDetectedPairCauses
            );
            logger.atDebug().addArgument(Thread.currentThread().getName()).addArgument(question.getId()).addArgument(analysis.duplicateMatchCount())
                  .log("Thread '{}' finished duplicate analysis for question id={} with {} matches");
            return new QuestionDuplicateCheckResult(
                    question,
                    analysis,
                    existingErrorPrefixes,
                    questionDetectedPairCauses
            );
        }
        catch (Exception e) {
            logger.atWarn().setCause(e).addArgument(question.getId()).addArgument(question.getTitle())
                  .log("Skipping duplicate analysis for question id={} title='{}' due to error - continuing with next question");
            return null;
        }
    }

    private Map<Long, Set<String>> buildExistingErrorPrefixMap(List<Question> authorQuestions, boolean ignoreExistingErrorPrefixes) {
        if (ignoreExistingErrorPrefixes || authorQuestions == null || authorQuestions.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<String>> existingErrorPrefixesByQuestionId = new HashMap<>();
        for (Question question : authorQuestions) {
            if (question != null && question.getId() != null) {
                existingErrorPrefixesByQuestionId.put(
                        question.getId(),
                        getExistingErrorPrefixes(question)
                );
            }
        }
        return existingErrorPrefixesByQuestionId;
    }

    /**
     * Extract existing error prefixes from a question as a Set for O(1) lookup.
     * Replaces the inefficient hasErrorWithDescriptionPrefix method.
     */
    private Set<String> getExistingErrorPrefixes(Question question) {
        Set<String> prefixes = new HashSet<>();
        if (question == null || question.getQuestionErrors() == null || question.getQuestionErrors().isEmpty()) {
            return prefixes;
        }

        for (QuestionError error : question.getQuestionErrors()) {
            collectErrorPrefix(
                    prefixes,
                    error
            );
        }
        return prefixes;
    }

    private void collectErrorPrefix(Set<String> prefixes, QuestionError error) {
        if (error == null || error.getDescription() == null) {
            return;
        }
        String description = error.getDescription();
        addPrefixIfMatched(
                prefixes,
                description,
                MyUtil.MISSING_ANSWER
        );
        addPrefixIfMatched(
                prefixes,
                description,
                MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS
        );
        addPrefixIfMatched(
                prefixes,
                description,
                MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS
        );
    }

    private void addPrefixIfMatched(Set<String> prefixes, String description, String prefix) {
        if (description.startsWith(prefix)) {
            prefixes.add(prefix);
        }
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionDuplicates(Long questionId) {
        Question question = questionRepository.findById(questionId).orElse(null);
        logger.debug("Fetching duplicates for question id={}, no duplicates {}, no errors {}",
                     questionId,
                     question != null ? question.getDuplicateLinks().size() : "N/A",
                     question != null ? question.getQuestionErrors().size() : "N/A");
        if (question == null) {
            return null;
        }

        QuestionDto dto = questionMapper.toDto(question);
        questionDtoEnricher.enrichWithErrors(
                dto,
                question
        );
        return dto;
    }

    @Transactional
    public void removeDuplicateAssociations(Long questionId) {
        if (questionId == null) {
            return;
        }

        List<QuestionDuplicate> links = questionDuplicateRepository.findByQuestionIdOrDuplicateQuestionId(
                questionId,
                questionId
        );
        Set<Long> affectedQuestionIds = new LinkedHashSet<>();
        affectedQuestionIds.add(questionId);
        for (QuestionDuplicate link : links) {
            if (link.getQuestion() != null && link.getQuestion().getId() != null) {
                affectedQuestionIds.add(link.getQuestion().getId());
            }
            if (link.getDuplicateQuestion() != null && link.getDuplicateQuestion().getId() != null) {
                affectedQuestionIds.add(link.getDuplicateQuestion().getId());
            }
        }

        long removedLinks = questionDuplicateRepository.deleteByQuestionIdOrDuplicateQuestionId(
                questionId,
                questionId
        );
        if (removedLinks > 0) {
            questionDuplicateRepository.flush();
        }

        cleanupDuplicateErrors(affectedQuestionIds);
    }

    @Transactional
    public void removeAllDuplicateAssociationsForQuestion(Long questionId) {
        if (questionId == null) {
            logger.atWarn().log("removeAllDuplicateAssociationsForQuestion called with null questionId");
            return;
        }
        List<QuestionDuplicate> links = questionDuplicateRepository.findByQuestionIdOrDuplicateQuestionId(
                questionId,
                questionId
        );
        Set<Long> affectedQuestionIds = new LinkedHashSet<>();
        for (QuestionDuplicate link : links) {
            affectedQuestionIds.add(link.getQuestion().getId());
            affectedQuestionIds.add(link.getDuplicateQuestion().getId());
        }
        affectedQuestionIds.remove(questionId);

        long removedLinks = questionDuplicateRepository.deleteByQuestionIdOrDuplicateQuestionId(
                questionId,
                questionId
        );
        if (removedLinks > 0) {
            questionDuplicateRepository.flush();
        }
        logger.atInfo().addArgument(questionId).addArgument(removedLinks).log("Removed {} links, {} total, for question {}");
        cleanupDuplicateErrors(affectedQuestionIds);
    }

    /**
     * Removes duplicate links only between {@code questionId} and each of the given
     * {@code duplicateIds}, leaving any other duplicate links for {@code questionId} intact.
     * Links can be stored with the pair in either column order, so both directions are attempted.
     */
    @Transactional
    public void removeSpecificDuplicateAssociations(Long questionId, List<Long> duplicateIds) {
        logger.atInfo().addArgument(questionId).addArgument(duplicateIds).log("Removing specific duplicate associations for question {}: {}");
        if (questionId == null || duplicateIds == null || duplicateIds.isEmpty()) {
            logger.atWarn().addArgument(questionId).log("removeSpecificDuplicateAssociations called with null questionId or empty duplicateIds for question {}");
            return;
        }

        Set<Long> affectedQuestionIds = new LinkedHashSet<>();
        affectedQuestionIds.add(questionId);
        long removedLinks = 0;
        for (Long duplicateId : duplicateIds) {
            if (duplicateId == null) {
                continue;
            }
            affectedQuestionIds.add(duplicateId);
            removedLinks += questionDuplicateRepository.deleteByQuestionIdAndDuplicateQuestionId(questionId, duplicateId);
            removedLinks += questionDuplicateRepository.deleteByQuestionIdAndDuplicateQuestionId(duplicateId, questionId);
        }

        if (removedLinks > 0) {
            questionDuplicateRepository.flush();
        }
        logger.atInfo().addArgument(removedLinks).addArgument(duplicateIds.size()).log("Removed {} of {} requested duplicate links");
        cleanupDuplicateErrors(affectedQuestionIds);
    }

    /**
     * Flags a duplicate link as resolved without deleting it, mirroring
     * {@code QuestionErrorService.resolveErrorById}.
     */
    @Transactional
    public void markDuplicateResolved(Long duplicateLinkId) {
        logger.atInfo().addArgument(duplicateLinkId).log("Marking duplicate link {} as resolved");
        if (duplicateLinkId == null) {
            logger.atWarn().log("markDuplicateResolved called with null duplicateLinkId");
            throw new IllegalArgumentException("Duplicate link ID cannot be null");
        }
        QuestionDuplicate link = questionDuplicateRepository.findById(duplicateLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Duplicate link not found with id: " + duplicateLinkId));

        String previousStatus = link.getStatus();
        if (ControllerSettings.DUPLICATE_STATUS_RESOLVED.equals(previousStatus)) {
            logger.atWarn().addArgument(duplicateLinkId).log("Duplicate link {} is already resolved");
        }
        link.setStatus(ControllerSettings.DUPLICATE_STATUS_RESOLVED);
        questionDuplicateRepository.save(link);
        logger.atInfo().addArgument(duplicateLinkId).addArgument(previousStatus).addArgument(ControllerSettings.DUPLICATE_STATUS_RESOLVED)
                .log("Duplicate link {} status transitioned from {} to {}");
    }

    // Direct QuestionError creation makes separate linking unnecessary.
    public void saveQuestionErrors(List<QuestionError> questionErrors) {
        if (questionErrors == null || questionErrors.isEmpty()) {
            return;
        }
        questionErrorRepository.saveAll(questionErrors);
    }

    @Transactional
    public DuplicateRecomputeSummary recomputeDuplicatesForCourse(String courseName) {
        return recomputeDuplicatesForCourseInternal(
                courseName,
                defaultSimilarityStrategy.getAlgorithmName()
        );
    }

    /**
     * Recompute duplicates for a specific list of questions (question bank or author scope).
     * Duplicates are detected by comparing within the provided list plus all course questions as context.
     *
     * @param courseName           the course these questions belong to
     * @param questionsToProcess   the questions to recompute duplicates for
     * @param similarityAlgorithm  the similarity algorithm name to use
     * @return DuplicateRecomputeSummary with results
     */
    @Transactional
    public DuplicateRecomputeSummary recomputeDuplicatesForQuestionList(
            String courseName,
            List<Question> questionsToProcess,
            String similarityAlgorithm) {
        AbstractQuestionSimilarityStrategy selectedStrategy = resolveStrategy(similarityAlgorithm);
        List<Question> safeQuestionsToProcess = questionsToProcess == null ? List.of() : questionsToProcess;
        return recomputeDuplicatesForQuestions(
                courseName,
                selectedStrategy,
                safeQuestionsToProcess,
                safeQuestionsToProcess.size()
        );
    }

    @Transactional
    public DuplicateRecomputeSummary recomputeDuplicatesForCourse(String courseName, String similarityAlgorithm) {
        return recomputeDuplicatesForCourseInternal(
                courseName,
                similarityAlgorithm
        );
    }

    private DuplicateRecomputeSummary recomputeDuplicatesForCourseInternal(String courseName, String similarityAlgorithm) {
        AbstractQuestionSimilarityStrategy selectedStrategy = resolveStrategy(similarityAlgorithm);

        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course cannot be null or empty");
        }

        List<Question> allCourseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                courseName,
                null,
                null,
                null
        ));
        return recomputeDuplicatesForQuestions(
                courseName,
                selectedStrategy,
                allCourseQuestions,
                allCourseQuestions.size()
        );
    }

    @Transactional
    public DuplicateRecomputeSummary recomputeDuplicatesForCourseSubset(String courseName, String similarityAlgorithm, int maxQuestions) {
        AbstractQuestionSimilarityStrategy selectedStrategy = resolveStrategy(similarityAlgorithm);

        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course cannot be null or empty");
        }
        if (maxQuestions <= 0) {
            throw new IllegalArgumentException("maxQuestions must be greater than 0");
        }

        List<Question> allCourseQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                courseName,
                null,
                null,
                null
        ));
        List<Question> selectedQuestions = allCourseQuestions.stream().filter(q -> q.getId() != null).sorted(Comparator.comparing(Question::getId)).limit(maxQuestions).toList();

        logger.atInfo().addArgument(selectedQuestions.size()).addArgument(maxQuestions).addArgument(courseName).log("Selected {} questions (max {}) for subset duplicate recompute on course '{}'");

        return recomputeDuplicatesForQuestions(
                courseName,
                selectedStrategy,
                selectedQuestions,
                allCourseQuestions.size()
        );
    }

    private DuplicateRecomputeSummary recomputeDuplicatesForQuestions(
            String courseName,
            AbstractQuestionSimilarityStrategy selectedStrategy,
            List<Question> questionsToProcess,
            int courseQuestionCount
    ) {
        List<Question> safeQuestionsToProcess = questionsToProcess == null ? List.of() : questionsToProcess;
        logger.atInfo().addArgument(courseName)
              .addArgument(selectedStrategy.getAlgorithmName())
              .addArgument(safeQuestionsToProcess.size())
              .log("Starting duplicate recompute for course '{}' using '{}' over {} questions");

        List<Question> multichoiceQuestions = safeQuestionsToProcess.stream().filter(question -> question.getType() == QuestionType.MULTICHOICE).toList();
        List<Question> truefalseQuestions = safeQuestionsToProcess.stream().filter(question -> question.getType() == QuestionType.TRUEFALSE).toList();

        List<Long> questionIds = safeQuestionsToProcess.stream().map(Question::getId).filter(Objects::nonNull).toList();
        DuplicateCleanupSummary cleanupSummary = clearDuplicateStateForQuestions(questionIds);

        logger.atInfo().addArgument(cleanupSummary.duplicateLinksRemoved()).addArgument(cleanupSummary.duplicateErrorsRemoved()).log("Duplicate cleanup finished: removed links={}, removed errors={}");

        List<QuestionError> createdErrors = new ArrayList<>();
        Set<String> persistedPairs = new HashSet<>();
        if (!multichoiceQuestions.isEmpty()) {
            logger.atInfo()
                  .addArgument(multichoiceQuestions.size())
                  .log("Recomputing duplicates for {} multichoice questions");
            createdErrors.addAll(detectDuplicatesForRecompute(
                    multichoiceQuestions,
                    multichoiceQuestions,
                    persistedPairs,
                    selectedStrategy
            ));
        }
        if (!truefalseQuestions.isEmpty()) {
            logger.atInfo().addArgument(truefalseQuestions.size()).log("Recomputing duplicates for {} true/false questions");
            createdErrors.addAll(detectDuplicatesForRecompute(
                    truefalseQuestions,
                    truefalseQuestions,
                    persistedPairs,
                    selectedStrategy
            ));
        }
        saveQuestionErrors(createdErrors);

        logger.atInfo().addArgument(courseName).addArgument(createdErrors.size()).addArgument(persistedPairs.size())
              .log("Duplicate recompute completed for course '{}': created errors={}, persisted pairs={}");

        return new DuplicateRecomputeSummary(
                courseQuestionCount,
                multichoiceQuestions.size(),
                truefalseQuestions.size(),
                cleanupSummary.duplicateLinksRemoved(),
                cleanupSummary.duplicateErrorsRemoved(),
                createdErrors.size()
        );
    }

    private DuplicateCleanupSummary clearDuplicateStateForQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return new DuplicateCleanupSummary(
                    0,
                    0
            );
        }

        int duplicateLinksRemoved = Math.toIntExact(
                questionDuplicateRepository.deleteByQuestionIdInOrDuplicateQuestionIdIn(
                questionIds,
                questionIds
        ));
        // Ensure deletes are applied before reinserting links in the same transaction.
        if (duplicateLinksRemoved > 0) {
            questionDuplicateRepository.flush();
        }

        long removedTitleErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(
                questionIds,
                MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS
        );
        long removedAnswerErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(
                questionIds,
                MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS
        );

        int duplicateErrorsRemoved = Math.toIntExact(removedTitleErrors + removedAnswerErrors);
        return new DuplicateCleanupSummary(
                duplicateLinksRemoved,
                duplicateErrorsRemoved
        );
    }

    /**
     * Optimized duplicate analysis using lazy-cached normalized data.
     */
    private DuplicateAnalysis analyzeQuestionWithCache(
            Question question, List<Question> allCourseQuestions, LazyNormalizedQuestionCache cache, AbstractQuestionSimilarityStrategy similarityStrategy,
            Map<String, String> detectedPairCauses
    ) {
        if (question == null || question.getType() == null) {
            return new DuplicateAnalysis(
                    false,
                    false,
                    false,
                    0
            );
        }

        return switch (question.getType()) {
            case MULTICHOICE -> {
                if (!hasAllMultichoiceAnswers(question)) {
                    yield new DuplicateAnalysis(
                            false,
                            false,
                            true,
                            0
                    );
                }
                yield analyzeMultichoiceWithCache(
                        question,
                        allCourseQuestions,
                        cache,
                        similarityStrategy,
                        detectedPairCauses
                );
            }
            case TRUEFALSE -> {
                if (!hasTrueFalseAnswer(question)) {
                    yield new DuplicateAnalysis(
                            false,
                            false,
                            true,
                            0
                    );
                }
                yield analyzeTrueFalseWithCache(
                        question,
                        allCourseQuestions,
                        cache,
                        similarityStrategy,
                        detectedPairCauses
                );
            }
            default -> {
                logger.atDebug().addArgument(question.getType()).log("Question type '{}' not recognized");
                yield new DuplicateAnalysis(
                        false,
                        false,
                        false,
                        0
                );
            }
        };
    }

    private DuplicateAnalysis analyzeMultichoiceWithCache(
            Question question, List<Question> allCourseQuestions, LazyNormalizedQuestionCache cache,
            AbstractQuestionSimilarityStrategy similarityStrategy, Map<String, String> detectedPairCauses
    ) {
        String normalizedTitle = normalize(question.getTitle());
        Set<String> questionAnswers = buildAnswerSetForQuestion(question);
        String[] sourceAnswerArray = questionAnswers.toArray(String[]::new);
        int[] sourceAnswerLengths = new int[sourceAnswerArray.length];
        for (int i = 0; i < sourceAnswerArray.length; i++) {
            sourceAnswerLengths[i] = sourceAnswerArray[i].length();
        }

        boolean hasTitleDuplicate = false;
        boolean hasAnswerDuplicate = false;
        int duplicateMatchCount = 0;

        for (Question candidate : allCourseQuestions) {
            if (!isPotentialDuplicateCandidateForMultichoice(
                    question,
                    candidate
            )) {
                continue;
            }

            boolean titleMatch = areTextsEquivalent(
                    normalizedTitle,
                    cache.getNormalizedTitle(candidate),
                    similarityStrategy
            );
            boolean answerMatch = cache.hasAnswerIntersection(
                    candidate,
                    question.getId(),
                    questionAnswers,
                    sourceAnswerArray,
                    sourceAnswerLengths,
                    similarityStrategy
            );

            if (titleMatch) {
                hasTitleDuplicate = true;
            }
            if (answerMatch) {
                hasAnswerDuplicate = true;
            }
            if (titleMatch || answerMatch) {
                String cause = buildMultichoiceDuplicateCause(
                        question,
                        candidate,
                        titleMatch,
                        answerMatch,
                        similarityStrategy
                );
                registerDuplicatePair(
                        detectedPairCauses,
                        question.getId(),
                        candidate.getId(),
                        cause
                );
                duplicateMatchCount++;
            }
        }

        return new DuplicateAnalysis(
                hasTitleDuplicate,
                hasAnswerDuplicate,
                false,
                duplicateMatchCount
        );
    }

    private DuplicateAnalysis analyzeTrueFalseWithCache(
            Question question, List<Question> allCourseQuestions, LazyNormalizedQuestionCache cache, AbstractQuestionSimilarityStrategy similarityStrategy,
            Map<String, String> detectedPairCauses
    ) {
        String normalizedTitle = normalize(question.getTitle());
        String normalizedText = normalize(question.getText());

        boolean hasTitleDuplicate = false;
        boolean hasAnswerDuplicate = false;
        int duplicateMatchCount = 0;

        for (Question candidate : allCourseQuestions) {
            if (candidate == null || candidate.getType() != question.getType() || !isPotentialDuplicateCandidateForTrueFalse(
                    question,
                    candidate
            )) {
                continue;
            }

            boolean titleMatch = areTextsEquivalent(
                    normalizedTitle,
                    cache.getNormalizedTitle(candidate),
                    similarityStrategy
            );
            boolean textMatch = areTextsEquivalent(
                    normalizedText,
                    cache.getNormalizedText(candidate),
                    similarityStrategy
            );

            if (titleMatch) {
                hasTitleDuplicate = true;
            }
            if (textMatch) {
                hasAnswerDuplicate = true;
            }
            if (titleMatch || textMatch) {
                String cause = buildTrueFalseDuplicateCause(
                        question,
                        candidate,
                        titleMatch,
                        textMatch,
                        similarityStrategy
                );
                registerDuplicatePair(
                        detectedPairCauses,
                        question.getId(),
                        candidate.getId(),
                        cause
                );
                duplicateMatchCount++;
            }
        }

        return new DuplicateAnalysis(
                hasTitleDuplicate,
                hasAnswerDuplicate,
                false,
                duplicateMatchCount
        );
    }

    private void registerDuplicatePair(Map<String, String> detectedPairCauses, Long sourceQuestionId, Long candidateQuestionId, String cause) {
        if (detectedPairCauses == null || sourceQuestionId == null || candidateQuestionId == null) {
            return;
        }
        String pairKey = toPairKey(
                sourceQuestionId,
                candidateQuestionId
        );
        String normalizedCause = sanitizeDuplicateCause((cause == null || cause.isBlank()) ? CAUSE_DEFAULT_DUPLICATE : cause);
        detectedPairCauses.putIfAbsent(
                pairKey,
                normalizedCause
        );
        detectedPairCauses.computeIfPresent(
                pairKey,
                (key, existingCause) -> mergeDuplicateCauses(
                        existingCause,
                        normalizedCause
                )
        );
    }

    private boolean isPotentialDuplicateCandidateForMultichoice(Question question, Question candidate) {
        if (candidate == null || candidate.getId() == null || question.getId() == null) {
            return false;
        }
        if (question.getType() == null || candidate.getType() != question.getType()) {
            return false;
        }
        if (candidate.getId().equals(question.getId())) {
            return false;
        }
        return !MyUtil.SKIPPED_DUE_TO_ERROR.equals(candidate.getTitle());
    }

    private boolean isPotentialDuplicateCandidateForTrueFalse(Question question, Question candidate) {
        return isPotentialDuplicateCandidateForMultichoice(
                question,
                candidate
        );
    }

    private Set<String> buildAnswerSetForQuestion(Question question) {
        Set<String> answers = HashSet.newHashSet(NUM_ANSWERS);
        addNormalizedAnswer(
                answers,
                question.getResponse1()
        );
        addNormalizedAnswer(
                answers,
                question.getResponse2()
        );
        addNormalizedAnswer(
                answers,
                question.getResponse3()
        );
        addNormalizedAnswer(
                answers,
                question.getResponse4()
        );
        return answers;
    }

    private void addNormalizedAnswer(Set<String> answers, String answer) {
        String normalized = normalize(answer);
        if (normalized != null) {
            answers.add(normalized);
        }
    }

    private QuestionError createQuestionError(Question question, String description) {
        return buildQuestionErrorForQuestion(
                question,
                description
        );
    }

    private int persistDuplicateLinks(Map<String, String> detectedPairCauses, Set<String> persistedPairs, boolean checkExistingLinks) {
        if (detectedPairCauses == null || detectedPairCauses.isEmpty()) {
            return 0;
        }

        List<String> orderedPairKeys = sortPairKeys(detectedPairCauses.keySet());
        List<String> pairsToPersist = collectPairsToPersist(
                orderedPairKeys,
                persistedPairs
        );

        if (pairsToPersist.isEmpty()) {
            return 0;
        }

        Set<Long> allRequiredIds = collectRequiredQuestionIds(pairsToPersist);
        Set<String> existingPairKeys = loadExistingPairKeys(
                allRequiredIds,
                checkExistingLinks
        );
        Map<Long, Question> batchLoadedQuestions = loadQuestionsById(allRequiredIds);
        List<QuestionDuplicate> linksToSave = buildLinksToSave(
                pairsToPersist,
                existingPairKeys,
                batchLoadedQuestions
        );

        if (linksToSave.isEmpty()) {
            return 0;
        }

        try {
            questionDuplicateRepository.saveAll(linksToSave);
            return linksToSave.size();
        }
        catch (DataIntegrityViolationException e) {
            logger.atDebug().setCause(e).addArgument(linksToSave.size()).log("Duplicate pairs already exist during batch save, attempted {} inserts");
            return 0;
        }
    }

    private List<String> sortPairKeys(Set<String> pairKeys) {
        return pairKeys.stream().sorted((left, right) -> {
            long[] leftPair = parsePairKey(left);
            long[] rightPair = parsePairKey(right);
            int lowerComparison = Long.compare(
                    leftPair[0],
                    rightPair[0]
            );
            return lowerComparison != 0 ? lowerComparison : Long.compare(
                    leftPair[1],
                    rightPair[1]
            );
        }).toList();
    }

    private List<String> collectPairsToPersist(List<String> orderedPairKeys, Set<String> persistedPairs) {
        List<String> pairsToPersist = new ArrayList<>(orderedPairKeys.size());
        for (String pairKey : orderedPairKeys) {
            if (persistedPairs == null || persistedPairs.add(pairKey)) {
                pairsToPersist.add(pairKey);
            }
        }
        return pairsToPersist;
    }

    private Set<Long> collectRequiredQuestionIds(List<String> pairsToPersist) {
        Set<Long> allRequiredIds = new LinkedHashSet<>();
        for (String pairKey : pairsToPersist) {
            long[] pairIds = parsePairKey(pairKey);
            allRequiredIds.add(pairIds[0]);
            allRequiredIds.add(pairIds[1]);
        }
        return allRequiredIds;
    }

    private Set<String> loadExistingPairKeys(Set<Long> allRequiredIds, boolean checkExistingLinks) {
        if (!checkExistingLinks) {
            return Set.of();
        }
        return questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(
                allRequiredIds,
                allRequiredIds
        ).stream().map(link -> toPairKey(
                link.getQuestion().getId(),
                link.getDuplicateQuestion().getId()
        )).collect(Collectors.toSet());
    }

    private Map<Long, Question> loadQuestionsById(Set<Long> questionIds) {
        Map<Long, Question> batchLoadedQuestions = new LinkedHashMap<>();
        Iterable<Question> loadedQuestionsIterable = questionRepository.findAllById(questionIds);
        for (Question question : loadedQuestionsIterable) {
            batchLoadedQuestions.put(
                    question.getId(),
                    question
            );
        }
        return batchLoadedQuestions;
    }

    private List<QuestionDuplicate> buildLinksToSave(
            List<String> pairsToPersist,
            Set<String> existingPairKeys,
            Map<Long, Question> batchLoadedQuestions
    ) {
        List<QuestionDuplicate> linksToSave = new ArrayList<>();
        for (String pairKey : pairsToPersist) {
            if (existingPairKeys.contains(pairKey)) {
                continue;
            }

            long[] pairIds = parsePairKey(pairKey);
            Question lowerQuestion = batchLoadedQuestions.get(pairIds[0]);
            Question higherQuestion = batchLoadedQuestions.get(pairIds[1]);

            if (lowerQuestion != null && higherQuestion != null) {
                QuestionDuplicate link = new QuestionDuplicate();
                link.setQuestion(lowerQuestion);
                link.setDuplicateQuestion(higherQuestion);
                linksToSave.add(link);
            }
        }
        return linksToSave;
    }

    private String toPairKey(Long left, Long right) {
        long lower = Math.min(
                left,
                right
        );
        long higher = Math.max(
                left,
                right
        );
        return lower + "-" + higher;
    }

    private long[] parsePairKey(String pairKey) {
        int separatorIndex = pairKey.indexOf('-');
        return new long[]{
                Long.parseLong(pairKey.substring(
                        0,
                        separatorIndex
                )),
                Long.parseLong(pairKey.substring(separatorIndex + 1))
        };
    }


    private void cleanupDuplicateErrors(Set<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<Long> cleanupCandidateIds = new ArrayList<>();
        for (Long questionId : questionIds) {
            if (isEligibleForDuplicateErrorCleanup(questionId)) {
                cleanupCandidateIds.add(questionId);
            }
        }

        if (cleanupCandidateIds.isEmpty()) {
            return;
        }

        long removedTitleErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(
                cleanupCandidateIds,
                MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS
        );
        long removedAnswerErrors = questionErrorRepository.deleteByQuestionIdInAndDescriptionStartingWith(
                cleanupCandidateIds,
                MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS
        );

        logger.atDebug().addArgument(cleanupCandidateIds.size()).addArgument(removedTitleErrors).addArgument(removedAnswerErrors)
              .log("Duplicate-error cleanup finished for {} questions; removed title errors={}, answer errors={}");
    }

    private boolean isEligibleForDuplicateErrorCleanup(Long questionId) {
        return questionId != null && questionDuplicateRepository.countByQuestionIdOrDuplicateQuestionId(
                questionId,
                questionId
        ) == 0;
    }

    private boolean isDuplicateErrorDescription(String description) {
        if (description == null) {
            return false;
        }
        return description.startsWith(MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS) || description.startsWith(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
    }

    public boolean hasDuplicateError(Question question) {
        if (question == null || question.getQuestionErrors() == null) {
            return false;
        }

        return question.getQuestionErrors().stream().map(QuestionError::getDescription).filter(Objects::nonNull).anyMatch(this::isDuplicateErrorDescription);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isSubstringMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    private boolean areTextsEquivalent(String left, String right, AbstractQuestionSimilarityStrategy similarityStrategy) {
        if (left == null || right == null) {
            return false;
        }
        if (isSubstringMatch(
                left,
                right
        )) {
            return true;
        }
        return similarityStrategy != null && similarityStrategy.isSimilar(
                left,
                right
        );
    }

    private String buildMultichoiceDuplicateCause(
            Question source,
            Question candidate,
            boolean titleMatch,
            boolean answerMatch,
            AbstractQuestionSimilarityStrategy similarityStrategy
    ) {
        List<String> causes = new ArrayList<>(2);
        if (titleMatch) {
            causes.add(buildFieldDuplicateCause(
                    CAUSE_FIELD_TITLE,
                    source.getTitle(),
                    candidate.getTitle(),
                    similarityStrategy
            ));
        }
        if (answerMatch) {
            causes.add(buildMultichoiceAnswerCause(
                    source,
                    candidate,
                    similarityStrategy
            ));
        }
        return mergeCauseParts(causes);
    }

    private String buildTrueFalseDuplicateCause(
            Question source,
            Question candidate,
            boolean titleMatch,
            boolean textMatch,
            AbstractQuestionSimilarityStrategy similarityStrategy
    ) {
        List<String> causes = new ArrayList<>(2);
        if (titleMatch) {
            causes.add(buildFieldDuplicateCause(
                    CAUSE_FIELD_TITLE,
                    source.getTitle(),
                    candidate.getTitle(),
                    similarityStrategy
            ));
        }
        if (textMatch) {
            causes.add(buildFieldDuplicateCause(
                    CAUSE_FIELD_TEXT,
                    source.getText(),
                    candidate.getText(),
                    similarityStrategy
            ));
        }
        return mergeCauseParts(causes);
    }

    private String buildMultichoiceAnswerCause(Question source, Question candidate, AbstractQuestionSimilarityStrategy similarityStrategy) {
        List<String> sourceAnswers = Arrays.asList(
                source.getResponse1(),
                source.getResponse2(),
                source.getResponse3(),
                source.getResponse4()
        );
        List<String> candidateAnswers = Arrays.asList(
                candidate.getResponse1(),
                candidate.getResponse2(),
                candidate.getResponse3(),
                candidate.getResponse4()
        );

        for (String sourceAnswer : sourceAnswers) {
            String normalizedSource = normalize(sourceAnswer);
            if (normalizedSource == null) {
                continue;
            }
            for (String candidateAnswer : candidateAnswers) {
                String normalizedCandidate = normalize(candidateAnswer);
                if (normalizedCandidate == null) {
                    continue;
                }
                if (isSubstringMatch(
                        normalizedSource,
                        normalizedCandidate
                )) {
                    return buildSubstringCause(
                            CAUSE_FIELD_ANSWER,
                            sourceAnswer,
                            candidateAnswer
                    );
                }
                if (similarityStrategy != null && similarityStrategy.isSimilar(
                        normalizedSource,
                        normalizedCandidate
                )) {
                    return buildSimilarityCause(
                            CAUSE_FIELD_ANSWER,
                            sourceAnswer,
                            candidateAnswer
                    );
                }
            }
        }

        return CAUSE_FIELD_ANSWER + " " + CAUSE_DEFAULT_SUFFIX;
    }

    private String buildFieldDuplicateCause(String fieldName, String sourceValue, String candidateValue, AbstractQuestionSimilarityStrategy similarityStrategy) {
        String normalizedSource = normalize(sourceValue);
        String normalizedCandidate = normalize(candidateValue);
        if (normalizedSource == null || normalizedCandidate == null) {
            return fieldName + " " + CAUSE_DEFAULT_SUFFIX;
        }
        if (isSubstringMatch(
                normalizedSource,
                normalizedCandidate
        )) {
            return buildSubstringCause(
                    fieldName,
                    sourceValue,
                    candidateValue
            );
        }
        if (similarityStrategy != null && similarityStrategy.isSimilar(
                normalizedSource,
                normalizedCandidate
        )) {
            return buildSimilarityCause(
                    fieldName,
                    sourceValue,
                    candidateValue
            );
        }
        return fieldName + " " + CAUSE_DEFAULT_SUFFIX;
    }

    private String buildSubstringCause(String fieldName, String sourceValue, String candidateValue) {
        String source = safeCauseValue(sourceValue);
        String candidate = safeCauseValue(candidateValue);
        String normalizedSource = normalize(source);
        String normalizedCandidate = normalize(candidate);

        if (normalizedSource == null || normalizedCandidate == null) {
            return fieldName + ": " + CAUSE_DEFAULT_SUFFIX;
        }
        if (normalizedSource.contains(normalizedCandidate) && !normalizedSource.equals(normalizedCandidate)) {
            return CAUSE_SUBSTRING_TEMPLATE.formatted(
                    fieldName,
                    candidate,
                    source
            );
        }
        if (normalizedCandidate.contains(normalizedSource) && !normalizedSource.equals(normalizedCandidate)) {
            return CAUSE_SUBSTRING_TEMPLATE.formatted(
                    fieldName,
                    source,
                    candidate
            );
        }
        return CAUSE_EXACT_TEMPLATE.formatted(
                fieldName,
                source,
                candidate
        );
    }

    private String buildSimilarityCause(String fieldName, String sourceValue, String candidateValue) {
        return CAUSE_SIMILAR_TEMPLATE.formatted(
                fieldName,
                safeCauseValue(sourceValue),
                safeCauseValue(candidateValue)
        );
    }

    private String safeCauseValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String mergeCauseParts(List<String> causeParts) {
        if (causeParts == null || causeParts.isEmpty()) {
            return CAUSE_DEFAULT_DUPLICATE;
        }
        return causeParts.stream()
                         .filter(Objects::nonNull)
                         .map(String::trim)
                         .filter(part -> !part.isEmpty())
                         .distinct()
                         .sorted(this::compareCauseParts)
                         .collect(Collectors.joining(CAUSE_PART_DELIMITER));
    }

    private String mergeDuplicateCauses(String existing, String incoming) {
        if (existing == null || existing.isBlank()) {
            return sanitizeDuplicateCause(incoming);
        }
        if (incoming == null || incoming.isBlank()) {
            return sanitizeDuplicateCause(existing);
        }

        LinkedHashSet<String> parts = new LinkedHashSet<>();
        addCauseParts(
                parts,
                existing
        );
        addCauseParts(
                parts,
                incoming
        );

        String merged = parts.stream().sorted(this::compareCauseParts).collect(Collectors.joining(CAUSE_PART_DELIMITER));
        return sanitizeDuplicateCause(merged);
    }

    private void addCauseParts(Set<String> parts, String cause) {
        if (parts == null || cause == null || cause.isBlank()) {
            return;
        }
        for (String part : cause.split(CAUSE_PART_SPLIT_REGEX)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
    }

    private int compareCauseParts(String left, String right) {
        int leftPriority = causePriority(left);
        int rightPriority = causePriority(right);
        if (leftPriority != rightPriority) {
            return Integer.compare(
                    leftPriority,
                    rightPriority
            );
        }
        String safeLeft = left == null ? "" : left;
        String safeRight = right == null ? "" : right;
        return safeLeft.compareToIgnoreCase(safeRight);
    }

    private int causePriority(String causePart) {
        if (causePart == null || causePart.isBlank()) {
            return CAUSE_PRIORITY_GENERIC;
        }
        String normalized = causePart.toLowerCase(Locale.ROOT);
        if (normalized.contains("matches exactly")) {
            return CAUSE_PRIORITY_EXACT;
        }
        if (normalized.contains("found as substring")) {
            return CAUSE_PRIORITY_SUBSTRING;
        }
        if (normalized.contains("considered similar")) {
            return CAUSE_PRIORITY_SIMILAR;
        }
        return CAUSE_PRIORITY_GENERIC;
    }

    private String sanitizeDuplicateCause(String cause) {
        if (cause == null) {
            return null;
        }
        String trimmed = cause.trim();
        if (trimmed.length() <= DUPLICATE_CAUSE_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(
                0,
                DUPLICATE_CAUSE_MAX_LENGTH
        );
    }

    private record DuplicateCleanupSummary(int duplicateLinksRemoved, int duplicateErrorsRemoved) {
    }

    private record DuplicateAnalysis(boolean hasTitleDuplicate, boolean hasAnswerDuplicate, boolean hasMissingAnswer, int duplicateMatchCount) {
    }

    private record QuestionDuplicateCheckResult(
            Question question, DuplicateAnalysis analysis, Set<String> existingErrorPrefixes, Map<String, String> detectedPairCauses
    ) {
    }

    private record DuplicateCheckTask(
            Question question,
            Set<String> existingErrorPrefixes,
            CompletableFuture<QuestionDuplicateCheckResult> future
    ) {
    }

    private record DuplicateExecutionPlan(int total, int progressStep, int totalBatches) {
    }

    public record DuplicateRecomputeSummary(int totalQuestions, int multichoiceQuestions, int truefalseQuestions, int duplicateLinksRemoved, int duplicateErrorsRemoved, int duplicateErrorsCreated) {
    }

    /**
     * Lightweight on-demand cache that normalizes strings only when accessed.
     * Avoids expensive upfront cache construction.
     */
    private class LazyNormalizedQuestionCache {
        private final Map<Long, String> titleCache = new HashMap<>();
        private final Map<Long, String> textCache = new HashMap<>();
        private final Map<Long, Set<String>> answerSetCache = new HashMap<>();

        public String getNormalizedTitle(Question question) {
            if (question == null || question.getId() == null) {
                return null;
            }
            Long id = question.getId();
            return titleCache.computeIfAbsent(
                    id,
                    k -> normalize(question.getTitle())
            );
        }

        public String getNormalizedText(Question question) {
            if (question == null || question.getId() == null) {
                return null;
            }
            Long id = question.getId();
            return textCache.computeIfAbsent(
                    id,
                    k -> normalize(question.getText())
            );
        }

        public boolean hasAnswerIntersection(
                Question candidate, Long sourceQuestionId, Set<String> sourceAnswers, String[] sourceAnswerArray, int[] sourceAnswerLengths,
                AbstractQuestionSimilarityStrategy similarityStrategy
        ) {
            Long candidateId = candidate == null ? null : candidate.getId();
            if (candidateId == null || sourceAnswers == null || sourceAnswers.isEmpty()) {
                return false;
            }
            // Explicit self-exclusion: never compare a question's answers against itself.
            if (candidateId.equals(sourceQuestionId)) {
                return false;
            }

            Set<String> candidateAnswers = answerSetCache.computeIfAbsent(
                    candidateId,
                    k -> buildAnswerSetForQuestion(candidate)
            );
            if (candidateAnswers.isEmpty()) {
                return false;
            }

            if (hasExactAnswerIntersection(candidateAnswers, sourceAnswers)) {
                return true;
            }

            if (sourceAnswerArray == null || sourceAnswerLengths == null || sourceAnswerArray.length != sourceAnswerLengths.length) {
                return false;
            }

            return hasSubstringOrSimilarAnswerMatch(
                    candidateAnswers,
                    sourceAnswerArray,
                    sourceAnswerLengths,
                    similarityStrategy
            );
        }

        private boolean hasExactAnswerIntersection(Set<String> candidateAnswers, Set<String> sourceAnswers) {
            // Fast path: exact-answer intersection via hash lookups.
            Set<String> smaller = candidateAnswers.size() <= sourceAnswers.size() ? candidateAnswers : sourceAnswers;
            Set<String> larger = smaller == candidateAnswers ? sourceAnswers : candidateAnswers;
            for (String answer : smaller) {
                if (larger.contains(answer)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasSubstringOrSimilarAnswerMatch(
                Set<String> candidateAnswers,
                String[] sourceAnswerArray,
                int[] sourceAnswerLengths,
                AbstractQuestionSimilarityStrategy similarityStrategy
        ) {
            for (String candidateAnswer : candidateAnswers) {
                if (candidateAnswer == null) {
                    continue;
                }
                int candidateLength = candidateAnswer.length();
                for (int i = 0; i < sourceAnswerArray.length; i++) {
                    String sourceAnswer = sourceAnswerArray[i];
                    if (sourceAnswer == null) {
                        continue;
                    }
                    int sourceLength = sourceAnswerLengths[i];
                    if (hasSubstringOverlap(
                            candidateAnswer,
                            sourceAnswer,
                            candidateLength,
                            sourceLength
                    )) {
                        return true;
                    }
                    if (isSimilar(
                            similarityStrategy,
                            candidateAnswer,
                            sourceAnswer
                    )) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean hasSubstringOverlap(String candidateAnswer, String sourceAnswer, int candidateLength, int sourceLength) {
            if (candidateLength >= sourceLength) {
                return candidateAnswer.contains(sourceAnswer);
            }
            return sourceAnswer.contains(candidateAnswer);
        }

        private boolean isSimilar(AbstractQuestionSimilarityStrategy similarityStrategy, String left, String right) {
            return similarityStrategy != null && similarityStrategy.isSimilar(
                    left,
                    right
            );
        }
    }

}
