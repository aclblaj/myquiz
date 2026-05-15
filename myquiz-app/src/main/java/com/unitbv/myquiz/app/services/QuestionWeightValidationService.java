package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.util.InputTemplate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for validating QuestionBank questions during Excel parsing.
 * Handles validation of question weights, point totals, and required fields.
 */
@Service
public class QuestionWeightValidationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionWeightValidationService.class);

    // Constants
    private static final int MIN_QUESTIONS_PER_SHEET = 3;
    private static final double WEIGHT_33_PERCENT = 33.33333;
    private static final int WEIGHT_33_LOWER_BOUND = 30;
    private static final int WEIGHT_33_UPPER_BOUND = 35;
    private static final double WEIGHT_50_PERCENT = 50.0;
    private static final Double WEIGHT_25_PERCENT = 25.0;

    private final QuestionErrorService questionErrorService;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;

    public QuestionWeightValidationService(QuestionErrorService questionErrorService, QuestionBankAuthorRepository questionBankAuthorRepository) {
        this.questionErrorService = questionErrorService;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
    }

    @Transactional
    public boolean validateMinimumQuestions(Sheet sheet, QuestionBankAuthor questionBankAuthor) {
        if (sheet.getLastRowNum() < MIN_QUESTIONS_PER_SHEET) {
            Question markerQuestion = new Question();
            markerQuestion.setCrtNo(-1);
            markerQuestion.setQuestionBankAuthor(questionBankAuthor);
            questionErrorService.addAuthorError(questionBankAuthor, markerQuestion, MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            questionBankAuthorRepository.save(questionBankAuthor);
            log.atInfo().log(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            return false;
        }
        return true;
    }

    public boolean isHeaderRow(Row row, InputTemplate inputTemplate, int rowNumber) {
        int positionPR1 = inputTemplate.getPositionPR1();
        Cell pr1Cell = row.getCell(positionPR1);
        if (pr1Cell != null && pr1Cell.getCellType() == CellType.STRING) {
            String cellValue = pr1Cell.getStringCellValue();
            if (cellValue.contains("PR1") || cellValue.contains("Punctaj")) {
                log.atDebug().setMessage("Skipping header row at line {}").addArgument(rowNumber).log();
                return true;
            }
        }
        return false;
    }

    public boolean isTrueFalseHeaderRow(Row row, InputTemplate inputTemplate, int rowNumber) {
        int positionPR1 = inputTemplate.getPositionPR1();
        Cell pr1Cell = row.getCell(positionPR1);
        if (pr1Cell != null && pr1Cell.getCellType() == CellType.STRING) {
            String cellValue = pr1Cell.getStringCellValue();
            if (cellValue.contains("PR1") || cellValue.contains("Punctaj") || cellValue.contains("TRUE")) {
                log.atDebug().setMessage("Skipping header row at line {}").addArgument(rowNumber).log();
                return true;
            }
        }
        return false;
    }

    public void repairQuestionPoints(Question question) {
        // repair 0.5 values
        if (isEqual05(question.getWeightResponse1())) {
            question.setWeightResponse1(WEIGHT_50_PERCENT);
        }
        if (isEqual05(question.getWeightResponse2())) {
            question.setWeightResponse2(WEIGHT_50_PERCENT);
        }
        if (isEqual05(question.getWeightResponse3())) {
            question.setWeightResponse3(WEIGHT_50_PERCENT);
        }
        if (isEqual05(question.getWeightResponse4())) {
            question.setWeightResponse4(WEIGHT_50_PERCENT);
        }

        // repair 0.25 values
        if (isEqual025(question.getWeightResponse1())) {
            question.setWeightResponse1(WEIGHT_25_PERCENT);
        }
        if (isEqual025(question.getWeightResponse2())) {
            question.setWeightResponse2(WEIGHT_25_PERCENT);
        }
        if (isEqual025(question.getWeightResponse3())) {
            question.setWeightResponse3(WEIGHT_25_PERCENT);
        }
        if (isEqual025(question.getWeightResponse4())) {
            question.setWeightResponse4(WEIGHT_25_PERCENT);
        }

        // Repair 33% values
        if (isApproximately33(question.getWeightResponse1())) {
            question.setWeightResponse1(WEIGHT_33_PERCENT);
        }
        if (isApproximately33(question.getWeightResponse2())) {
            question.setWeightResponse2(WEIGHT_33_PERCENT);
        }
        if (isApproximately33(question.getWeightResponse3())) {
            question.setWeightResponse3(WEIGHT_33_PERCENT);
        }
        if (isApproximately33(question.getWeightResponse4())) {
            question.setWeightResponse4(WEIGHT_33_PERCENT);
        }

        // Handle 100% single correct answer
        int correctIndex = findSingleCorrectAnswerIndex(question);
        if (correctIndex != -1) {
            setAllIncorrectTo100(question, correctIndex);
        }
    }

    private boolean isEqual025(Double weight) {
        return weight != null && weight.doubleValue() == 0.25;
    }

    private boolean isEqual05(Double weight) {
        return weight != null && weight.doubleValue() == 0.5;
    }

    private boolean isApproximately33(Double weight) {
        if (weight == null) return false;
        int intValue = weight.intValue();
        return intValue > WEIGHT_33_LOWER_BOUND && intValue < WEIGHT_33_UPPER_BOUND;
    }

    private int findSingleCorrectAnswerIndex(Question question) {
        if (question.getWeightResponse1() != null && question.getWeightResponse1().intValue() == 100) return 1;
        if (question.getWeightResponse2() != null && question.getWeightResponse2().intValue() == 100) return 2;
        if (question.getWeightResponse3() != null && question.getWeightResponse3().intValue() == 100) return 3;
        if (question.getWeightResponse4() != null && question.getWeightResponse4().intValue() == 100) return 4;
        return -1;
    }

    private void setAllIncorrectTo100(Question question, int correctIndex) {
        if (correctIndex != 1) question.setWeightResponse1(-100.0);
        if (correctIndex != 2) question.setWeightResponse2(-100.0);
        if (correctIndex != 3) question.setWeightResponse3(-100.0);
        if (correctIndex != 4) question.setWeightResponse4(-100.0);
    }

    public void checkQuestionTotalPoint(QuestionBankAuthor questionBankAuthor, Question question) {
        // Ensure all weights are non-null before calculations
        if (question.getWeightResponse1() == null || question.getWeightResponse2() == null || question.getWeightResponse3() == null || question.getWeightResponse4() == null) {
            return; // Skip validation if weights are incomplete
        }

        double total = question.getWeightResponse1() + question.getWeightResponse2() + question.getWeightResponse3() + question.getWeightResponse4();

        if (hasSingleCorrectAnswer(question)) {
            if (total != 100 && total != -200) {
                questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }
        } else if (isOneAnswerEqual33(question) && total > 1) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.TEMPLATE_ERROR_3_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (isOneAnswerEqual50(question) && total != 0) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.TEMPLATE_ERROR_2_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (total == 0 && !isOneAnswerEqual33(question) && !isOneAnswerEqual50(question)) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_POINTS);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    private boolean hasSingleCorrectAnswer(Question question) {
        return (question.getWeightResponse1() != null && question.getWeightResponse1().intValue() == 100) || (question.getWeightResponse2() != null && question.getWeightResponse2()
                                                                                                                                                               .intValue() == 100) || (question.getWeightResponse3() != null && question.getWeightResponse3()
                                                                                                                                                                                                                                        .intValue() == 100) || (question.getWeightResponse4() != null && question.getWeightResponse4()
                                                                                                                                                                                                                                                                                                                 .intValue() == 100);
    }

    private boolean isOneAnswerEqual33(Question question) {
        return (question.getWeightResponse1() != null && question.getWeightResponse1().intValue() == 33) || (question.getWeightResponse2() != null && question.getWeightResponse2()
                                                                                                                                                              .intValue() == 33) || (question.getWeightResponse3() != null && question.getWeightResponse3()
                                                                                                                                                                                                                                      .intValue() == 33) || (question.getWeightResponse4() != null && question.getWeightResponse4()
                                                                                                                                                                                                                                                                                                              .intValue() == 33);
    }

    private boolean isOneAnswerEqual50(Question question) {
        return (question.getWeightResponse1() != null && question.getWeightResponse1().intValue() == 50) || (question.getWeightResponse2() != null && question.getWeightResponse2()
                                                                                                                                                              .intValue() == 50) || (question.getWeightResponse3() != null && question.getWeightResponse3()
                                                                                                                                                                                                                                      .intValue() == 50) || (question.getWeightResponse4() != null && question.getWeightResponse4()
                                                                                                                                                                                                                                                                                                              .intValue() == 50);
    }

    public void checkTrueFalseQuestionTotalPoint(QuestionBankAuthor questionBankAuthor, Question question) {
        double total = question.getWeightFalse() + question.getWeightTrue();
        if ((question.getWeightTrue() == 100 && total != 100) || (question.getWeightFalse() == 100 && total != 100)) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.TEMPLATE_ERROR_TRUE_FALSE_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    public void checkAllAnswersArePresent(Question question) {
        if (question.getResponse1() == null || question.getResponse2() == null || question.getResponse3() == null || question.getResponse4() == null) {
            questionErrorService.addAuthorError(question.getQuestionBankAuthor(), question, MyUtil.MISSING_ANSWER);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }
}
