package com.unitbv.myquiz.services.impl;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.repositories.QuizRepository;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.EncodingSevice;
import com.unitbv.myquiz.services.MyUtil;
import com.unitbv.myquiz.services.QuestionService;
import com.unitbv.myquiz.util.InputTemplate;
import com.unitbv.myquiz.util.TemplateType;
import com.unitbv.myquizapi.dto.QuestionDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.springframework.util.StringUtils.getFilename;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuizRepository quizRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuestionServiceImpl.class);

    private final AuthorService authorService;
    private final AuthorErrorService authorErrorService;
    private final QuestionRepository questionRepository;
    private final EncodingSevice encodingSevice;
    private final QuizAuthorRepository quizAuthorRepository;

    private String initials;
    private TemplateType templateType;
    private InputTemplate inputTemplate;

    @Autowired
    public QuestionServiceImpl(AuthorService authorService,
                               AuthorErrorService authorErrorService,
                               QuestionRepository questionRepository,
                               EncodingSevice encodingSevice,
                               QuizAuthorRepository quizAuthorRepository,
                               QuizRepository quizRepository) {
        this.authorService = authorService;
        this.authorErrorService = authorErrorService;
        this.questionRepository = questionRepository;
        this.encodingSevice = encodingSevice;
        this.quizAuthorRepository = quizAuthorRepository;
        this.quizRepository = quizRepository;
    }

    public int parseExcelFilesFromFolder(Quiz quiz, File folder, int noFilesInput) {
        var ref = new Object() {
            int noFiles = noFilesInput;
        };
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                Arrays.stream(files).sorted().forEach(file ->
                    ref.noFiles = parseExcelFilesFromFolder(quiz, file, ref.noFiles));
            }
        } else if (folder.isFile() && folder.getName().endsWith(".xlsx")) {
            Author author = saveAuthorName(folder);
            if (author != null) {
                this.readAndParseFirstSheetFromExcelFile(quiz, author, folder.getAbsolutePath());
                ref.noFiles++;
            }
        } else {
            logger.atInfo().addArgument(folder.getAbsolutePath())
                  .log("Not readable target file: {}");
            Author author = saveAuthorName(folder);
            Question question = new Question();
            question.setCrtNo(-1);
            QuizAuthor quizAuthor = new QuizAuthor();
            quizAuthor.setAuthor(author);
            quizAuthor.setQuiz(quiz);
            quizAuthorRepository.save(quizAuthor);
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.ERROR_WRONG_FILE_TYPE);
        }
        return ref.noFiles;
    }


    public int parseExcelFilesFromFlatFolder(Quiz quiz, File folder) {
        var ref = new Object() {
            int noFiles = 0;
        };
        File[] files = folder.listFiles();
        if (files != null) {
            Arrays.stream(files).sorted().forEach(file -> {
                if (file.isFile() && file.getName().endsWith(".xlsx")) {
                    //extract author full name from the path - the substring until first underscore
                    String authorFullName = file.getName().substring(0, file.getName().indexOf("_"));

                    Author author = saveAuthorIfNotExists(authorFullName);
                    if (author != null) {
                        this.readAndParseFirstSheetFromExcelFile(quiz, author, file.getAbsolutePath());
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
                    authorErrorService.addAuthorError(quizAuthor, question, MyUtil.ERROR_WRONG_FILE_TYPE);
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
        Author author = new Author();
        author.setName(authorName);
        author.setInitials(initials);
        if (authorService.authorNameExists(author.getName())) {
            logger.atInfo().addArgument(author.getName())
                  .log("Author {} already exists in the database");
            author = authorService.getAuthorByName(author.getName());
        } else {
            author = authorService.saveAuthor(author);
        }
        authorService.addAuthorToList(author);
        return author;
    }

    public Author saveAuthorIfNotExists(String authorFullName) {
        initials = authorService.extractInitials(authorFullName);
        Author author = new Author();
        author.setName(authorFullName);
        author.setInitials(initials);
        if (authorService.authorNameExists(author.getName())) {
            logger.atInfo().addArgument(author.getName())
                  .log("Author {} already exists in the database");
            author = authorService.getAuthorByName(author.getName());
        } else {
            author = authorService.saveAuthor(author);
        }
        authorService.addAuthorToList(author);
        return author;
    }

    public String readAndParseFirstSheetFromExcelFile(Quiz quiz, Author author, String filePath) {
        String message;
        logger.atInfo().addArgument(filePath)
              .log("Start parse excel file: {}");
        authorErrorService.setSource(filePath);
        QuizAuthor quizAuthor = new QuizAuthor();
        quizAuthor.setAuthor(author);
        quizAuthor.setQuiz(quiz);
        quizAuthor.setSource(getFilename(filePath));
        quizAuthor = quizAuthorRepository.save(quizAuthor);

        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(0);
            message = processMultichoiceSheet(quizAuthor, sheet);
            logger.atInfo().addArgument(message).log("First sheet processed '{}'");
            if (workbook.getNumberOfSheets() > 1) {
                Sheet sheetTF = workbook.getSheetAt(1);
                message = processTruefalseSheet(quizAuthor, sheetTF);
                logger.atInfo().addArgument(message).log("Second sheet processed '{}'");
            } else {
                logger.atInfo().log("Single sheet workbook");
            }
            quizAuthorRepository.save(quizAuthor);
            message = "Finish parsing of the excel sheets";
        } catch (Exception e) {
            message = "Error processing file: " + e.getMessage();
            logger.atError().addArgument(filePath).addArgument(message)
                  .log("Error processing file: {} with message: {}");
        }
        return message;
    }

    public String processTruefalseSheet(QuizAuthor quizAuthor, Sheet sheet) {
        Question question = new Question();
        question.setQuizAuthor(quizAuthor);
        question.setCrtNo(0);
        question.setType(QuestionType.TRUEFALSE);

        int consecutiveEmptyRows = 0;
        for (Row row : sheet) {
            int currentRowNumber = row.getRowNum();
            question = new Question();
            question.setCrtNo(currentRowNumber);
            question.setType(QuestionType.TRUEFALSE);
            question.setQuizAuthor(quizAuthor);

            boolean isHeaderRow = false;

            int positionPR1 = inputTemplate.getPositionPR1();
            if (row.getCell(positionPR1) != null) {
                if ((row.getCell(positionPR1).getCellType() == CellType.STRING &&
                        (row.getCell(positionPR1).getStringCellValue().contains("PR1")
                                || row.getCell(positionPR1).getStringCellValue().contains("Punctaj")
                                || row.getCell(positionPR1).getStringCellValue().contains("TRUE")))
                    || (row.getCell(positionPR1).getCellType() == CellType.BOOLEAN)) {
                    isHeaderRow = true;
                    continue; // skip header line
                }
            }

            int noNotNull = countNotNullValues(row);
            if (noNotNull == 0) {
                consecutiveEmptyRows++;
                // stop processing when more than 3 consecutive empty rows detected
                if (consecutiveEmptyRows > 2) {
                    break;
                } else {
                    continue;
                }
            }
            if (noNotNull < 6) {
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_VALUES_LESS_THAN_6);
                if (isHeaderRow) {
                    break;
                } else {
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }

            convertRowToTrueFalseQuestion(quizAuthor, row, question);
            checkTrueFalseQuestionTotalPoint(quizAuthor, question);
            quizAuthor.getQuestions().add(question);
        }
        return "finish parsing of the true-false sheet";
    }

    public String processMultichoiceSheet(QuizAuthor quizAuthor, Sheet sheet) {
        Question question = new Question();
        question.setCrtNo(0);
        question.setType(QuestionType.MULTICHOICE);
        question.setQuizAuthor(quizAuthor);

        quizAuthor.setQuizErrors(new HashSet<>());
        quizAuthor.setQuestions(new HashSet<>());

        if (sheet.getLastRowNum() < 3) {
            QuizError quizError1 = new QuizError();
            quizError1.setDescription(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            quizError1.setQuizAuthor(quizAuthor);
            quizAuthor.getQuizErrors().add(quizError1);

            quizAuthorRepository.save(quizAuthor);

            logger.atInfo().log(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            return "error parsing file";
        }

        templateType = detectTemplateType(sheet);
        if (templateType != null) {
            quizAuthor.setTemplateType(templateType);
        } else {
            quizAuthor.setTemplateType(TemplateType.Other);
        }
        inputTemplate = new InputTemplate(templateType);

        int consecutiveEmptyRows = 0;
        int currentRowNumber = 0;
        // Iterate over rows
        for (Row row : sheet) {
            currentRowNumber = row.getRowNum();

            question = new Question();
            question.setCrtNo(currentRowNumber);
            question.setType(QuestionType.MULTICHOICE);
            question.setQuizAuthor(quizAuthor);

            boolean isHeaderRow = false;

            int positionPR1 = inputTemplate.getPositionPR1();
            if (row.getCell(positionPR1) != null) {
                if (row.getCell(positionPR1).getCellType() == CellType.STRING
                        && (row.getCell(positionPR1).getStringCellValue().contains("PR1")
                        || row.getCell(positionPR1).getStringCellValue().contains("Punctaj"))) {
                    isHeaderRow = true;
                    continue; // skip header line
                }
            }

            int noNotNull = countNotNullValues(row);
            if (noNotNull == 0) {
                consecutiveEmptyRows++;
                // stop processing when more than 3 consecutive empty rows detected
                if (consecutiveEmptyRows > 20) {
                    break;
                } else {
                    continue;
                }
            }
            if (noNotNull < 11) {
                QuizError quizErrorMissingValue = new QuizError();
                quizErrorMissingValue.setDescription(MyUtil.MISSING_VALUES_LESS_THAN_11);
                quizErrorMissingValue.setQuizAuthor(quizAuthor);
                quizErrorMissingValue.setRowNumber(currentRowNumber);
                quizAuthor.getQuizErrors().add(quizErrorMissingValue);

                if (isHeaderRow) {
                    break;
                } else {
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }

            convertRowToQuestion(quizAuthor, row, question);
            repairQuestionPoints(question);
            checkQuestionTotalPoint(quizAuthor, question);
            checkAllAnswersArePresent(question);
            if (!question.getTitle().equals(MyUtil.SKIPPED_DUE_TO_ERROR)) {
                quizAuthor.getQuestions().add(question);
            }

        }

        logger.atInfo()
              .addArgument(quizAuthor.getAuthor().getName())
              .addArgument(currentRowNumber)
              .log("Finish parsing multi choice sheet for '{}' with {} rows");
        return "finish parsing multichoice sheet";
    }

    private void checkAllAnswersArePresent(Question question) {
        if (question.getResponse1() == null || question.getResponse2() == null ||
                question.getResponse3() == null || question.getResponse4() == null) {
            authorErrorService.addAuthorError(question.getQuizAuthor(), question, MyUtil.MISSING_ANSWER);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    private TemplateType detectTemplateType(Sheet sheet) {
        TemplateType type = null;
        for (Row row : sheet) {
            int notNulls = countNotNullValues(row);
            if (notNulls==0) {
                continue; // skip empty lines
            } else {
                type = (notNulls == 11) ? TemplateType.Template2023 : TemplateType.Template2024;
                break;
            }
        }
        return type;
    }

    private void repairQuestionPoints(Question question) {
        int wR1 = question.getWeightResponse1().intValue();
        if (wR1 > 30 && wR1 < 35) {
            question.setWeightResponse1(33.33333);
            return;
        }
        int wR2 = question.getWeightResponse2().intValue();
        if (wR2 > 30 && wR2 < 35) {
            question.setWeightResponse2(33.33333);
            return;
        }
        int wR3 = question.getWeightResponse3().intValue();
        if (wR3 > 30 && wR3 < 35) {
            question.setWeightResponse3(33.33333);
            return;
        }
        int wR4 = question.getWeightResponse4().intValue();
        if (wR4 > 30 && wR4 < 35) {
            question.setWeightResponse4(33.33333);
            return;
        }

        if (wR1 == 100) {
            question.setWeightResponse2(-100.0);
            question.setWeightResponse3(-100.0);
            question.setWeightResponse4(-100.0);
        } else if (wR2 == 100) {
            question.setWeightResponse1(-100.0);
            question.setWeightResponse3(-100.0);
            question.setWeightResponse4(-100.0);
        } else if (wR3 == 100) {
            question.setWeightResponse1(-100.0);
            question.setWeightResponse2(-100.0);
            question.setWeightResponse4(-100.0);
        } else if (wR4 == 100) {
            question.setWeightResponse1(-100.0);
            question.setWeightResponse2(-100.0);
            question.setWeightResponse3(-100.0);
        }
    }

    @Override
    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    @Override
    public Question findQuestionById(Long id) {
        return questionRepository.findById(id).orElse(null);
    }

    @Override
    public boolean deleteQuestion(Long id) {
        Question question = findQuestionById(id);
        if (question != null) {
            questionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Question> findAllQuestions() {
        return (List<Question>) questionRepository.findAll();
    }

    public void convertRowToQuestion(QuizAuthor quizAuthor, Row row, Question question) {
        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        convertCellToDouble(quizAuthor, cellNrCrt, question);

        Cell cellTitlu = row.getCell(inputTemplate.getPositionTitle());
        if (cellTitlu == null) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_TITLE);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellTitlu.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.TITLE_NOT_STRING);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellTitlu.getCellType() == CellType.STRING) {
                question.setTitle(cellTitlu.getStringCellValue());
                if (question.getTitle().length() < 2) {
                    authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_TITLE);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                } else {
                    if (MyUtil.forbiddenTitles.contains(question.getTitle())) {
                        authorErrorService.addAuthorError(quizAuthor, question, MyUtil.REMOVE_TEMPLATE_QUESTION + question.getTitle());
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    }
                }
            }
        }
        question.setTitle(cleanAndConvert(question.getTitle()));

        String questionText = "";
        Cell cellText = row.getCell(inputTemplate.getPositionText());
        if (cellText == null) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellText.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.DATATYPE_ERROR);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellText.getCellType() == CellType.STRING) {
                questionText = cellText.getStringCellValue();
                if (questionText.isEmpty()) {
                    authorErrorService.addAuthorError(quizAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }
        }
        question.setText(cleanAndConvert(questionText));

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

    public void convertRowToTrueFalseQuestion(QuizAuthor quizAuthor, Row row, Question question) {
        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        convertCellToDouble(quizAuthor, cellNrCrt, question);

        Cell cellTitlu = row.getCell(inputTemplate.getPositionTitle());
        if (cellTitlu == null) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_TITLE);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellTitlu.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.TITLE_NOT_STRING);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellTitlu.getCellType() == CellType.STRING) {
                question.setTitle(cellTitlu.getStringCellValue());
                if (question.getTitle().length() < 2) {
                    authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_TITLE);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                } else {
                    if (MyUtil.forbiddenTitles.contains(question.getTitle())) {
                        authorErrorService.addAuthorError(quizAuthor, question, MyUtil.REMOVE_TEMPLATE_QUESTION + question.getTitle());
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    }
                }
            }
        }
        question.setTitle(cleanAndConvert(question.getTitle()));

        String questionText = "";
        Cell cellText = row.getCell(inputTemplate.getPositionText());
        if (cellText == null) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellText.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.DATATYPE_ERROR);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellText.getCellType() == CellType.STRING) {
                questionText = cellText.getStringCellValue();
                if (questionText.isEmpty()) {
                    authorErrorService.addAuthorError(quizAuthor, question, MyUtil.EMPTY_QUESTION_TEXT);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }
        }
        question.setText(cleanAndConvert(questionText));

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
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }
        } else if (isOneAnswerEqual33(question) && total > 1) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_3_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (isOneAnswerEqual50(question) && total != 0) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_2_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (total == 0 && !isOneAnswerEqual33(question) && !isOneAnswerEqual50(question)) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_POINTS);
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
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.TEMPLATE_ERROR_TRUE_FALSE_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    @Override
    public void deleteAllQuestions() {
        questionRepository.deleteAll();
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType value) {
        this.templateType = value;
    }

    @Override
    public Quiz saveQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    @Override
    public List<Question> getQuizzQuestionsForAuthor(Long id) {
        return questionRepository.findByQuizAuthorId(id);
    }

    @Override
    public List<Question> getQuestionsForAuthorId(Long authorId, String course) {
        return questionRepository.findByQuizAuthor_Author_IdAndQuizAuthor_Quiz_Course(authorId, course);
    }

    @Override
    public List<Question> getQuestionsForAuthorName(String authorName) {
        List<QuizAuthor> quizAuthors = quizAuthorRepository.findAllWithQuestionsByAuthorNameContainsIgnoreCase(authorName);
        List<Question> questions = new ArrayList<>();
        for (QuizAuthor quizAuthor : quizAuthors) {
            questions.addAll(quizAuthor.getQuestions());
        }
        return questions;
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
            questionDtos.add(convertToDto(question));
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
            questionDtos.add(convertToDto(question));
        }
        return questionDtos;
    }

    public String removeSpecialChars(String text) {
        text = text.replace("–", "-");
        text = text.replace("„", "\"");
//        text = text.replace(""", "\"");
        text = text.replace("'", "'");
        text = text.replace("…", "...");
        text = text.replace("–", "-");
        text = text.replace("—", "-");
        text = text.replace("\n", " ");
        text = text.replace("\r", " ");
        text = text.replace("\t", " ");
        text = text.replace("&", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.trim();
        return text;
    }

    public String removeEnumerations(String text) {
        text = text.replaceAll("[A-Da-d1-4]\\.|[A-Da-d1-4]\\)", "");
        return text;
    }

    public double convertCellToDouble(QuizAuthor quizAuthor, Cell cell, Question question) {
        double result = 0.0;
        try {
            if (cell == null) {
                question.setCrtNo(-1);
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_POINTS);
            } else {
                CellType cellType = cell.getCellType();
                if (cellType == CellType.STRING) {
                    result = Double.parseDouble(cell.getStringCellValue());
                } else if (cellType == CellType.NUMERIC) {
                    result = cell.getNumericCellValue();
                } else {
                    question.setCrtNo(cell.getRowIndex());
                    authorErrorService.addAuthorError(
                            quizAuthor, question,
                            MyUtil.NOT_NUMERIC_COLUMN + cell.getStringCellValue());
                }
            }
        } catch (Exception e) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.DATATYPE_ERROR);
            String logMsg = getExceptionClassName(e) + " " + e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            authorErrorService.addAuthorError(quizAuthor, question, logMsg);
        }
        return result;
    }

    public String getExceptionClassName(Exception e) {
        return e.getClass().getName().replaceAll(".*\\.", "");
    }

    public String cleanAndConvert(String text) {
        if (text == null) {
            return "";
        }
        text = encodingSevice.convertToUTF8(text);
        text = removeEnumerations(text);
        text = removeSpecialChars(text);
        return text;
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
                authorErrorService.addAuthorError(quizAuthor, question, MyUtil.MISSING_ANSWER);
            }
        } catch (Exception e) {
            authorErrorService.addAuthorError(quizAuthor, question, MyUtil.DATATYPE_ERROR);
            String logMsg = getExceptionClassName(e) + " " + e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            authorErrorService.addAuthorError(quizAuthor, question, logMsg);
        }
        return result;
    }

    // REST API operation implementations
    @Override
    public List<QuestionDto> getAllQuestions() {
        List<Question> questions = findAllQuestions();
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(convertToDto(question));
        }
        return questionDtos;
    }

    @Override
    public QuestionDto getQuestionById(Long id) {
        Question question = findQuestionById(id);
        return question != null ? convertToDto(question) : null;
    }

    @Override
    public QuestionDto createQuestion(QuestionDto questionDto) {
        Question question = convertToEntity(questionDto);
        Question savedQuestion = saveQuestion(question);
        return convertToDto(savedQuestion);
    }

    @Override
    public QuestionDto updateQuestion(QuestionDto questionDto) {
        if (questionDto.getId() == null) {
            return null;
        }
        Question existingQuestion = findQuestionById(questionDto.getId());
        if (existingQuestion == null) {
            return null;
        }
        Question question = convertToEntity(questionDto);
        question.setId(questionDto.getId());
        Question savedQuestion = saveQuestion(question);
        return convertToDto(savedQuestion);
    }


    @Override
    public List<QuestionDto> getQuestionsByQuizId(Long quizId) {
        List<Question> questions = questionRepository.findByQuizAuthor_Quiz_Id(quizId);
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(convertToDto(question));
        }
        return questionDtos;
    }

    private QuestionDto convertToDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setTitle(question.getTitle());
        dto.setText(question.getText());
        dto.setChapter(question.getChapter());
        dto.setRow(question.getCrtNo());
        
        // Add response fields
        dto.setResponse1(question.getResponse1());
        dto.setResponse2(question.getResponse2());
        dto.setResponse3(question.getResponse3());
        dto.setResponse4(question.getResponse4());
        
        // Add weight fields
        dto.setWeightResponse1(question.getWeightResponse1());
        dto.setWeightResponse2(question.getWeightResponse2());
        dto.setWeightResponse3(question.getWeightResponse3());
        dto.setWeightResponse4(question.getWeightResponse4());
        dto.setWeightTrue(question.getWeightTrue());
        dto.setWeightFalse(question.getWeightFalse());
        
        // Add author information
        if (question.getQuizAuthor() != null) {
            if (question.getQuizAuthor().getAuthor() != null) {
                dto.setAuthorName(question.getQuizAuthor().getAuthor().getName());
            }
            if (question.getQuizAuthor().getQuiz() != null) {
                dto.setCourse(question.getQuizAuthor().getQuiz().getCourse());
            }
        }
        
        return dto;
    }

    private Question convertToEntity(QuestionDto dto) {
        Question question = new Question();
        question.setTitle(dto.getTitle());
        question.setText(dto.getText());
        question.setChapter(dto.getChapter());
        return question;
    }

    @Override
    public List<QuestionDto> getQuestionsByCourse(String course) {
        List<Question> questions = questionRepository.findByQuizAuthor_Quiz_Course(course);
        List<QuestionDto> questionDtos = new ArrayList<>();
        
        for (Question question : questions) {
            questionDtos.add(convertToDto(question));
        }
        
        // Sort by row number (crtNo)
        questionDtos.sort((q1, q2) -> {
            if (q1.getRow() == null) return 1;
            if (q2.getRow() == null) return -1;
            return q1.getRow().compareTo(q2.getRow());
        });
        
        return questionDtos;
    }

    @Override
    public List<QuestionDto> getQuestionsFiltered(String course, Long authorId) {
        List<Question> questions;
        if (authorId != null) {
            questions = questionRepository.findByQuizAuthor_Quiz_CourseAndQuizAuthor_Author_Id(course, authorId);
        } else {
            questions = questionRepository.findByQuizAuthor_Quiz_Course(course);
        }
        List<QuestionDto> questionDtos = new ArrayList<>();
        for (Question question : questions) {
            questionDtos.add(convertToDto(question));
        }
        // Sort by row number (crtNo)
        questionDtos.sort((q1, q2) -> {
            if (q1.getRow() == null) return 1;
            if (q2.getRow() == null) return -1;
            return q1.getRow().compareTo(q2.getRow());
        });
        return questionDtos;
    }
}
