package com.unitbv.myquiz.services.impl;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.MyUtil;
import com.unitbv.myquiz.services.QuestionValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionValidationServiceImpl implements QuestionValidationService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionValidationServiceImpl.class);

    private final QuestionRepository questionRepository;
    private final AuthorErrorService authorErrorService;

    @Autowired
    public QuestionValidationServiceImpl(QuestionRepository questionRepository,
                                         AuthorErrorService authorErrorService) {
        this.questionRepository = questionRepository;
        this.authorErrorService = authorErrorService;
    }

    @Override
    public void checkDuplicatesQuestionsForAuthors(ArrayList<Author> authors, String course) {
        authors.forEach(author -> {
            List<Question> questions = getQuestionsForAuthorId(author.getId(), course);
            List<QuizError> quizErrors = detectAuthorErrors(questions);
            if (!quizErrors.isEmpty()) {
                authorErrorService.saveAllAuthorErrors(quizErrors);
            }
            logger.atInfo()
                  .addArgument(author.getName())
                  .addArgument(questions.size())
                  .addArgument(quizErrors.size())
                  .log("{} - no. questions: {}, no. errors: {}");
        });
    }

    private List<Question> getQuestionsForAuthorId(Long authorId, String course) {
        return questionRepository.findByQuizAuthor_Author_IdAndQuizAuthor_Quiz_Course(authorId, course);
    }

    private List<String> putAllQuestionsToList() {
        List<String> allAnswers = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question q : allQuestionInstances) {
            if (q.getResponse1() != null) allAnswers.add(q.getResponse1().toLowerCase());
            if (q.getResponse2() != null) allAnswers.add(q.getResponse2().toLowerCase());
            if (q.getResponse3() != null) allAnswers.add(q.getResponse3().toLowerCase());
            if (q.getResponse4() != null) allAnswers.add(q.getResponse4().toLowerCase());
        }
        return allAnswers;
    }

    private List<String> putAllTitlesToList() {
        List<String> allTitles = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question question : allQuestionInstances) {
            allTitles.add(question.getTitle().toLowerCase());
        }
        return allTitles;
    }

    private List<QuizError> detectAuthorErrors(List<Question> questions) {
        List<QuizError> quizErrors = new ArrayList<>();
        for (Question question : questions) {
            QuizError quizError = checkQuestionStrings(question);
            if (quizError != null) {
                quizError.setQuizAuthor(question.getQuizAuthor());
                quizErrors.add(quizError);
            }
        }
        return quizErrors;
    }

    private QuizError checkQuestionStrings(Question question) {
        QuizError quizError = null;
        switch (question.getType()) {
            case MULTICHOICE:
                if (hasAllAnswers(question)) {
                    if (checkAllAnswersForDuplicates(question)) {
                        quizError = getAuthorError(question, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    } else if (checkAllTitlesForDuplicates(question)) {
                        quizError = getAuthorError(question, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    }
                } else {
                    quizError = getAuthorError(question, MyUtil.MISSING_ANSWER);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
                break;
            case TRUEFALSE:
                logger.atDebug().addArgument(question)
                      .log("True false question: {} not checked");
                break;
            default:
                logger.atDebug().addArgument(question)
                      .log("Question type not recognized: {}");
                break;
        }
        return quizError;
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

    private boolean checkAllAnswersForDuplicates(Question question) {
        List<String> allQuestionsAnswers = putAllQuestionsToList();
        return isAnswerDuplicated(question.getResponse1(), allQuestionsAnswers) ||
               isAnswerDuplicated(question.getResponse2(), allQuestionsAnswers) ||
               isAnswerDuplicated(question.getResponse3(), allQuestionsAnswers) ||
               isAnswerDuplicated(question.getResponse4(), allQuestionsAnswers);
    }

    private static boolean isAnswerDuplicated(String questionText, List<String> allQuestionsAnswers) {
        return questionText != null && allQuestionsAnswers
                .stream()
                .filter(responseText -> responseText.equals(questionText.toLowerCase()))
                .count() > 1;
    }

    private boolean checkAllTitlesForDuplicates(Question question) {
        List<String> found = putAllTitlesToList().stream()
                .filter(title -> title.equals(question.getTitle().toLowerCase()))
                .toList();
        return question.getTitle() != null && found.size() > 1;
    }
}
