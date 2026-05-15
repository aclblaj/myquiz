package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.util.FileValidator;
import com.unitbv.myquiz.app.util.InputTemplate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Service responsible for parsing Excel files containing QuestionBank questions.
 * Handles both multiple choice and true/false question formats.
 * <p>
 * This service supports:
 * - Recursive directory traversal for batch file processing
 * - Template type detection (2023 vs 2024 formats)
 * - Multiple choice and true/false question parsing
 * - Error tracking and validation
 */
@Service
public class ExcelParsingService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelParsingService.class);

    // Excel Parser Configuration Constants
    private static final int MIN_MULTICHOICE_VALUES = 11;
    private static final int MIN_TRUEFALSE_VALUES = 6;
    private static final int MIN_QUESTIONS_PER_SHEET = 3;
    private static final int MAX_CONSECUTIVE_EMPTY_ROWS_TRUEFALSE = 2;
    private static final int MAX_CONSECUTIVE_EMPTY_ROWS_MULTICHOICE = 20;
    private static final int MAX_RECURSION_DEPTH = 10;

    private final QuestionWeightValidationService weightValidationService;
    private final CellConversionService cellConversionService;
    private final QuestionErrorService questionErrorService;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionErrorRepository questionErrorRepository;
    private final AuthorService authorService;

    public ExcelParsingService(QuestionWeightValidationService weightValidationService, CellConversionService cellConversionService, QuestionErrorService questionErrorService,
                               QuestionBankAuthorRepository questionBankAuthorRepository, QuestionErrorRepository questionErrorRepository, AuthorService authorService) {
        this.weightValidationService = weightValidationService;
        this.cellConversionService = cellConversionService;
        this.questionErrorService = questionErrorService;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionErrorRepository = questionErrorRepository;
        this.authorService = authorService;
    }

    /**
     * Recursively parse Excel files from a folder structure.
     *
     * @param questionBank The questionBank to associate questions with
     * @param folder       The root folder or file path to process
     * @param noFilesInput Starting count of processed files
     * @return Total number of Excel files successfully processed
     * @throws IllegalArgumentException if questionBank is null
     */
    public int parseExcelFilesFromFolder(QuestionBank questionBank, Path folder, int noFilesInput) {
        if (questionBank == null) {
            throw new IllegalArgumentException("QuestionBank cannot be null");
        }
        if (folder == null) {
            logger.atWarn().log("Folder is null, returning current file count");
            return noFilesInput;
        }

        return parseExcelFilesFromFolderInternal(questionBank, folder, noFilesInput, 0);
    }

    /**
     * Internal recursive implementation with depth tracking.
     */
    private int parseExcelFilesFromFolderInternal(QuestionBank questionBank, Path folder, int currentCount, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            logger.atWarn().addArgument(MAX_RECURSION_DEPTH).addArgument(folder).log("Maximum recursion depth {} reached at path: {}, stopping traversal");
            return currentCount;
        }

        if (!FileValidator.exists(folder)) {
            logger.atWarn().addArgument(folder).log("Path does not exist: {}");
            return currentCount;
        }

        int processedCount = currentCount;

        if (FileValidator.isDirectory(folder)) {
            processedCount = processDirectory(questionBank, folder, processedCount, depth);
        } else if (FileValidator.isRegularFile(folder)) {
            if (FileValidator.isExcelFile(folder)) {
                processedCount = processExcelFile(questionBank, folder, processedCount);
            } else {
                handleInvalidFile(questionBank, folder);
            }
        }

        return processedCount;
    }

    /**
     * Process all files in a directory recursively.
     */
    private int processDirectory(QuestionBank questionBank, Path directory, int currentCount, int depth) {
        Path[] files = FileValidator.listFiles(directory);

        if (files == null) {
            logger.atWarn().addArgument(directory).log("Cannot list files in directory: {}");
            return currentCount;
        }

        if (files.length == 0) {
            logger.atInfo().addArgument(directory).log("Empty directory: {}");
            return currentCount;
        } else {
            logger.atInfo().addArgument(directory).addArgument(files.length).log("Process directory '{}' with total number of files: {}");
        }

        int processedCount = currentCount;
        for (Path file : files) {
            try {
                processedCount = parseExcelFilesFromFolderInternal(questionBank, file, processedCount, depth + 1);
            } catch (Exception e) {
                logger.atError().addArgument(file).setCause(e).log("Error processing file/directory: {}");
            }
        }

        return processedCount;
    }

    /**
     * Process a single Excel file.
     */
    private int processExcelFile(QuestionBank questionBank, Path excelFile, int currentCount) {
        try {
            Author author = resolveAuthorForFile(excelFile);

            if (author == null) {
                logger.atWarn().addArgument(excelFile).log("Could not extract or save author from file: {}");
                handleInvalidFile(questionBank, excelFile);
                return currentCount;
            }

            logger.atInfo().addArgument(excelFile.getFileName()).addArgument(author.getName()).log("Processing Excel file '{}' for author '{}'");

            String result = parseFileSheets(questionBank, author, excelFile);

            if (result != null && !result.startsWith("Error")) {
                logger.atInfo().addArgument(excelFile.getFileName()).log("Successfully processed Excel file: {}");
                return currentCount + 1;
            } else {
                logger.atWarn().addArgument(excelFile.getFileName()).addArgument(result).log("Failed to process Excel file '{}': {}");
                return currentCount;
            }

        } catch (Exception e) {
            logger.atError().addArgument(excelFile).setCause(e).log("Exception while processing Excel file: {}");
            handleInvalidFile(questionBank, excelFile);
            return currentCount;
        }
    }

    /**
     * Handle invalid file by creating error record.
     */
    private void handleInvalidFile(QuestionBank questionBank, Path file) {
        try {
            logger.atInfo().addArgument(file).log("Not a readable Excel file: {}");

            Author author = resolveAuthorForFile(file);

            if (author != null) {
                Question errorQuestion = new Question();
                errorQuestion.setCrtNo(-1);

                QuestionBankAuthor questionBankAuthor = new QuestionBankAuthor();
                questionBankAuthor.setAuthor(author);
                questionBankAuthor.setQuestionBank(questionBank);
                questionBankAuthor.setSource(file.getFileName().toString());
                questionBankAuthor = questionBankAuthorRepository.save(questionBankAuthor);

                questionErrorService.addAuthorError(questionBankAuthor, errorQuestion, MyUtil.ERROR_WRONG_FILE_TYPE);

                logger.atWarn().addArgument(author.getName()).addArgument(file.getFileName()).log("Created error record for author '{}' with invalid file '{}'");
            }
        } catch (Exception e) {
            logger.atError().addArgument(file).setCause(e).log("Error handling invalid file: {}");
        }
    }

    /**
     * Resolve author from file name first ("Author Name_...") and fallback to path-based extraction.
     */
    private Author resolveAuthorForFile(Path file) {
        if (file == null || file.getFileName() == null) {
            return null;
        }

        String fileName = file.getFileName().toString();
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0) {
            String authorName = fileName.substring(0, underscoreIndex).trim();
            if (!authorName.isEmpty()) {
                AuthorDto authorDto = new AuthorDto();
                authorDto.setName(authorName);
                authorDto.setInitials(authorService.extractInitials(authorName));
                AuthorDto saved = authorService.saveAuthorDto(authorDto);
                if (saved != null && saved.getId() != null) {
                    return authorService.findAuthorEntityById(saved.getId());
                }
            }
        }

        return authorService.saveAuthorFromFile(file.toFile());
    }

    /**
     * Parse an Excel file and extract questions from all sheets.
     *
     * @param questionBank The questionBank to associate questions with
     * @param author       The author who submitted the file
     * @param filePath     Path to the Excel file (.xlsx format)
     * @return Status message indicating success or error details
     * @throws IllegalArgumentException if questionBank or author is null
     */
    public String parseFileSheets(QuestionBank questionBank, Author author, Path filePath) {
        if (questionBank == null) {
            throw new IllegalArgumentException("QuestionBank cannot be null");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        // Validate Excel file using FileValidator
        FileValidator.ValidationResult validation = FileValidator.validateExcelFile(filePath);
        if (validation.hasError()) {
            logger.atError().addArgument(filePath).addArgument(validation.getErrorMessage()).log("File validation failed for {}: {}");
            return "Error: " + validation.getErrorMessage();
        }

        logger.atInfo().addArgument(filePath).log("Start parse excel file: {}");

        QuestionBankAuthor questionBankAuthor = new QuestionBankAuthor();
        questionBankAuthor.setAuthor(author);
        questionBankAuthor.setQuestionBank(questionBank);
        questionBankAuthor.setSource(filePath.getFileName().toString());
        questionBankAuthor = questionBankAuthorRepository.save(questionBankAuthor);

        try (InputStream inputStream = Files.newInputStream(filePath); Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            TemplateType templateType = detectTemplateType(sheet);
            questionBankAuthor.setTemplateType(Objects.requireNonNullElse(templateType, TemplateType.Other));
            InputTemplate inputTemplate = new InputTemplate(templateType);

            String message = processMultichoiceSheet(questionBankAuthor, sheet, inputTemplate);
            logger.atInfo().addArgument(message).log("First sheet processed: '{}'");

            if (workbook.getNumberOfSheets() > 1) {
                Sheet sheetTF = workbook.getSheetAt(1);
                message = processTruefalseSheet(questionBankAuthor, sheetTF, inputTemplate);
                logger.atInfo().addArgument(message).log("Second sheet processed: '{}'");
            } else {
                logger.atInfo().log("Single sheet workbook");
            }

            int questionCount = questionBankAuthor.getQuestions() != null ? questionBankAuthor.getQuestions().size() : 0;
            int errorCount = (int) questionErrorRepository.countByQuestionQuestionBankAuthorId(questionBankAuthor.getId());
            logger.atInfo().addArgument(filePath.getFileName()).addArgument(questionBankAuthor.getAuthor().getName()).addArgument(questionCount).addArgument(errorCount).log(
                    "File '{}' | author '{}' | questions extracted: {} | errors: {}");

            questionBankAuthorRepository.save(questionBankAuthor);
            return "Finish parsing of the excel sheets";

        } catch (IOException e) {
            logger.atError().addArgument(filePath).setCause(e).log("I/O error processing Excel file: {}");
            return "I/O error reading file: " + e.getMessage();
        } catch (Exception e) {
            logger.atError().addArgument(filePath).setCause(e).log("Unexpected error processing Excel file: {}");
            return "Unexpected error processing file: " + e.getMessage();
        }
    }

    /**
     * Process a sheet containing true/false questions.
     */
    public String processTruefalseSheet(QuestionBankAuthor questionBankAuthor, Sheet sheet, InputTemplate inputTemplate) {
        int consecutiveEmptyRows = 0;

        for (Row row : sheet) {
            int currentRowNumber = row.getRowNum();
            RowProcessingResult result = processTrueFalseRowWithResult(row, questionBankAuthor, inputTemplate, currentRowNumber);

            if (result == RowProcessingResult.EMPTY_ROW) {
                consecutiveEmptyRows++;
                if (consecutiveEmptyRows > MAX_CONSECUTIVE_EMPTY_ROWS_TRUEFALSE) {
                    logger.info("Stopping processing: too many consecutive empty rows");
                    break;
                }
            } else if (result == RowProcessingResult.PROCESSED) {
                consecutiveEmptyRows = 0;
            }
        }
        return "finish parsing of the true-false sheet";
    }

    /**
     * Process a sheet containing multiple choice questions.
     */
    public String processMultichoiceSheet(QuestionBankAuthor questionBankAuthor, Sheet sheet, InputTemplate inputTemplate) {
        logger.atInfo().addArgument(questionBankAuthor.getAuthor().getName()).addArgument(questionBankAuthor.getTemplateType()).log("Start parsing multi choice sheet for '{}' template {}");

        questionBankAuthor.setQuestions(new ArrayList<>());

        if (!weightValidationService.validateMinimumQuestions(sheet, questionBankAuthor)) {
            return "error parsing file";
        }

        TemplateType templateType = detectTemplateType(sheet);
        int consecutiveEmptyRows = 0;
        int currentRowNumber = 0;

        for (Row row : sheet) {
            currentRowNumber = row.getRowNum();
            RowProcessingResult result = processMultichoiceRowWithResult(row, questionBankAuthor, inputTemplate, templateType, currentRowNumber);

            if (result == RowProcessingResult.EMPTY_ROW) {
                consecutiveEmptyRows++;
                if (consecutiveEmptyRows > MAX_CONSECUTIVE_EMPTY_ROWS_MULTICHOICE) {
                    logger.info("Stopping processing: too many consecutive empty rows");
                    break;
                }
            } else if (result == RowProcessingResult.PROCESSED) {
                consecutiveEmptyRows = 0;
            }
        }

        logger.info("Finish parsing multi choice sheet for '{}' with {} rows", questionBankAuthor.getAuthor().getName(), currentRowNumber);
        return "finish parsing multichoice sheet";
    }

    /**
     * Process a single row and return the result status.
     */
    private RowProcessingResult processMultichoiceRowWithResult(Row row, QuestionBankAuthor questionBankAuthor, InputTemplate inputTemplate, TemplateType templateType, int rowNumber) {
        if (weightValidationService.isHeaderRow(row, inputTemplate, rowNumber)) {
            return RowProcessingResult.SKIP_HEADER;
        }

        int noNotNull = cellConversionService.countNotNullValues(row);
        if (noNotNull == 0) {
            return RowProcessingResult.EMPTY_ROW;
        }

        processMultichoiceRow(questionBankAuthor, row, inputTemplate, templateType, rowNumber, noNotNull);
        return RowProcessingResult.PROCESSED;
    }

    /**
     * Process a single true/false row.
     */
    private RowProcessingResult processTrueFalseRowWithResult(Row row, QuestionBankAuthor questionBankAuthor, InputTemplate inputTemplate, int rowNumber) {
        if (weightValidationService.isTrueFalseHeaderRow(row, inputTemplate, rowNumber)) {
            return RowProcessingResult.SKIP_HEADER;
        }

        int noNotNull = cellConversionService.countNotNullValues(row);
        if (noNotNull == 0) {
            return RowProcessingResult.EMPTY_ROW;
        }

        Question question = new Question();
        question.setCrtNo(rowNumber);
        question.setType(QuestionType.TRUEFALSE);
        question.setQuestionBankAuthor(questionBankAuthor);

        if (noNotNull < MIN_TRUEFALSE_VALUES) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_VALUES_LESS_THAN_6);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            cellConversionService.convertRowToTrueFalseQuestion(questionBankAuthor, row, question, inputTemplate);
            weightValidationService.checkTrueFalseQuestionTotalPoint(questionBankAuthor, question);
            questionBankAuthor.getQuestions().add(question);
        }

        return RowProcessingResult.PROCESSED;
    }

    /**
     * Process a single multichoice row.
     */
    private void processMultichoiceRow(QuestionBankAuthor questionBankAuthor, Row row, InputTemplate inputTemplate, TemplateType templateType, int rowNumber, int noNotNull) {
        Question question = new Question();
        question.setCrtNo(rowNumber);
        question.setType(QuestionType.MULTICHOICE);
        question.setQuestionBankAuthor(questionBankAuthor);

        if (noNotNull < MIN_MULTICHOICE_VALUES) {
            questionErrorService.addAuthorError(questionBankAuthor, question, MyUtil.MISSING_VALUES_LESS_THAN_11);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            cellConversionService.convertRowToQuestion(questionBankAuthor, row, question, inputTemplate, templateType);
            weightValidationService.repairQuestionPoints(question);
            weightValidationService.checkQuestionTotalPoint(questionBankAuthor, question);
            weightValidationService.checkAllAnswersArePresent(question);
        }
        if (!MyUtil.SKIPPED_DUE_TO_ERROR.equals(question.getTitle())) {
            questionBankAuthor.getQuestions().add(question);
        }
    }

    /**
     * Detect template type based on first non-empty row.
     * Template2023 has exactly 11 columns, Template2024 has more.
     *
     * @param sheet The sheet to analyze
     * @return Detected template type or TemplateType.Other if unable to determine
     */
    private TemplateType detectTemplateType(Sheet sheet) {
        for (Row row : sheet) {
            int notNulls = cellConversionService.countNotNullValues(row);
            if (notNulls != 0) {
                return (notNulls == MIN_MULTICHOICE_VALUES) ? TemplateType.Template2023 : TemplateType.Template2024;
            }
        }
        return TemplateType.Other;
    }

    /**
     * Detect template type based on first non-empty row.
     */
    /**
     * Enum representing the result of row processing
     */
    private enum RowProcessingResult {
        SKIP_HEADER,
        EMPTY_ROW,
        PROCESSED
    }
}
