package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.entities.QuizError;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionValidationService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionValidationService.class);

    private final QuestionRepository questionRepository;
    private final QuestionErrorRepository questionErrorRepository;
    private final QuestionService questionService;

    @Autowired
    public QuestionValidationService(
            QuestionRepository questionRepository,
            QuestionErrorRepository questionErrorRepository,
            QuestionService questionService) {
        this.questionService = questionService;
        this.questionErrorRepository = questionErrorRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Check for duplicate questions between author's questions and all existing questions in the database
     * for the same course.
     *
     * This method validates that questions submitted by authors don't overlap with existing questions
     * in the database for the same course. It checks both question titles and answers for duplicates.
     *
     * @param authors List of authors whose questions should be validated
     * @param course Course name to filter questions
     * @return List of QuizError objects representing duplicate violations
     */
    public List<QuizError> checkDuplicatesQuestionsForAuthors(ArrayList<Author> authors, String course) {
        List<QuizError> allErrors = new ArrayList<>();

        // Load all existing questions for the course ONCE (optimization)
        List<Question> allCourseQuestions = questionRepository.findAll(
            QuestionSpecification.byFilters(course, null, null, null));

        // Build lookup maps for efficient duplicate detection
        List<String> existingTitles = buildTitleList(allCourseQuestions);
        List<String> existingAnswers = buildAnswerList(allCourseQuestions);

        logger.atInfo()
              .addArgument(course)
              .addArgument(allCourseQuestions.size())
              .log("Checking duplicates for course '{}' against {} existing questions");

        // Check each author's questions
        for (Author author : authors) {
            List<Question> authorQuestions = questionService.getQuestionsForAuthorId(author.getId(), course);
            List<QuizError> quizErrors = detectDuplicates(authorQuestions, existingTitles, existingAnswers);
            allErrors.addAll(quizErrors);

            logger.atInfo()
                  .addArgument(author.getName())
                  .addArgument(authorQuestions.size())
                  .addArgument(quizErrors.size())
                  .log("{} - no. questions: {}, no. duplicates found: {}");
        }

        return allErrors;
    }

    /**
     * Build list of all titles from questions (lowercase for case-insensitive comparison).
     */
    private List<String> buildTitleList(List<Question> questions) {
        List<String> titles = new ArrayList<>();
        for (Question question : questions) {
            if (question.getTitle() != null && !question.getTitle().equals(MyUtil.SKIPPED_DUE_TO_ERROR)) {
                titles.add(question.getTitle().toLowerCase());
            }
        }
        return titles;
    }

    /**
     * Build list of all answers from questions (lowercase for case-insensitive comparison).
     */
    private List<String> buildAnswerList(List<Question> questions) {
        List<String> answers = new ArrayList<>();
        for (Question question : questions) {
            if (question.getResponse1() != null) answers.add(question.getResponse1().toLowerCase());
            if (question.getResponse2() != null) answers.add(question.getResponse2().toLowerCase());
            if (question.getResponse3() != null) answers.add(question.getResponse3().toLowerCase());
            if (question.getResponse4() != null) answers.add(question.getResponse4().toLowerCase());
        }
        return answers;
    }

    /**
     * Detect duplicates by comparing author's questions against existing questions in the database.
     * Now preserves question titles instead of marking them as SKIPPED_DUE_TO_ERROR.
     * Note: QuestionError links are created separately after QuizErrors are persisted.
     *
     * @param authorQuestions Questions to validate
     * @param existingTitles All existing question titles in the course
     * @param existingAnswers All existing answers in the course
     * @return List of errors for duplicate violations
     */
    private List<QuizError> detectDuplicates(List<Question> authorQuestions,
                                             List<String> existingTitles,
                                             List<String> existingAnswers) {
        List<QuizError> quizErrors = new ArrayList<>();

        for (Question question : authorQuestions) {
            // Skip already marked errors (legacy check)
            if (MyUtil.SKIPPED_DUE_TO_ERROR.equals(question.getTitle())) {
                continue;
            }

            QuizError quizError = checkQuestionForDuplicates(question, existingTitles, existingAnswers);
            if (quizError != null) {
                quizError.setQuizAuthor(question.getQuizAuthor());
                quizErrors.add(quizError);
            }
        }

        return quizErrors;
    }

    /**
     * Link saved QuizErrors to their corresponding Questions via QuestionError junction table.
     * This must be called AFTER QuizErrors are persisted to the database.
     *
     * @param quizErrors The list of saved QuizErrors (must have IDs)
     */
    public void linkErrorsToQuestions(List<QuizError> quizErrors) {
        if (quizErrors == null || quizErrors.isEmpty()) {
            return;
        }

        int linkedCount = 0;
        for (QuizError quizError : quizErrors) {
            // Find the question by row number and quiz author
            if (quizError.getRowNumber() != null && quizError.getQuizAuthor() != null) {
                // Get questions for this quiz author
                List<Question> questions = questionRepository.findAll(
                    QuestionSpecification.byQuizAuthorId(quizError.getQuizAuthor().getId())
                );

                // Find the question with matching row number
                for (Question question : questions) {
                    if (question.getCrtNo() == quizError.getRowNumber()) {
                        // Create the link directly - the unique constraint will prevent duplicates
                        try {
                            QuestionError questionError = new QuestionError(question, quizError);
                            questionErrorRepository.save(questionError);
                            linkedCount++;
                            logger.atDebug()
                                  .addArgument(question.getId())
                                  .addArgument(quizError.getId())
                                  .log("Linked question {} to error {}");
                        } catch (Exception e) {
                            // Duplicate key or other constraint violation - skip silently
                            logger.atDebug()
                                  .addArgument(question.getId())
                                  .addArgument(quizError.getId())
                                  .log("Link already exists between question {} and error {}");
                        }
                        break;
                    }
                }
            }
        }

        logger.atInfo()
              .addArgument(linkedCount)
              .addArgument(quizErrors.size())
              .log("Linked {} errors to their questions (out of {} total errors)");
    }

    /**
     * Check a single question for duplicates against existing questions.
     *
     * Validates that:
     * 1. Question title doesn't already exist in the database
     * 2. Question answers don't already exist in the database
     * 3. All required answers are present (for multichoice questions)
     *
     * @param question Question to validate
     * @param existingTitles List of all existing titles in the course
     * @param existingAnswers List of all existing answers in the course
     * @return QuizError if duplicate found, null otherwise
     */
    private QuizError checkQuestionForDuplicates(Question question,
                                                  List<String> existingTitles,
                                                  List<String> existingAnswers) {
        switch (question.getType()) {
            case MULTICHOICE:
                if (hasAllAnswers(question)) {
                    // Check if any answer already exists in database
                    if (checkAnswersAgainstExisting(question, existingAnswers)) {
                        QuizError error = getAuthorError(question, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
                        // NO LONGER mark title as skipped - preserve the original title
                        return error;
                    }
                    // Check if title already exists in database
                    if (checkTitleAgainstExisting(question, existingTitles)) {
                        QuizError error = getAuthorError(question, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
                        // NO LONGER mark title as skipped - preserve the original title
                        return error;
                    }
                } else {
                    QuizError error = getAuthorError(question, MyUtil.MISSING_ANSWER);
                    // NO LONGER mark title as skipped - preserve the original title
                    return error;
                }
                break;
            case TRUEFALSE:
                logger.atDebug().addArgument(question.getTitle())
                      .log("True/false question '{}' validation skipped (not checked for duplicates)");
                break;
            default:
                logger.atDebug().addArgument(question.getType())
                      .log("Question type '{}' not recognized");
                break;
        }
        return null;
    }

    /**
     * Check if any of the question's answers already exist in the database.
     *
     * @param question Question to check
     * @param existingAnswers All existing answers in the course
     * @return true if at least one answer is a duplicate
     */
    private boolean checkAnswersAgainstExisting(Question question, List<String> existingAnswers) {
        String r1 = question.getResponse1() != null ? question.getResponse1().toLowerCase() : null;
        String r2 = question.getResponse2() != null ? question.getResponse2().toLowerCase() : null;
        String r3 = question.getResponse3() != null ? question.getResponse3().toLowerCase() : null;
        String r4 = question.getResponse4() != null ? question.getResponse4().toLowerCase() : null;

        // Check if any answer appears more than once (meaning it exists elsewhere)
        return hasAnswerInList(r1, existingAnswers) ||
               hasAnswerInList(r2, existingAnswers) ||
               hasAnswerInList(r3, existingAnswers) ||
               hasAnswerInList(r4, existingAnswers);
    }

    /**
     * Check if an answer appears in the existing answers list (excluding the question's own answers).
     * An answer is considered duplicate if it appears more than once in the list.
     */
    private boolean hasAnswerInList(String answer, List<String> existingAnswers) {
        if (answer == null) return false;

        long count = existingAnswers.stream()
            .filter(existing -> existing.equals(answer))
            .count();

        // If count > 1, the answer exists in other questions (not just this one)
        return count > 1;
    }

    /**
     * Check if question title already exists in the database.
     *
     * @param question Question to check
     * @param existingTitles All existing titles in the course
     * @return true if title is a duplicate
     */
    private boolean checkTitleAgainstExisting(Question question, List<String> existingTitles) {
        if (question.getTitle() == null) return false;

        String titleLower = question.getTitle().toLowerCase();
        long count = existingTitles.stream()
            .filter(title -> title.equals(titleLower))
            .count();

        // If count > 1, the title exists in other questions (not just this one)
        return count > 1;
    }

    private static QuizError getAuthorError(Question question, String description) {
        QuizError quizError = new QuizError();
        quizError.setRowNumber(question.getCrtNo());
        quizError.setDescription(getDescriptionWithTitle(question, description));
        return quizError;
    }

    public static String getDescriptionWithTitle(Question question, String description) {
        return description + " (" + question.getTitle() + ")";
    }

    private static boolean hasAllAnswers(Question question) {
        return question.getResponse1() != null &&
                question.getResponse2() != null &&
                question.getResponse3() != null &&
                question.getResponse4() != null;
    }
}
