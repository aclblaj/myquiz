package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.QuestionDto;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Service
public class QuestionDuplicationService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionDuplicationService.class);
    private static final int DUPLICATE_CHECK_BATCH_SIZE = 250;

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
            @Value("${myquiz.duplicates.similarity.algorithm:levenshtein}") String defaultSimilarityAlgorithm,
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
                        new LevenshteinQuestionSimilarityStrategy(),
                        new JaroWinklerQuestionSimilarityStrategy()
                ),
                "levenshtein",
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
        if (!strategyMap.containsKey("levenshtein")) {
            strategyMap.put(
                    "levenshtein",
                    new LevenshteinQuestionSimilarityStrategy()
            );
        }
        if (!strategyMap.containsKey("jaro-winkler")) {
            strategyMap.put(
                    "jaro-winkler",
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
        logger.atWarn().addArgument(algorithmName).log("Unknown duplicate similarity algorithm '{}', falling back to levenshtein");
        return similarityStrategies.get("levenshtein");
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
    public List<QuestionError> checkDuplicateQuestionsForAuthors(ArrayList<Author> authors, String course) {
        if (authors == null || authors.isEmpty()) {
            logger.atWarn().log("No authors provided for duplicate checking");
            return new ArrayList<>();
        }
        if (course == null || course.isBlank()) {
            logger.atWarn().log("No course provided for duplicate checking");
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
                    true,
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
        if (uploadedQuestions == null || uploadedQuestions.isEmpty()) {
            logger.atWarn().log("No uploaded questions provided for duplicate checking");
            return new ArrayList<>();
        }
        if (course == null || course.isBlank()) {
            logger.atWarn().log("No course provided for duplicate checking");
            return new ArrayList<>();
        }

        List<Long> courseQuestionIds = allCourseQuestions == null ? List.of() : allCourseQuestions.stream().map(Question::getId).filter(Objects::nonNull).toList();
        DuplicateCleanupSummary cleanupSummary = clearDuplicateStateForQuestions(courseQuestionIds);
        logger.atInfo().addArgument(course).addArgument(cleanupSummary.duplicateLinksRemoved()).addArgument(cleanupSummary.duplicateErrorsRemoved())
              .log("Pre-check duplicate cleanup for course '{}': removed links={}, removed errors={}");

        logger.atInfo().addArgument(uploadedQuestions.size()).addArgument(course).addArgument(allCourseQuestions.size())
              .log("Checking {} uploaded questions for duplicates in course '{}' against {} existing questions");

        List<QuestionError> questionErrors = detectDuplicates(
                uploadedQuestions,
                allCourseQuestions
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
            logger.atWarn().log("No course provided for duplicate checking");
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

        return checkDuplicatesInCourse(
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
        return detectDuplicates(
                authorQuestions,
                allCourseQuestions,
                true,
                new HashSet<>()
        );
    }

    private List<QuestionError> detectDuplicates(List<Question> authorQuestions, List<Question> allCourseQuestions, boolean checkExistingLinks) {
        return detectDuplicates(
                authorQuestions,
                allCourseQuestions,
                checkExistingLinks,
                false,
                new HashSet<>()
        );
    }

    private List<QuestionError> detectDuplicates(List<Question> authorQuestions, List<Question> allCourseQuestions, boolean checkExistingLinks, Set<String> persistedPairs) {
        return detectDuplicates(
                authorQuestions,
                allCourseQuestions,
                checkExistingLinks,
                false,
                persistedPairs
        );
    }

    private List<QuestionError> detectDuplicates(
            List<Question> authorQuestions, List<Question> allCourseQuestions, boolean checkExistingLinks, boolean ignoreExistingErrorPrefixes,
            Set<String> persistedPairs
    ) {
        return detectDuplicates(
                authorQuestions,
                allCourseQuestions,
                checkExistingLinks,
                ignoreExistingErrorPrefixes,
                persistedPairs,
                defaultSimilarityStrategy
        );
    }

    private List<QuestionError> detectDuplicates(
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

        int total = authorQuestions.size();
        int processed = 0;
        int progressStep = Math.max(
                1,
                total / 5
        );
        int totalBatches = (total + DUPLICATE_CHECK_BATCH_SIZE - 1) / DUPLICATE_CHECK_BATCH_SIZE;

        logger.atInfo().addArgument(total).addArgument(DUPLICATE_CHECK_BATCH_SIZE).addArgument(totalBatches)
              .log("Submitting duplicate checks for {} questions in sequential batches of {} ({} batches)");

        for (int start = 0, batchIndex = 1; start < total; start += DUPLICATE_CHECK_BATCH_SIZE, batchIndex++) {
            int end = Math.min(
                    start + DUPLICATE_CHECK_BATCH_SIZE,
                    total
            );
            List<Question> batch = authorQuestions.subList(
                    start,
                    end
            );
            Set<String> batchDetectedPairKeys = new LinkedHashSet<>();

            logger.atDebug().addArgument(batchIndex).addArgument(totalBatches).addArgument(batch.size()).log("Starting duplicate-check batch {}/{} with {} questions");

            List<DuplicateCheckTask> tasks = batch.stream().map(question -> {
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

            for (DuplicateCheckTask task : tasks) {
                processed++;
                QuestionDuplicateCheckResult result = resolveTaskResult(
                        task,
                        allCourseQuestions,
                        similarityStrategy,
                        processed,
                        total
                );

                if (result == null || result.question() == null || result.analysis() == null) {
                    continue;
                }

                batchDetectedPairKeys.addAll(result.detectedPairKeys());

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

                if (processed % progressStep == 0 || processed == total) {
                    logger.atInfo().addArgument(processed).addArgument(total).addArgument(questionErrors.size()).log("Duplicate checks progress: {}/{} questions processed, {} errors collected");
                }
            }

            int persistedInBatch = persistDuplicateLinks(
                    batchDetectedPairKeys,
                    persistedPairs,
                    checkExistingLinks
            );

            logger.atDebug().addArgument(batchIndex).addArgument(totalBatches).addArgument(batchDetectedPairKeys.size()).addArgument(persistedInBatch)
                  .log("Finished duplicate-check batch {}/{}; detected pair keys={}, persisted links={}");

            if (batchIndex < totalBatches) {
                logger.atDebug().addArgument(batchIndex + 1).addArgument(totalBatches).log("Starting next batch ({}/{}) after previous batch persistence completed");
            }
        }

        return questionErrors;
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
            Set<String> questionDetectedPairKeys = new HashSet<>();
            DuplicateAnalysis analysis = analyzeQuestionWithCache(
                    question,
                    allCourseQuestions,
                    candidateCache,
                    similarityStrategy,
                    questionDetectedPairKeys
            );
            logger.atDebug().addArgument(Thread.currentThread().getName()).addArgument(question.getId()).addArgument(analysis.duplicateMatchCount())
                  .log("Thread '{}' finished duplicate analysis for question id={} with {} matches");
            return new QuestionDuplicateCheckResult(
                    question,
                    analysis,
                    existingErrorPrefixes,
                    questionDetectedPairKeys
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
        if (question.getQuestionErrors() != null) {
            for (QuestionError error : question.getQuestionErrors()) {
                String description = error.getDescription();
                if (description != null) {
                    // Store the error prefix for quick lookup
                    if (description.startsWith(MyUtil.MISSING_ANSWER)) {
                        prefixes.add(MyUtil.MISSING_ANSWER);
                    }
                    if (description.startsWith(MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS)) {
                        prefixes.add(MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
                    }
                    if (description.startsWith(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS)) {
                        prefixes.add(MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
                    }
                }
            }
        }
        return prefixes;
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionDuplicates(Long questionId) {
        Question question = questionRepository.findById(questionId).orElse(null);
        if (question == null) {
            return null;
        }

        QuestionDto baseQuestion = questionMapper.toDto(question);
        questionDtoEnricher.enrichWithErrors(
                baseQuestion,
                question
        );
        return baseQuestion;
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
        cleanupDuplicateErrors(affectedQuestionIds);
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
        return recomputeDuplicatesForCourse(
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
            createdErrors.addAll(detectDuplicates(
                    multichoiceQuestions,
                    multichoiceQuestions,
                    false,
                    true,
                    persistedPairs,
                    selectedStrategy
            ));
        }
        if (!truefalseQuestions.isEmpty()) {
            logger.atInfo().addArgument(truefalseQuestions.size()).log("Recomputing duplicates for {} true/false questions");
            createdErrors.addAll(detectDuplicates(
                    truefalseQuestions,
                    truefalseQuestions,
                    false,
                    true,
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
            Set<String> detectedPairKeys
    ) {
        if (question == null || question.getType() == null) {
            return new DuplicateAnalysis(
                    false,
                    false,
                    false,
                    0
            );
        }

        switch (question.getType()) {
            case MULTICHOICE:
                if (!hasAllMultichoiceAnswers(question)) {
                    return new DuplicateAnalysis(
                            false,
                            false,
                            true,
                            0
                    );
                }
                return analyzeMultichoiceWithCache(
                        question,
                        allCourseQuestions,
                        cache,
                        similarityStrategy,
                        detectedPairKeys
                );
            case TRUEFALSE:
                if (!hasTrueFalseAnswer(question)) {
                    return new DuplicateAnalysis(
                            false,
                            false,
                            true,
                            0
                    );
                }
                return analyzeTrueFalseWithCache(
                        question,
                        allCourseQuestions,
                        cache,
                        similarityStrategy,
                        detectedPairKeys
                );
            default:
                logger.atDebug().addArgument(question.getType()).log("Question type '{}' not recognized");
                return new DuplicateAnalysis(
                        false,
                        false,
                        false,
                        0
                );
        }
    }

    private DuplicateAnalysis analyzeMultichoiceWithCache(
            Question question, List<Question> allCourseQuestions, LazyNormalizedQuestionCache cache,
            AbstractQuestionSimilarityStrategy similarityStrategy, Set<String> detectedPairKeys
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
                registerDuplicatePair(
                        detectedPairKeys,
                        question.getId(),
                        candidate.getId()
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
            Set<String> detectedPairKeys
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
                registerDuplicatePair(
                        detectedPairKeys,
                        question.getId(),
                        candidate.getId()
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

    private void registerDuplicatePair(Set<String> detectedPairKeys, Long sourceQuestionId, Long candidateQuestionId) {
        if (detectedPairKeys == null || sourceQuestionId == null || candidateQuestionId == null) {
            return;
        }
        detectedPairKeys.add(toPairKey(
                sourceQuestionId,
                candidateQuestionId
        ));
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
        Set<String> answers = new HashSet<>(4);
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

    private int persistDuplicateLinks(Set<String> detectedPairKeys, Set<String> persistedPairs, boolean checkExistingLinks) {
        if (detectedPairKeys == null || detectedPairKeys.isEmpty()) {
            return 0;
        }

        List<String> orderedPairKeys = detectedPairKeys.stream().sorted((left, right) -> {
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
        List<String> pairsToPersist = new ArrayList<>(orderedPairKeys.size());
        for (String pairKey : orderedPairKeys) {
            if (persistedPairs == null || persistedPairs.add(pairKey)) {
                pairsToPersist.add(pairKey);
            }
        }

        if (pairsToPersist.isEmpty()) {
            return 0;
        }

        Set<Long> allRequiredIds = new LinkedHashSet<>();
        for (String pairKey : pairsToPersist) {
            long[] pairIds = parsePairKey(pairKey);
            allRequiredIds.add(pairIds[0]);
            allRequiredIds.add(pairIds[1]);
        }

        Set<String> existingPairKeys = Set.of();
        if (checkExistingLinks) {
            existingPairKeys = questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(
                    allRequiredIds,
                    allRequiredIds
            ).stream().map(link -> toPairKey(
                    link.getQuestion().getId(),
                    link.getDuplicateQuestion().getId()
            )).collect(Collectors.toSet());
        }

        Map<Long, Question> batchLoadedQuestions = new LinkedHashMap<>();
        Iterable<Question> loadedQuestionsIterable = questionRepository.findAllById(allRequiredIds);
        for (Question q : loadedQuestionsIterable) {
            batchLoadedQuestions.put(
                    q.getId(),
                    q
            );
        }

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
            if (questionId == null) {
                continue;
            }
            if (questionDuplicateRepository.countByQuestionIdOrDuplicateQuestionId(
                    questionId,
                    questionId
            ) > 0) {
                continue;
            }
            cleanupCandidateIds.add(questionId);
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

    private record DuplicateCleanupSummary(int duplicateLinksRemoved, int duplicateErrorsRemoved) {
    }

    private record DuplicateAnalysis(boolean hasTitleDuplicate, boolean hasAnswerDuplicate, boolean hasMissingAnswer, int duplicateMatchCount) {
    }

    private record QuestionDuplicateCheckResult(
            Question question, DuplicateAnalysis analysis, Set<String> existingErrorPrefixes, Set<String> detectedPairKeys
    ) {
    }

    private record DuplicateCheckTask(
            Question question,
            Set<String> existingErrorPrefixes,
            CompletableFuture<QuestionDuplicateCheckResult> future
    ) {
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
            if (candidateId == null || sourceAnswers.isEmpty()) {
                return false;
            }
            // Explicit self-exclusion: never compare a question's answers against itself.
            if (candidateId.equals(sourceQuestionId)) {
                return false;
            }

            Set<String> candidateAnswers = answerSetCache.get(candidateId);
            if (candidateAnswers == null) {
                candidateAnswers = buildAnswerSetForQuestion(candidate);
                answerSetCache.put(
                        candidateId,
                        candidateAnswers
                );
            }
            if (candidateAnswers.isEmpty()) {
                return false;
            }

            // Fast path: exact-answer intersection via hash lookups.
            Set<String> smaller = candidateAnswers.size() <= sourceAnswers.size() ? candidateAnswers : sourceAnswers;
            Set<String> larger = smaller == candidateAnswers ? sourceAnswers : candidateAnswers;
            for (String answer : smaller) {
                if (larger.contains(answer)) {
                    return true;
                }
            }

            // Fallback: bidirectional substring check with precomputed source lengths.
            for (String candidateAnswer : candidateAnswers) {
                int candidateLength = candidateAnswer.length();
                for (int i = 0; i < sourceAnswerArray.length; i++) {
                    String sourceAnswer = sourceAnswerArray[i];
                    int sourceLength = sourceAnswerLengths[i];
                    if (candidateLength >= sourceLength) {
                        if (candidateAnswer.contains(sourceAnswer)) {
                            return true;
                        }
                    } else if (sourceAnswer.contains(candidateAnswer)) {
                        return true;
                    }
                    if (similarityStrategy != null && similarityStrategy.isSimilar(
                            candidateAnswer,
                            sourceAnswer
                    )) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}

