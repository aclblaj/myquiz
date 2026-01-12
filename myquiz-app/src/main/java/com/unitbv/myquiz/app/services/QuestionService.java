package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterDto;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Course;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.Quiz;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import com.unitbv.myquiz.app.entities.QuizError;
import com.unitbv.myquiz.app.mapper.QuestionDtoEnricher;
import com.unitbv.myquiz.app.mapper.QuestionMapper;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuizRepository;
import com.unitbv.myquiz.app.specifications.CourseSpecification;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.specifications.QuizAuthorSpecification;
import com.unitbv.myquiz.app.specifications.QuizSpecification;
import com.unitbv.myquiz.app.util.InputTemplate;
import com.unitbv.myquiz.api.types.TemplateType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.util.StringUtils.getFilename;

@Service
public class QuestionService {

    private final QuizRepository quizRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);

    // Excel Parser Configuration Constants
    private static final int MIN_MULTICHOICE_VALUES = 11;
    private static final int MIN_TRUEFALSE_VALUES = 6;
    private static final int MIN_QUESTIONS_PER_SHEET = 3;
    private static final int MAX_CONSECUTIVE_EMPTY_ROWS_TRUEFALSE = 2;
    private static final int MAX_CONSECUTIVE_EMPTY_ROWS_MULTICHOICE = 20;
    private static final String EXCEL_FILE_EXTENSION = ".xlsx";

    // Weight validation constants
    private static final double MIN_WEIGHT = -100.0;
    private static final double MAX_WEIGHT = 100.0;
    private static final double WEIGHT_33_PERCENT = 33.33333;
    private static final int WEIGHT_33_LOWER_BOUND = 30;
    private static final int WEIGHT_33_UPPER_BOUND = 35;

    private final AuthorService authorService;
    private final QuizErrorService quizErrorService;
    private final QuestionRepository questionRepository;
    private final EncodingSevice encodingSevice;
    private final QuizAuthorRepository quizAuthorRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final AuthorRepository authorRepository;
    private final QuestionMapper questionMapper;
    private final QuestionDtoEnricher questionDtoEnricher;

    private String initials;

    @Autowired
    public QuestionService(AuthorService authorService,
                               QuizErrorService quizErrorService,
                               QuestionRepository questionRepository,
                               EncodingSevice encodingSevice,
                               QuizAuthorRepository quizAuthorRepository,
                               QuizRepository quizRepository,
                               CourseRepository courseRepository,
                               CourseService courseService,
                               AuthorRepository authorRepository,
                               QuestionMapper questionMapper,
                               QuestionDtoEnricher questionDtoEnricher) {
        this.authorService = authorService;
        this.quizErrorService = quizErrorService;
        this.questionRepository = questionRepository;
        this.encodingSevice = encodingSevice;
        this.quizAuthorRepository = quizAuthorRepository;
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.authorRepository = authorRepository;
        this.questionMapper = questionMapper;
        this.questionDtoEnricher = questionDtoEnricher;
    }

    /**
     * Recursively parse Excel files from a folder structure.
     * 
     * This method traverses a directory tree, processing Excel files (.xlsx) and tracking invalid files.
     * It follows the upload-sd.md specifications for archive upload (Section 2.4).
     * 
     * Directory Structure Expected:
     * - Root folder containing author subfolders or direct Excel files
     * - Each author subfolder contains Excel files submitted by that author
     * - File names are used to extract author information
     * 
     * @param quiz The quiz to associate questions with
     * @param folder The root folder or file to process
     * @param noFilesInput Starting count of processed files
     * @return Total number of Excel files successfully processed
     * @throws IllegalArgumentException if quiz is null
     */
    public int parseExcelFilesFromFolder(Quiz quiz, File folder, int noFilesInput) {
        // Validation
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz cannot be null");
        }
        if (folder == null) {
            logger.atWarn().log("Folder is null, returning current file count");
            return noFilesInput;
        }
        
        return parseExcelFilesFromFolderInternal(quiz, folder, noFilesInput, 0);
    }
    
    /**
     * Internal recursive implementation with depth tracking to prevent stack overflow.
     * 
     * @param quiz The quiz to associate questions with
     * @param folder The current folder or file to process
     * @param currentCount Current count of processed files
     * @param depth Current recursion depth
     * @return Updated count of processed files
     */
    private int parseExcelFilesFromFolderInternal(Quiz quiz, File folder, int currentCount, int depth) {
        // Safety check: prevent excessive recursion depth
        final int MAX_DEPTH = 10;
        if (depth > MAX_DEPTH) {
            logger.atWarn()
                  .addArgument(folder.getAbsolutePath())
                  .addArgument(MAX_DEPTH)
                  .log("Maximum recursion depth {} reached at path: {}, stopping traversal");
            return currentCount;
        }
        
        // Check if folder exists
        if (!folder.exists()) {
            logger.atWarn().addArgument(folder.getAbsolutePath())
                  .log("Path does not exist: {}");
            return currentCount;
        }
        
        int processedCount = currentCount;
        
        // Handle directory: recursively process all files
        if (folder.isDirectory()) {
            processedCount = processDirectory(quiz, folder, processedCount, depth);
        }
        // Handle Excel file: parse and extract questions
        else if (folder.isFile()) {
            if (isExcelFile(folder)) {
                processedCount = processExcelFile(quiz, folder, processedCount);
            } else {
                handleInvalidFile(quiz, folder);
            }
        }
        
        return processedCount;
    }
    
    /**
     * Process all files in a directory, recursively traversing subdirectories.
     * 
     * @param quiz The quiz to associate questions with
     * @param directory The directory to process
     * @param currentCount Current count of processed files
     * @param depth Current recursion depth
     * @return Updated count of processed files
     */
    private int processDirectory(Quiz quiz, File directory, int currentCount, int depth) {
        File[] files = directory.listFiles();
        
        if (files == null) {
            logger.atWarn().addArgument(directory.getAbsolutePath())
                  .log("Cannot list files in directory (permission issue?): {}");
            return currentCount;
        }
        
        if (files.length == 0) {
            logger.atInfo().addArgument(directory.getAbsolutePath())
                  .log("Empty directory: {}");
            return currentCount;
        }
        
        // Sort files for consistent processing order
        Arrays.sort(files);
        
        int processedCount = currentCount;
        for (File file : files) {
            try {
                processedCount = parseExcelFilesFromFolderInternal(quiz, file, processedCount, depth + 1);
            } catch (Exception e) {
                logger.atError()
                      .addArgument(file.getAbsolutePath())
                      .setCause(e)
                      .log("Error processing file/directory: {}");
                // Continue processing other files even if one fails
            }
        }
        
        return processedCount;
    }
    
    /**
     * Process a single Excel file: extract author, parse questions.
     * 
     * @param quiz The quiz to associate questions with
     * @param excelFile The Excel file to process
     * @param currentCount Current count of processed files
     * @return Updated count (incremented by 1 if successful)
     */
    private int processExcelFile(Quiz quiz, File excelFile, int currentCount) {
        try {
            // Extract author information from file path
            Author author = saveAuthorName(excelFile);
            
            if (author == null) {
                logger.atWarn().addArgument(excelFile.getAbsolutePath())
                      .log("Could not extract or save author from file: {}");
                handleInvalidFile(quiz, excelFile);
                return currentCount;
            }
            
            // Parse Excel file and extract questions
            logger.atInfo()
                  .addArgument(excelFile.getName())
                  .addArgument(author.getName())
                  .log("Processing Excel file '{}' for author '{}'");
            
            String result = parseFileSheets(quiz, author, excelFile.getAbsolutePath());
            
            if (result != null && !result.startsWith("Error")) {
                logger.atInfo()
                      .addArgument(excelFile.getName())
                      .log("Successfully processed Excel file: {}");
                return currentCount + 1;
            } else {
                logger.atWarn()
                      .addArgument(excelFile.getName())
                      .addArgument(result)
                      .log("Failed to process Excel file '{}': {}");
                return currentCount;
            }
            
        } catch (Exception e) {
            logger.atError()
                  .addArgument(excelFile.getAbsolutePath())
                  .setCause(e)
                  .log("Exception while processing Excel file: {}");
            handleInvalidFile(quiz, excelFile);
            return currentCount;
        }
    }
    
    /**
     * Handle invalid file: create error record for tracking.
     * 
     * @param quiz The quiz context
     * @param file The invalid file
     */
    private void handleInvalidFile(Quiz quiz, File file) {
        try {
            logger.atInfo()
                  .addArgument(file.getAbsolutePath())
                  .log("Not a readable Excel file: {}");
            
            Author author = saveAuthorName(file);
            
            if (author != null) {
                // Create error record
                Question errorQuestion = new Question();
                errorQuestion.setCrtNo(-1);
                
                QuizAuthor quizAuthor = new QuizAuthor();
                quizAuthor.setAuthor(author);
                quizAuthor.setQuiz(quiz);
                quizAuthor.setSource(file.getName());
                quizAuthor = quizAuthorRepository.save(quizAuthor);
                
                quizErrorService.addAuthorError(quizAuthor, errorQuestion, MyUtil.ERROR_WRONG_FILE_TYPE);
                
                logger.atWarn()
                      .addArgument(author.getName())
                      .addArgument(file.getName())
                      .log("Created error record for author '{}' with invalid file '{}'");
            }
        } catch (Exception e) {
            logger.atError()
                  .addArgument(file.getAbsolutePath())
                  .setCause(e)
                  .log("Error handling invalid file: {}");
        }
    }
    
    /**
     * Check if a file is an Excel file (.xlsx).
     *
     * @param file The file to check
     * @return true if file has .xlsx extension
     */
    private boolean isExcelFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(EXCEL_FILE_EXTENSION);
    }


    public int parseExcelFilesFromFlatFolder(Quiz quiz, File folder) {
        var ref = new Object() {
            int noFiles = 0;
        };
        File[] files = folder.listFiles();
        if (files != null) {
            Arrays.stream(files).sorted().forEach(file -> {
                if (file.isFile() && file.getName().endsWith(EXCEL_FILE_EXTENSION)) {
                    //extract author full name from the path - the substring until first underscore
                    String authorFullName = file.getName().substring(0, file.getName().indexOf("_"));

                    Author author = saveAuthorIfNotExists(authorFullName);
                    if (author != null) {
                        this.parseFileSheets(quiz, author, file.getAbsolutePath());
                        ref.noFiles++;
                    }
                } else {
                    logger.atInfo().addArgument(file.getAbsolutePath()).log("Not readable target file: {}");
                    Author author = saveAuthorName(file);
                    Question question = new Question();
                    question.setCrtNo(-1);
                    QuizAuthor quizAuthor = new QuizAuthor();
                    quizAuthor.setAuthor(author);
                    quizAuthor.setQuiz(quiz);
                    quizAuthorRepository.save(quizAuthor);
                    quizErrorService.addAuthorError(quizAuthor, question, MyUtil.ERROR_WRONG_FILE_TYPE);
                }
            });
        }
        return ref.noFiles;
    }

    /**
     * Save author name in the database - return author if successful
     *
     * @param folder the file/folder containing author information
     * @return Author entity
     */
    public Author saveAuthorName(File folder) {
        String authorName = authorService.extractAuthorNameFromPath(folder.getAbsolutePath());
        initials = authorService.extractInitials(authorName);
        AuthorDto authorDto = new AuthorDto();
        authorDto.setName(authorName);
        authorDto.setInitials(initials);
        if (authorService.authorNameExists(authorDto.getName())) {
            logger.atInfo().addArgument(authorDto.getName())
                  .log("Author {} already exists in the database");
            authorDto = authorService.getAuthorByName(authorDto.getName());
        } else {
            authorDto = authorService.saveAuthorDto(authorDto);
        }
        // Note: Removed stateful addAuthorToList - callers should track authors locally if needed
        // Return entity from repository
        return authorRepository.findById(authorDto.getId()).orElse(null);
    }

    public Author saveAuthorIfNotExists(String authorFullName) {
        initials = authorService.extractInitials(authorFullName);
        AuthorDto authorDto = new AuthorDto();
        authorDto.setName(authorFullName);
        authorDto.setInitials(initials);
        if (authorService.authorNameExists(authorDto.getName())) {
            logger.atInfo().addArgument(authorDto.getName())
                  .log("Author {} already exists in the database");
            authorDto = authorService.getAuthorByName(authorDto.getName());
        } else {
            authorDto = authorService.saveAuthorDto(authorDto);
        }
        // Note: Removed stateful addAuthorToList - callers should track authors locally if needed
        // Return entity from repository
        return authorRepository.findById(authorDto.getId()).orElse(null);
    }

    /**
     * Parse an Excel file and extract questions from all sheets.
     *
     * Processes the first sheet as multichoice questions and optionally
     * the second sheet as true/false questions. Creates a QuizAuthor
     * association and tracks all errors during parsing.
     *
     * @param quiz The quiz to associate questions with (must not be null)
     * @param author The author who submitted the file (must not be null)
     * @param filePath Path to the Excel file (.xlsx format)
     * @return Status message indicating success or error details
     * @throws IllegalArgumentException if quiz or author is null
     */
    public String parseFileSheets(Quiz quiz, Author author, String filePath) {
        // Input validation
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz cannot be null");
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

        String message;
        logger.atInfo().addArgument(filePath)
              .log("Start parse excel file: {}");

        QuizAuthor quizAuthor = new QuizAuthor();
        quizAuthor.setAuthor(author);
        quizAuthor.setQuiz(quiz);
        quizAuthor.setSource(getFilename(filePath));
        quizAuthor = quizAuthorRepository.save(quizAuthor);

        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Detect template type and create InputTemplate
            TemplateType templateType = detectTemplateType(sheet);
            quizAuthor.setTemplateType(Objects.requireNonNullElse(templateType, TemplateType.Other));
            InputTemplate inputTemplate = new InputTemplate(templateType);

            message = processMultichoiceSheet(quizAuthor, sheet, inputTemplate);
            logger.atInfo().addArgument(message).log("First sheet processed '{}'");

            if (workbook.getNumberOfSheets() > 1) {
                Sheet sheetTF = workbook.getSheetAt(1);
                message = processTruefalseSheet(quizAuthor, sheetTF, inputTemplate);
                logger.atInfo().addArgument(message).log("Second sheet processed '{}'");
            } else {
                logger.atInfo().log("Single sheet workbook");
            }

            quizAuthorRepository.save(quizAuthor);
            message = "Finish parsing of the excel sheets";

        } catch (java.io.FileNotFoundException e) {
            message = "File not found: " + filePath;
            logger.atError().setCause(e).addArgument(filePath)
                  .log("Excel file not found: {}");
        } catch (java.io.IOException e) {
            message = "I/O error reading file: " + e.getMessage();
            logger.atError().setCause(e).addArgument(filePath)
                  .log("I/O error processing Excel file: {}");
        } catch (Exception e) {
            message = "Unexpected error processing file: " + e.getMessage();
            logger.atError().setCause(e).addArgument(filePath)
                  .log("Unexpected error processing Excel file: {}");
        }
        return message;
    }

    /**
     * Process a sheet containing true/false questions.
     *
     * Expected format:
     * - Column positions defined in InputTemplate
     * - Minimum 6 non-null values per valid row
     * - Stops after 2 consecutive empty rows
     *
     * @param quizAuthor The QuizAuthor association to add questions to
     * @param sheet The Excel sheet containing true/false questions
     * @param inputTemplate Template configuration for column positions
     * @return Status message
     */
    public String processTruefalseSheet(QuizAuthor quizAuthor, Sheet sheet, InputTemplate inputTemplate) {
        int consecutiveEmptyRows = 0;

        for (Row row : sheet) {
            int currentRowNumber = row.getRowNum();

            // Check for header row and skip
            int positionPR1 = inputTemplate.getPositionPR1();
            Cell pr1Cell = row.getCell(positionPR1);
            if (pr1Cell != null && pr1Cell.getCellType() == CellType.STRING) {
                String cellValue = pr1Cell.getStringCellValue();
                if (cellValue.contains("PR1") || cellValue.contains("Punctaj") || cellValue.contains("TRUE")) {
                    logger.atDebug().addArgument(currentRowNumber)
                          .log("Skipping header row at line {}");
                    continue;
                }
            }

            // Check for empty row
            int noNotNull = countNotNullValues(row);
            if (noNotNull == 0) {
                consecutiveEmptyRows++;
                if (consecutiveEmptyRows > MAX_CONSECUTIVE_EMPTY_ROWS_TRUEFALSE) {
                    logger.atInfo().log("Stopping processing: too many consecutive empty rows");
                    break;
                }
                continue;
            }

            // Reset counter when we find a non-empty row
            consecutiveEmptyRows = 0;

            // Create new question for this row
            Question question = new Question();
            question.setCrtNo(currentRowNumber);
            question.setType(QuestionType.TRUEFALSE);
            question.setQuizAuthor(quizAuthor);

            // Validate row has minimum required values
            if (noNotNull < MIN_TRUEFALSE_VALUES) {
                quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_VALUES_LESS_THAN_6);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }

            convertRowToTrueFalseQuestion(quizAuthor, row, question, inputTemplate);
            checkTrueFalseQuestionTotalPoint(quizAuthor, question);
            quizAuthor.getQuestions().add(question);
        }
        return "finish parsing of the true-false sheet";
    }

    /**
     * Process a sheet containing multiple choice questions.
     *
     * Expected format:
     * - Column positions defined in InputTemplate
     * - Minimum 11 non-null values per valid row
     * - Stops after 20 consecutive empty rows
     *
     * @param quizAuthor The QuizAuthor association to add questions to
     * @param sheet The Excel sheet containing multichoice questions
     * @param inputTemplate Template configuration for column positions
     * @return Status message
     */
    public String processMultichoiceSheet(QuizAuthor quizAuthor, Sheet sheet, InputTemplate inputTemplate) {
        quizAuthor.setQuizErrors(new HashSet<>());
        quizAuthor.setQuestions(new HashSet<>());

        if (!validateMinimumQuestions(sheet, quizAuthor)) {
            return "error parsing file";
        }

        TemplateType templateType = detectTemplateType(sheet);
        int consecutiveEmptyRows = 0;
        int currentRowNumber = 0;

        for (Row row : sheet) {
            currentRowNumber = row.getRowNum();

            if (isHeaderRow(row, inputTemplate, currentRowNumber)) {
                continue;
            }

            int noNotNull = countNotNullValues(row);

            if (noNotNull == 0) {
                consecutiveEmptyRows++;
                if (consecutiveEmptyRows > MAX_CONSECUTIVE_EMPTY_ROWS_MULTICHOICE) {
                    logger.atInfo().log("Stopping processing: too many consecutive empty rows");
                    break;
                }
                continue;
            }

            consecutiveEmptyRows = 0;
            processMultichoiceRow(quizAuthor, row, inputTemplate, templateType, currentRowNumber, noNotNull);
        }

        logger.atInfo()
              .addArgument(quizAuthor.getAuthor().getName())
              .addArgument(currentRowNumber)
              .log("Finish parsing multi choice sheet for '{}' with {} rows");
        return "finish parsing multichoice sheet";
    }

    /**
     * Validate that sheet has minimum required questions
     */
    private boolean validateMinimumQuestions(Sheet sheet, QuizAuthor quizAuthor) {
        if (sheet.getLastRowNum() < MIN_QUESTIONS_PER_SHEET) {
            QuizError quizError1 = new QuizError();
            quizError1.setDescription(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            quizError1.setQuizAuthor(quizAuthor);
            quizAuthor.getQuizErrors().add(quizError1);
            quizAuthorRepository.save(quizAuthor);
            logger.atInfo().log(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            return false;
        }
        return true;
    }

    /**
     * Check if row is a header row that should be skipped
     */
    private boolean isHeaderRow(Row row, InputTemplate inputTemplate, int rowNumber) {
        int positionPR1 = inputTemplate.getPositionPR1();
        Cell pr1Cell = row.getCell(positionPR1);
        if (pr1Cell != null && pr1Cell.getCellType() == CellType.STRING) {
            String cellValue = pr1Cell.getStringCellValue();
            if (cellValue.contains("PR1") || cellValue.contains("Punctaj")) {
                logger.atDebug().addArgument(rowNumber)
                      .log("Skipping header row at line {}");
                return true;
            }
        }
        return false;
    }

    /**
     * Process a single multichoice row
     */
    private void processMultichoiceRow(QuizAuthor quizAuthor, Row row, InputTemplate inputTemplate,
                                       TemplateType templateType, int rowNumber, int noNotNull) {
        Question question = new Question();
        question.setCrtNo(rowNumber);
        question.setType(QuestionType.MULTICHOICE);
        question.setQuizAuthor(quizAuthor);

        if (noNotNull < MIN_MULTICHOICE_VALUES) {
            QuizError quizErrorMissingValue = new QuizError();
            quizErrorMissingValue.setDescription(MyUtil.MISSING_VALUES_LESS_THAN_11);
            quizErrorMissingValue.setQuizAuthor(quizAuthor);
            quizErrorMissingValue.setRowNumber(rowNumber);
            quizAuthor.getQuizErrors().add(quizErrorMissingValue);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }

        convertRowToQuestion(quizAuthor, row, question, inputTemplate, templateType);
        repairQuestionPoints(question);
        checkQuestionTotalPoint(quizAuthor, question);
        checkAllAnswersArePresent(question);

        if (!question.getTitle().equals(MyUtil.SKIPPED_DUE_TO_ERROR)) {
            quizAuthor.getQuestions().add(question);
        }
    }

    private void checkAllAnswersArePresent(Question question) {
        if (question.getResponse1() == null || question.getResponse2() == null ||
                question.getResponse3() == null || question.getResponse4() == null) {
            quizErrorService.addAuthorError(question.getQuizAuthor(), question, MyUtil.MISSING_ANSWER);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    private TemplateType detectTemplateType(Sheet sheet) {
        TemplateType type = null;
        for (Row row : sheet) {
            int notNulls = countNotNullValues(row);
            if (notNulls != 0) {
                type = (notNulls == 11) ? TemplateType.Template2023 : TemplateType.Template2024;
                break;
            }
        }
        return type;
    }

    private void repairQuestionPoints(Question question) {
        // First pass: Repair 33% values (for 3-out-of-4 correct answers)
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

        // Second pass: Handle 100% single correct answer case
        int correctIndex = findSingleCorrectAnswerIndex(question);
        if (correctIndex != -1) {
            setAllIncorrectTo100(question, correctIndex);
        }
    }

    private boolean isApproximately33(Double weight) {
        if (weight == null) return false;
        int intValue = weight.intValue();
        return intValue > WEIGHT_33_LOWER_BOUND && intValue < WEIGHT_33_UPPER_BOUND;
    }

    private int findSingleCorrectAnswerIndex(Question question) {
        if (question.getWeightResponse1().intValue() == 100) return 1;
        if (question.getWeightResponse2().intValue() == 100) return 2;
        if (question.getWeightResponse3().intValue() == 100) return 3;
        if (question.getWeightResponse4().intValue() == 100) return 4;
        return -1;
    }

    private void setAllIncorrectTo100(Question question, int correctIndex) {
        if (correctIndex != 1) question.setWeightResponse1(-100.0);
        if (correctIndex != 2) question.setWeightResponse2(-100.0);
        if (correctIndex != 3) question.setWeightResponse3(-100.0);
        if (correctIndex != 4) question.setWeightResponse4(-100.0);
    }

    
    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    
    public Question findQuestionById(Long id) {
        return questionRepository.findOne(
            QuestionSpecification.byId(id)
        ).orElse(null);
    }

    @Transactional
    public boolean deleteQuestion(Long id) {
        Question question = findQuestionById(id);
        if (question != null) {
            questionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    
    public List<Question> findAllQuestions() {
        return (List<Question>) questionRepository.findAll();
    }

    public void convertRowToQuestion(QuizAuthor quizAuthor, Row row, Question question,
                                     InputTemplate inputTemplate, TemplateType templateType) {
        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        convertCellToDouble(quizAuthor, cellNrCrt, question);

        validateAndSetTitle(quizAuthor, row, question, inputTemplate);
        validateAndSetQuestionText(quizAuthor, row, question, inputTemplate);

        Cell cellPR1 = row.getCell(inputTemplate.getPositionPR1());
        question.setWeightResponse1(convertCellToDouble(quizAuthor, cellPR1, question));

        Cell cellResponse1 = row.getCell(inputTemplate.getPositionResponse1());
        question.setResponse1(cleanAndConvert(getValueAsString(quizAuthor, cellResponse1, question)));

        Cell cellPR2 = row.getCell(inputTemplate.getPositionPR2());
        question.setWeightResponse2(convertCellToDouble(quizAuthor, cellPR2, question));

        Cell cellResponse2 = row.getCell(inputTemplate.getPositionResponse2());
        question.setResponse2(cleanAndConvert(getValueAsString(quizAuthor, cellResponse2, question)));

        Cell cellPR3 = row.getCell(inputTemplate.getPositionPR3());
        question.setWeightResponse3(convertCellToDouble(quizAuthor, cellPR3, question));

        Cell cellResponse3 = row.getCell(inputTemplate.getPositionResponse3());
        question.setResponse3(cleanAndConvert(getValueAsString(quizAuthor, cellResponse3, question)));

        Cell cellPR4 = row.getCell(inputTemplate.getPositionPR4());
        question.setWeightResponse4(convertCellToDouble(quizAuthor, cellPR4, question));

        Cell cellResponse4 = row.getCell(inputTemplate.getPositionResponse4());
        question.setResponse4(cleanAndConvert(getValueAsString(quizAuthor, cellResponse4, question)));

        if (templateType == TemplateType.Template2024) {
            Cell cellCourse = row.getCell(inputTemplate.getPositionCourse());
            question.setChapter(cleanAndConvert(getValueAsString(quizAuthor, cellCourse, question)));
        }
    }

    /**
     * Validate and set title from cell, handling all error cases
     */
    private void validateAndSetTitle(QuizAuthor quizAuthor, Row row, Question question, InputTemplate inputTemplate) {
        Cell cellTitlu = row.getCell(inputTemplate.getPositionTitle());

        if (cellTitlu == null) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_TITLE);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellTitlu.getCellType() == CellType.NUMERIC) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.TITLE_NOT_STRING);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellTitlu.getCellType() == CellType.STRING) {
            String title = cellTitlu.getStringCellValue();
            if (title.length() < 2) {
                quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_TITLE);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (MyUtil.forbiddenTitles.contains(title)) {
                quizErrorService.addAuthorError(quizAuthor, question, MyUtil.REMOVE_TEMPLATE_QUESTION + title);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else {
                question.setTitle(cleanAndConvert(title));
            }
        } else {
            question.setTitle(cleanAndConvert(question.getTitle()));
        }
    }

    /**
     * Validate and set question text from cell, handling all error cases
     */
    private void validateAndSetQuestionText(QuizAuthor quizAuthor, Row row, Question question, InputTemplate inputTemplate) {
        Cell cellText = row.getCell(inputTemplate.getPositionText());

        if (cellText == null) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellText.getCellType() == CellType.NUMERIC) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.DATATYPE_ERROR);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            return;
        }

        if (cellText.getCellType() == CellType.STRING) {
            String questionText = cellText.getStringCellValue();
            if (questionText.isEmpty()) {
                quizErrorService.addAuthorError(quizAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else {
                question.setText(cleanAndConvert(questionText));
            }
        }
    }

    public void convertRowToTrueFalseQuestion(QuizAuthor quizAuthor, Row row, Question question,
                                              InputTemplate inputTemplate) {
        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        convertCellToDouble(quizAuthor, cellNrCrt, question);

        validateAndSetTitle(quizAuthor, row, question, inputTemplate);
        validateAndSetQuestionText(quizAuthor, row, question, inputTemplate);

        Cell cellPRTrue = row.getCell(inputTemplate.getPositionPRTrue());
        question.setWeightTrue(convertCellToDouble(quizAuthor, cellPRTrue, question));

        Cell cellPRFalse = row.getCell( inputTemplate.getPositionPRFalse());
        question.setWeightFalse(convertCellToDouble(quizAuthor, cellPRFalse, question));

        Cell cellResponse1 = row.getCell(inputTemplate.getPositionResponse1());
        question.setResponse1(cleanAndConvert(getValueAsString(quizAuthor, cellResponse1, question)));
    }

    public void checkQuestionTotalPoint(QuizAuthor quizAuthor, Question question) {
        double total = question.getWeightResponse1() + question.getWeightResponse2() + question.getWeightResponse3() + question.getWeightResponse4();
        if (question.getWeightResponse1().intValue() == 100 || question.getWeightResponse2().intValue() == 100 || question.getWeightResponse3().intValue() == 100 || question.getWeightResponse4().intValue() == 100) {
            if (total != 100 && total != -200) {
                quizErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }
        } else if (isOneAnswerEqual33(question) && total > 1) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_3_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (isOneAnswerEqual50(question) && total != 0) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_2_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (total == 0 && !isOneAnswerEqual33(question) && !isOneAnswerEqual50(question)) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_POINTS);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    private static boolean isOneAnswerEqual33(Question question) {
        return question.getWeightResponse1().intValue() == 33 || question.getWeightResponse2().intValue() == 33 || question.getWeightResponse3().intValue() == 33 || question.getWeightResponse4()
                                                                                                                                                                             .intValue() == 33;
    }

    private static boolean isOneAnswerEqual50(Question question) {
        return question.getWeightResponse1().intValue() == 50 || question.getWeightResponse2().intValue() == 50 || question.getWeightResponse3().intValue() == 50 || question.getWeightResponse4()
                                                                                                                                                                             .intValue() == 50;
    }

    public void checkTrueFalseQuestionTotalPoint(QuizAuthor quizAuthor, Question question) {
        double total = question.getWeightFalse() + question.getWeightTrue();
        if ((question.getWeightTrue() == 100 && total != 100) || (question.getWeightFalse() == 100 && total != 100)) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_TRUE_FALSE_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    
    public void deleteAllQuestions() {
        questionRepository.deleteAll();
    }


    public Quiz saveQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    
    public List<Question> getQuizzQuestionsForAuthor(Long id) {
        // Refactored: Use Specification-based filtering for all question queries
        Specification<Question> spec = QuestionSpecification.byFilters(null, id, null, null);
        return questionRepository.findAll(spec);
    }

    public List<Question> getQuestionsForAuthorId(Long authorId, String course) {
        Specification<Question> spec = QuestionSpecification.byFilters(course, authorId, null, null);
        return questionRepository.findAll(spec);
    }

    public List<Question> getQuestionsForAuthorName(String authorName) {
        Specification<Question> spec = QuestionSpecification.hasAuthorName(authorName);
        return questionRepository.findAll(spec);
    }

    public List<QuestionDto> getQuestionsByQuizId(Long quizId) {
        Specification<Question> spec = QuestionSpecification.byFilters(null, null, quizId, null);
        List<Question> questions = questionRepository.findAll(spec);
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(questionMapper.toDto(question));
        }
        return questionDtos;
    }

    /**
     * Returns a list of QuestionDto for a given author ID and filter.
     * @param authorId the author ID
     * @param filter the filter string
     * @return list of QuestionDto
     */
    public List<QuestionDto> getQuestionDtosForAuthorId(Long authorId, String filter) {
        List<Question> questions = getQuestionsForAuthorId(authorId, filter);
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(questionMapper.toDto(question));
        }
        return questionDtos;
    }

    /**
     * Returns a list of QuestionDto for a given author name.
     * @param authorName the author name
     * @return list of QuestionDto
     */
    public List<QuestionDto> getQuestionDtosForAuthorName(String authorName) {
        List<Question> questions = getQuestionsForAuthorName(authorName);
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(questionMapper.toDto(question));
        }
        return questionDtos;
    }

    public String removeSpecialChars(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        text = text.replace("–", "-");
        text = text.replace("„", "\"");
        text = text.replace("…", "...");
        text = text.replace("—", "-");
        text = text.replace("\n", " ");
        text = text.replace("\r", " ");
        text = text.replace("\t", " ");
        text = text.replace("&", " ");

        // Remove multiple spaces more efficiently
        text = text.replaceAll("\\s+", " ");
        text = text.trim();

        return text;
    }

    public String removeEnumerations(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[A-Da-d1-4]\\.|[A-Da-d1-4]\\)", "");
    }

    public double convertCellToDouble(QuizAuthor quizAuthor, Cell cell, Question question) {
        if (cell == null) {
            question.setCrtNo(-1);
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_POINTS);
            return 0.0;
        }

        try {
            CellType cellType = cell.getCellType();

            double result = switch (cellType) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> {
                    String strValue = cell.getStringCellValue().trim();
                    if (strValue.isEmpty()) {
                        quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_POINTS);
                        yield 0.0;
                    }
                    yield Double.parseDouble(strValue);
                }
                case BLANK -> {
                    quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_POINTS);
                    yield 0.0;
                }
                default -> {
                    question.setCrtNo(cell.getRowIndex());
                    quizErrorService.addAuthorError(quizAuthor, question,
                                                    MyUtil.NOT_NUMERIC_COLUMN + " (Type: " + cellType + ")");
                    yield 0.0;
                }
            };

            // Validate range for question weights
            if (result < MIN_WEIGHT || result > MAX_WEIGHT) {
                quizErrorService.addAuthorError(quizAuthor, question,
                                                "Invalid weight value: " + result + " (must be between -100 and 100)");
            }

            return result;

        } catch (NumberFormatException e) {
            quizErrorService.addAuthorError(quizAuthor, question,
                                            MyUtil.DATATYPE_ERROR + ": Cannot parse '" + cell.getStringCellValue() + "' as number");
            logger.atError().setCause(e)
                  .addArgument(cell.getStringCellValue())
                  .log("Cannot parse cell value as number: {}");
            return 0.0;
        } catch (Exception e) {
            quizErrorService.addAuthorError(quizAuthor, question,
                                            MyUtil.DATATYPE_ERROR + ": " + e.getMessage());
            logger.atError().setCause(e).log("Unexpected error converting cell to double");
            return 0.0;
        }
    }

    public String getExceptionClassName(Exception e) {
        return e.getClass().getName().replaceAll(".*\\.", "");
    }

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

    public String getValueAsString(QuizAuthor quizAuthor, Cell cell, Question question) {
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
                quizErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_ANSWER);
            }
        } catch (Exception e) {
            quizErrorService.addAuthorError(quizAuthor, question, MyUtil.DATATYPE_ERROR);
            String logMsg = getExceptionClassName(e) + " " + e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            quizErrorService.addAuthorError(quizAuthor, question, logMsg);
        }
        return result;
    }

    // REST API operation implementations
    
    public List<QuestionDto> getAllQuestions() {
        List<Question> questions = findAllQuestions();
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(questionMapper.toDto(question));
        }
        return questionDtos;
    }

    
    @Transactional(readOnly = true)
    public QuestionDto getQuestionById(Long id) {
        Question question = findQuestionById(id);
        if (question != null) {
            QuestionDto dto = questionMapper.toDto(question);
            questionDtoEnricher.enrichWithErrors(dto, question);
            return dto;
        }
        return null;
    }

    @Transactional
    public QuestionDto createQuestion(QuestionDto questionDto) {
        Question question = questionMapper.toEntity(questionDto);

        readOrCreateDefaultQuestionData(questionDto, question);

        Question savedQuestion = saveQuestion(question);
        return questionMapper.toDto(savedQuestion);
    }

    private void readOrCreateDefaultQuestionData(QuestionDto questionDto, Question question) {
        String course = createCourse(questionDto);
        Quiz quiz = createQuiz(questionDto, course);
        Author author = createAuthor(questionDto);
        if (author != null) {
            QuizAuthor quizAuthor = createQuizAuthor(quiz, author);
            question.setQuizAuthor(quizAuthor);
        }
    }

    private QuizAuthor createQuizAuthor(Quiz quiz, Author author) {
        Optional<QuizAuthor> optQuizAuthor = quizAuthorRepository.findOne(
            QuizAuthorSpecification.byQuizAndAuthor(quiz.getId(), author.getId())
        );
        QuizAuthor quizAuthor;
        if (optQuizAuthor.isPresent()) {
            quizAuthor = optQuizAuthor.get();
        } else {
            quizAuthor = new QuizAuthor();
            quizAuthor.setQuiz(quiz);
            quizAuthor.setAuthor(author);
            quizAuthorRepository.save(quizAuthor);
        }
        return quizAuthor;
    }

    private Author createAuthor(QuestionDto questionDto) {
        String authorName = questionDto.getAuthorName();
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

    private Quiz createQuiz(QuestionDto questionDto, String course) {
        Specification<Quiz> spec = QuizSpecification.byCourse(course);
        List<Quiz> quizzes = quizRepository.findAll(spec);
        Quiz quiz = quizzes.isEmpty() ? null : quizzes.getFirst();
        if (quiz == null) {
            quiz = new Quiz();
            quiz.setCourse(course);
            quiz.setName(questionDto.getQuizName());
            saveQuiz(quiz);
            quiz = quizRepository.findAll(spec).getFirst();
        }
        return quiz;
    }

    private String createCourse(QuestionDto questionDto) {
        String course = questionDto.getCourse();
        Specification<Course> spec = CourseSpecification.byCourseName(course);
        List<Course> courseDtos = courseRepository.findAll(spec);
        if (courseDtos.isEmpty()
                && (course != null && course.equals("Unknown Course"))
        ) {
            CourseDto courseDto = new CourseDto();
            courseDto.setCourse(course);
            courseService.createCourse(courseDto);
        }
        return course;
    }

    @Transactional
    public QuestionDto updateQuestion(QuestionDto questionDto) {
        if (questionDto.getId() == null) {
            return null;
        }
        Question existingQuestion = findQuestionById(questionDto.getId());
        if (existingQuestion == null) {
            return null;
        }

        Question question = questionMapper.toEntity(questionDto);
        readOrCreateDefaultQuestionData(questionDto, question);

        question.setId(questionDto.getId());
        Question savedQuestion = saveQuestion(question);
        return questionMapper.toDto(savedQuestion);
    }


    public List<QuestionDto> getQuestionsByCourse(String course, Integer page, Integer pageSize) {
        // Convert 1-based page number to 0-based page index for Spring Data
        int pageIndex = (page != null && page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Specification<Question> spec = QuestionSpecification.byFilters(course, null, null, null);
        Page<Question> questions = questionRepository.findAll(spec, pageable);
        return getQuestionDtosSortedByRow(questions);
    }

    
    @Transactional(readOnly = true)
    public QuestionFilterDto getQuestionsFiltered(String course, Long authorId,
                                                  Integer page, Integer pageSize,
                                                  Long quizId, QuestionType questionType) {
        // Convert 1-based page number to 0-based page index for Spring Data
        int pageIndex = (page != null && page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Specification<Question> spec = QuestionSpecification.byFilters(course, authorId, quizId, questionType);

        long startTime = System.currentTimeMillis();
        Page<Question> questions = questionRepository.findAll(spec, pageable);
        long queryTime = System.currentTimeMillis() - startTime;

        logger.atInfo().log("Fetched page {} ({} questions of {} total) in {}ms for filters: course='{}', authorId={}, quizId={}, type={}",
            page, questions.getNumberOfElements(), questions.getTotalElements(), queryTime, course, authorId, quizId, questionType);

        List<QuestionDto> questionDtos = getQuestionDtosSortedByRow(questions);
        QuestionFilterDto dto = new QuestionFilterDto();
        dto.setQuestions(questionDtos);
        // Return 1-based page number for frontend
        dto.setCurrentPage(questions.getNumber() + 1);
        dto.setTotalPages(questions.getTotalPages());
        dto.setSelectedCourse(course);
        dto.setSelectedAuthorId(authorId);
        dto.setSelectedQuizId(quizId);
        dto.setTotalItems(questions.getTotalElements());
        return dto;
    }

    private List<QuestionDto> getQuestionDtosSortedByRow(Page<Question> questions) {
        List<QuestionDto> questionDtos = new ArrayList<>(questions.getNumberOfElements());

        for (Question question : questions.getContent()) {
            QuestionDto dto = questionMapper.toDto(question);
            questionDtos.add(dto);
        }
        // Sort by row number (crtNo)
        questionDtos.sort((q1, q2) -> {
            if (q1.getRow() == null) return 1;
            if (q2.getRow() == null) return -1;
            return q1.getRow().compareTo(q2.getRow());
        });
        return questionDtos;
    }


    
    public void updateQuestionAuthorRelationship(Question question, String authorName) {
        if (authorName == null || authorName.trim().isEmpty()) {
            question.setQuizAuthor(null);
            return;
        }

        String trimmedAuthorName = authorName.trim();

        AuthorDto authorDto = authorService.getAuthorByName(trimmedAuthorName);
        Author authorEntity;
        if (authorDto == null) {
            // Create new author via DTO service
            AuthorDto newAuthorDto = new AuthorDto();
            newAuthorDto.setName(trimmedAuthorName);
            newAuthorDto.setInitials(authorService.extractInitials(trimmedAuthorName));
            newAuthorDto = authorService.saveAuthorDto(newAuthorDto);
            authorEntity = authorRepository.findById(newAuthorDto.getId()).orElse(null);
            logger.info("Created new author: {}", trimmedAuthorName);
        } else {
            authorEntity = authorRepository.findById(authorDto.getId()).orElse(null);
        }

        if (authorEntity == null) {
            logger.error("Could not resolve Author entity for name: {}", trimmedAuthorName);
            return;
        }

        QuizAuthor existingQuizAuthor = question.getQuizAuthor();
        if (existingQuizAuthor != null && existingQuizAuthor.getAuthor() != null && existingQuizAuthor.getAuthor().getId().equals(authorEntity.getId())) {
            return; // relationship already correct
        }

        QuizAuthor quizAuthor = existingQuizAuthor != null ? existingQuizAuthor : new QuizAuthor();
        quizAuthor.setAuthor(authorEntity);
        question.setQuizAuthor(quizAuthor);
        logger.info("Updated question author relationship to: {}", trimmedAuthorName);
    }
}
