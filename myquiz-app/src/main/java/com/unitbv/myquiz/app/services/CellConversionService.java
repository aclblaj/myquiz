package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.util.InputTemplate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for converting Excel cells to Question entities.
 * Handles:
 * - Cell type detection and conversion
 * - Text cleaning and encoding
 * - Row to Question mapping
 * - Value extraction and validation
 */
@Service
public class CellConversionService {

    private static final Logger logger = LoggerFactory.getLogger(CellConversionService.class);

    // Weight validation constants
    private static final double MIN_WEIGHT = -100.0;
    private static final double MAX_WEIGHT = 100.0;

    private final QuestionErrorService questionErrorService;
    private final EncodingSevice encodingSevice;
    private final TextProcessingService textProcessingService;

    public CellConversionService(QuestionErrorService questionErrorService, EncodingSevice encodingSevice, TextProcessingService textProcessingService) {
        this.questionErrorService = questionErrorService;
        this.encodingSevice = encodingSevice;
        this.textProcessingService = textProcessingService;
    }

    /**
     * Count non-null values in a row.
     *
     * @param row The row to count
     * @return Number of non-null, non-empty cells
     */
    public int countNotNullValues(Row row) {
        int count = 0;
        for (Cell cell : row) {
            if (cell != null) {
                if (cell.getCellType() == CellType.STRING) {
                    if (!cell.getStringCellValue().isEmpty()) {
                        count++;
                    }
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Convert a cell value to double with error handling.
     *
     * @param questionBankAuthor Context for error tracking
     * @param cell               The cell to convert
     * @param question           Question for error association
     * @return Converted double value or 0.0 on error
     */
    public double convertCellToDouble(QuestionBankAuthor questionBankAuthor, Cell cell, Question question) {
        if (cell == null) {
            question.setCrtNo(-1);
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_POINTS);
            return 0.0;
        }


        try {
            CellType cellType = cell.getCellType();

            double result = switch (cellType) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> {
                    String strValue = cell.getStringCellValue().trim();
                    // if the strValue contains "33," or "0," replace "," with "."
                    strValue = replaceComma(strValue);


                    if (strValue.isEmpty()) {
                        questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_POINTS);
                        yield 0.0;
                    }
                    yield Double.parseDouble(strValue);
                }
                case BLANK -> {
                    questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_POINTS);
                    yield 0.0;
                }
                default -> {
                    question.setCrtNo(cell.getRowIndex());
                    questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.NOT_NUMERIC_COLUMN + " (Type: " + cellType + ")");
                    yield 0.0;
                }
            };

            // Validate range
            if (result < MIN_WEIGHT || result > MAX_WEIGHT) {
                questionErrorService.addAuthorError(questionBankAuthor, question, "Invalid weight value: " + result + " (must be between -100 and 100)");
            }

            return result;

        } catch (NumberFormatException e) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.DATATYPE_ERROR + ": Cannot parse '" + cell.getStringCellValue() + "' as number");
            logger.error(
                    "For author '{}',template '{}', cannot parse cell value as number '{}'", questionBankAuthor.getAuthorName(), questionBankAuthor.getTemplateType(), cell.getStringCellValue(),
                    e
            );
            return 0.0;
        } catch (Exception e) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.DATATYPE_ERROR + ": " + e.getMessage());
            logger.error("Unexpected error converting cell to double", e);
            return 0.0;
        }
    }

    private String replaceComma(String strValue) {
        if (!strValue.isEmpty() && strValue.contains(",")) {
            strValue = strValue.replace(",", ".");
        }
        return strValue;
    }

    /**
     * Get cell value as string with error handling.
     *
     * @param questionBankAuthor Context for error tracking
     * @param cell               The cell to read
     * @param question           Question for error association
     * @return String value or empty string
     */
    public String getValueAsString(QuestionBankAuthor questionBankAuthor, Cell cell, Question question) {
        String result = "";
        try {
            if (cell != null) {
                CellType cellType = cell.getCellType();
                if (cellType == CellType.STRING) {
                    result = cell.getStringCellValue();
                } else if (cellType == CellType.NUMERIC) {
                    result = String.valueOf(cell.getNumericCellValue());
                }
            }
            if (result.isEmpty()) {
                questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_ANSWER);
            }
        } catch (Exception e) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.DATATYPE_ERROR);
            String logMsg = getExceptionClassName(e) + " " + e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            questionErrorService.addAuthorError(questionBankAuthor, question, logMsg);
        }
        return result;
    }

    /**
     * Convert Excel row to multiple choice Question entity.
     */
    public void convertRowToQuestion(QuestionBankAuthor questionBankAuthor, Row row, Question question, InputTemplate inputTemplate, TemplateType templateType) {
        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        convertCellToDouble(questionBankAuthor, cellNrCrt, question);

        validateAndSetTitle(questionBankAuthor, row, question, inputTemplate);
        validateAndSetQuestionText(questionBankAuthor, row, question, inputTemplate);

        Cell cellPR1 = row.getCell(inputTemplate.getPositionPR1());
        question.setWeightResponse1(convertCellToDouble(questionBankAuthor, cellPR1, question));

        Cell cellResponse1 = row.getCell(inputTemplate.getPositionResponse1());
        question.setResponse1(cleanAndConvert(getValueAsString(questionBankAuthor, cellResponse1, question)));

        Cell cellPR2 = row.getCell(inputTemplate.getPositionPR2());
        question.setWeightResponse2(convertCellToDouble(questionBankAuthor, cellPR2, question));

        Cell cellResponse2 = row.getCell(inputTemplate.getPositionResponse2());
        question.setResponse2(cleanAndConvert(getValueAsString(questionBankAuthor, cellResponse2, question)));

        Cell cellPR3 = row.getCell(inputTemplate.getPositionPR3());
        question.setWeightResponse3(convertCellToDouble(questionBankAuthor, cellPR3, question));

        Cell cellResponse3 = row.getCell(inputTemplate.getPositionResponse3());
        question.setResponse3(cleanAndConvert(getValueAsString(questionBankAuthor, cellResponse3, question)));

        Cell cellPR4 = row.getCell(inputTemplate.getPositionPR4());
        question.setWeightResponse4(convertCellToDouble(questionBankAuthor, cellPR4, question));

        Cell cellResponse4 = row.getCell(inputTemplate.getPositionResponse4());
        question.setResponse4(cleanAndConvert(getValueAsString(questionBankAuthor, cellResponse4, question)));

        Cell cellReference = row.getCell(inputTemplate.getPostionReference());
        question.setAnswerReferenceText(textProcessingService.prepareImportedReference(getOptionalCellValueAsString(cellReference)));

        if (templateType == TemplateType.Template2024) {
            Cell cellCourse = row.getCell(inputTemplate.getPositionCourse());
            question.setChapter(cleanAndConvert(getValueAsString(questionBankAuthor, cellCourse, question)));
        }
    }

    /**
     * Convert Excel row to true/false Question entity.
     */
    public void convertRowToTrueFalseQuestion(QuestionBankAuthor questionBankAuthor, Row row, Question question, InputTemplate inputTemplate) {
        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        convertCellToDouble(questionBankAuthor, cellNrCrt, question);

        validateAndSetTitle(questionBankAuthor, row, question, inputTemplate);
        validateAndSetQuestionText(questionBankAuthor, row, question, inputTemplate);

        Cell cellPRTrue = row.getCell(inputTemplate.getPositionPRTrue());
        question.setWeightTrue(convertCellToDouble(questionBankAuthor, cellPRTrue, question));

        Cell cellPRFalse = row.getCell(inputTemplate.getPositionPRFalse());
        question.setWeightFalse(convertCellToDouble(questionBankAuthor, cellPRFalse, question));

        Cell cellResponse1 = row.getCell(inputTemplate.getPositionResponse1());
        question.setResponse1(cleanAndConvert(getValueAsString(questionBankAuthor, cellResponse1, question)));

        Cell cellReference = row.getCell(inputTemplate.getPostion_TF_Reference());
        question.setAnswerReferenceText(textProcessingService.prepareImportedReference(getOptionalCellValueAsString(cellReference)));
    }

    private String getOptionalCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return null;
    }

    /**
     * Validate and set title from cell.
     */
    private void validateAndSetTitle(QuestionBankAuthor questionBankAuthor, Row row, Question question, InputTemplate inputTemplate) {
        Cell cellTitlu = row.getCell(inputTemplate.getPositionTitle());

        if (cellTitlu == null) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_TITLE);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellTitlu.getCellType() == CellType.NUMERIC) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.TITLE_NOT_STRING);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellTitlu.getCellType() == CellType.STRING) {
            String title = cellTitlu.getStringCellValue();
            if (title.length() < 2) {
                questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_TITLE);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (MyUtil.FORBIDDEN_TITLES.contains(title)) {
                questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.REMOVE_TEMPLATE_QUESTION + title);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else {
                question.setTitle(cleanAndConvert(title));
            }
        } else {
            question.setTitle(cleanAndConvert(question.getTitle()));
        }
    }

    /**
     * Validate and set question text from cell.
     */
    private void validateAndSetQuestionText(QuestionBankAuthor questionBankAuthor, Row row, Question question, InputTemplate inputTemplate) {
        Cell cellText = row.getCell(inputTemplate.getPositionText());

        if (cellText == null) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellText.getCellType() == CellType.NUMERIC) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.DATATYPE_ERROR);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellText.getCellType() == CellType.STRING) {
            String questionText = cellText.getStringCellValue();
            if (questionText.isEmpty()) {
                questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else {
                question.setText(cleanAndConvert(questionText));
            }
        }
    }

    /**
     * Clean and convert text: encoding, remove enumerations, remove special chars.
     *
     * @param text Text to clean
     * @return Cleaned text or empty string if null
     */
    public String cleanAndConvert(String text) {
        if (text == null) {
            return "";
        }

        text = encodingSevice.convertToUTF8(text);
        if (text == null) {
            return "";
        }

        text = removeEnumerations(text);
        text = removeSpecialChars(text);

        return text != null ? text : "";
    }

    /**
     * Remove special characters and normalize whitespace.
     */
    private String removeSpecialChars(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        text = text.replace("â€“", "-");
        text = text.replace("â€ž", "\"");
        text = text.replace("â€¦", "...");
        text = text.replace("â€”", "-");
        text = text.replace("\n", " ");
        text = text.replace("\r", " ");
        text = text.replace("\t", " ");
        text = text.replace("&", " ");

        text = text.replaceAll("\\s+", " ");
        text = text.trim();

        return text;
    }

    /**
     * Remove standalone answer enumerations (A., B., 1., 2., (A), (1), etc.)
     * only when they appear as list prefixes (start of text or after whitespace).
     * This avoids altering abbreviations inside multi-character parentheses, e.g. (SA), (LOC).
     */
    private String removeEnumerations(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text.replaceAll("(^|\\s)\\(?[A-Da-d1-4][\\.)](?=\\s|$)", "$1");
    }

    /**
     * Get exception class name without package.
     */
    private String getExceptionClassName(Exception e) {
        return e.getClass().getName().replaceAll(".*\\.", "");
    }
}
