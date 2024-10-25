package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.repositories.QuestionRepository;
import com.unitbv.myquiz.util.InputTemplate;
import com.unitbv.myquiz.util.TemplateType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class QuestionServiceImpl implements QuestionService{


    Logger logger = LoggerFactory.getLogger(QuestionServiceImpl.class);

    AuthorService authorService;

    AuthorErrorService authorErrorService;

    QuestionRepository questionRepository;

    EncodingSevice encodingSevice;

    private Executor readAndParseFileThreadPool;

    Author author;
    String initials;
    private TemplateType templateType;

    private InputTemplate inputTemplate;

    List<String> allQuestionsAnswers;
    List<String> allTitles;
    List<Question> allQuestionInstances;

    @Autowired
    public QuestionServiceImpl(@Qualifier("readAndParseFileTaskExecutor") Executor readAndParseFileThreadPool, AuthorService authorService, AuthorErrorService authorErrorService, QuestionRepository questionRepository, EncodingSevice encodingSevice) {
        this.authorService = authorService;
        this.authorErrorService = authorErrorService;
        this.questionRepository = questionRepository;
        this.encodingSevice = encodingSevice;
        this.readAndParseFileThreadPool = readAndParseFileThreadPool;
    }

    public int parseExcelFilesFromFolder(File folder, int noFilesInput) {

        int noFiles = noFilesInput;
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {

                for (File file : files) {
                    noFiles = parseExcelFilesFromFolder(file, noFiles);
                }
            }
        } else if (folder.isFile() && folder.getName().endsWith(".xlsx")) {
            if (!saveAuthorName(folder)) {
                // read and process files in parallel
                CompletableFuture<String> message = CompletableFuture.supplyAsync(
                        () -> this.readAndParseFirstSheetFromExcelFile(folder.getAbsolutePath()),
                        readAndParseFileThreadPool
                );

                String msg;
                try {
                    msg = message.get();
                    logger.atInfo().addArgument(folder.getAbsolutePath()).addArgument(msg)
                          .log("File {} processed with result: {}");
                } catch (Exception e) {
                    logger.atError().log("Parse file exception ", e);
                }
                noFiles++;
            }
        } else {
            logger.atInfo().addArgument(folder.getAbsolutePath())
                  .log("Not readable target file: {}");
            if (!saveAuthorName(folder)) {
                author = authorService.getAuthorByName(author.getName());
            }
            Question question = new Question();
            question.setAuthor(author);
            question.setCrtNo(-1);
            authorErrorService.setSource(folder.getName());
            authorErrorService.addAuthorError(author, question, MyUtil.ERROR_WRONG_FILE_TYPE);
        }
        return noFiles;
    }

    /**
     * Save author name in the database - return true if author already exists
     * @param folder
     * @return boolean
     */
    public boolean saveAuthorName(File folder) {
        boolean result = false;
        String authorName = authorService.extractAuthorNameFromPath(folder.getAbsolutePath());
        initials = authorService.extractInitials(authorName);
        author = new Author();
        author.setName(authorName);
        author.setInitials(initials);
        if (authorService.authorNameExists(author.getName())) {
            result = true;
            logger.atInfo().addArgument(author.getName())
                  .log("Author {} already exists in the database");
            author = authorService.getAuthorByName(author.getName());
        } else {
            author = authorService.saveAuthor(author);
            authorService.addAuthorToList(author);
        }
        return result;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String readAndParseFirstSheetFromExcelFile(String filePath) {
        String message = "ready";
        logger.atInfo().addArgument(filePath)
              .log("Start parse excel file: {}");
        authorErrorService.setSource(filePath);

        try (FileInputStream fileInputStream = new FileInputStream(filePath);Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet
            message = processMultichoiceSheet(sheet); // first sheet contains the multi choices questions
            logger.atInfo().addArgument(message).log("First sheet processed '{}'");
            if (workbook.getNumberOfSheets()>1) {
                Sheet sheetTF = workbook.getSheetAt(1); // Get the second sheet - true/false questions
                message = processTruefalseSheet(sheetTF); // second sheet contains the true/false questions
                logger.atInfo().addArgument(message).log("Second sheet processed '{}'");
            } else {
                logger.atInfo().log("Single sheet workbook");
            }
            message = "Finish parsing of the excel sheets";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }

    public String processTruefalseSheet(Sheet sheet) {
        Question question = new Question();
        question.setAuthor(author);
        question.setCrtNo(0);
        question.setType(QuestionType.TRUEFALSE);

        int consecutiveEmptyRows = 0;
        for (Row row : sheet) {
            int currentRowNumber = row.getRowNum();

            question = new Question();
            question.setAuthor(author);
            question.setCrtNo(currentRowNumber);
            question.setType(QuestionType.TRUEFALSE);

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
                authorErrorService.addAuthorError(author, question, MyUtil.MISSING_VALUES_LESS_THAN_6);
                if (isHeaderRow) {
                    break;
                } else {
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }

            convertRowToTrueFalseQuestion(row, question);
            checkTrueFalseQuestionTotalPoint(question);
            saveQuestion(question);
        }
        return "finish parsing of the true-false sheet";
    }

    public String processMultichoiceSheet(Sheet sheet) {
        Question question = new Question();
        question.setAuthor(author);
        question.setCrtNo(0);
        question.setType(QuestionType.MULTICHOICE);

        if (sheet.getLastRowNum() < 15) {
            authorErrorService.addAuthorError(author, question, MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            logger.atInfo().log(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
            return "error parsing file";
        }

        templateType = sheet.getRow(0).getPhysicalNumberOfCells() == 11 ? TemplateType.Template2023 : TemplateType.Template2024;
        inputTemplate = new InputTemplate(templateType);

        int consecutiveEmptyRows = 0;
        int currentRowNumber = 0;
        // Iterate over rows
        for (Row row : sheet) {
            currentRowNumber = row.getRowNum();

            question = new Question();
            question.setAuthor(author);
            question.setCrtNo(currentRowNumber);
            question.setType(QuestionType.MULTICHOICE);

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
                authorErrorService.addAuthorError(author, question, MyUtil.MISSING_VALUES_LESS_THAN_11);
                if (isHeaderRow) {
                    break;
                } else {
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }

            convertRowToQuestion(row, question);
            checkQuestionTotalPoint(question);
            //checkQuestionStrings(question);
            saveQuestion(question);
        }
        logger.atInfo()
              .addArgument(author.getName())
              .addArgument(currentRowNumber)
              .log("Finish parsing multi choice sheet for '{}' with {} rows");
        return "finish parsing multichoice sheet";
    }

    public void saveQuestion(Question question) {
        if (!question.getTitle().equals(MyUtil.SKIPPED_DUE_TO_ERROR)) {
            if (!addQuestion(question)) {
                authorErrorService.addAuthorError(author, question, MyUtil.UNALLOWED_CHARS);
            }
        }
    }

    public void convertRowToQuestion(Row row, Question question) {
        double cellNrCrtDouble = 0.0;

        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        cellNrCrtDouble = convertCellToDouble(cellNrCrt, question);

        Cell cellTitlu = row.getCell(inputTemplate.getPositionTitle());
        if (cellTitlu == null) {
            authorErrorService.addAuthorError(author, question, MyUtil.MISSING_TITLE);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellTitlu.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(author, question, MyUtil.TITLE_NOT_STRING);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellTitlu.getCellType() == CellType.STRING) {
                question.setTitle(cellTitlu.getStringCellValue());
                if (question.getTitle().length() < 2) {
                    authorErrorService.addAuthorError(author, question, MyUtil.MISSING_TITLE);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                } else {
                    if (MyUtil.forbiddenTitles.contains(question.getTitle())) {
                        authorErrorService.addAuthorError(author, question, MyUtil.REMOVE_TEMPLATE_QUESTION + question.getTitle());
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    }
                }
            }
        }
        question.setTitle(cleanAndConvert(question.getTitle()));

        String questionText = "";
        Cell cellText = row.getCell(inputTemplate.getPositionText());
        if (cellText == null) {
            authorErrorService.addAuthorError(author, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellText.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(author, question, MyUtil.DATATYPE_ERROR);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellText.getCellType() == CellType.STRING) {
                questionText = cellText.getStringCellValue();
                if (questionText.isEmpty()) {
                    authorErrorService.addAuthorError(author, question, MyUtil.EMPTY_QUESTION_TEXT);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }
        }
        question.setText(cleanAndConvert(questionText));

        Cell cellPR1 = row.getCell(inputTemplate.getPositionPR1());
        question.setWeightResponse1(convertCellToDouble(cellPR1, question));

        Cell cellResponse1 = row.getCell(inputTemplate.getPositionResponse1());
        question.setResponse1(cleanAndConvert(getValueAsString(cellResponse1, question)));

        Cell cellPR2 = row.getCell(inputTemplate.getPositionPR2());
        question.setWeightResponse2(convertCellToDouble(cellPR2, question));

        Cell cellResponse2 = row.getCell(inputTemplate.getPositionResponse2());
        question.setResponse2(cleanAndConvert(getValueAsString(cellResponse2, question)));

        Cell cellPR3 = row.getCell(inputTemplate.getPositionPR3());
        question.setWeightResponse3(convertCellToDouble(cellPR3, question));

        Cell cellResponse3 = row.getCell(inputTemplate.getPositionResponse3());
        question.setResponse3(cleanAndConvert(getValueAsString(cellResponse3, question)));

        Cell cellPR4 = row.getCell(inputTemplate.getPositionPR4());
        question.setWeightResponse4(convertCellToDouble(cellPR4, question));

        Cell cellResponse4 = row.getCell(inputTemplate.getPositionResponse4());
        question.setResponse4(cleanAndConvert(getValueAsString(cellResponse4, question)));
        if (templateType == TemplateType.Template2024) {
            Cell cellCourse = row.getCell(inputTemplate.getPositionCourse());
            question.setCourse(cleanAndConvert(getValueAsString(cellCourse, question)));
        }
    }

    public void convertRowToTrueFalseQuestion(Row row, Question question) {
        double cellNrCrtDouble = 0.0;

        Cell cellNrCrt = row.getCell(inputTemplate.getPositionNO());
        cellNrCrtDouble = convertCellToDouble(cellNrCrt, question);

        Cell cellTitlu = row.getCell(inputTemplate.getPositionTitle());
        if (cellTitlu == null) {
            authorErrorService.addAuthorError(author, question, MyUtil.MISSING_TITLE);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellTitlu.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(author, question, MyUtil.TITLE_NOT_STRING);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellTitlu.getCellType() == CellType.STRING) {
                question.setTitle(cellTitlu.getStringCellValue());
                if (question.getTitle().length() < 2) {
                    authorErrorService.addAuthorError(author, question, MyUtil.MISSING_TITLE);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                } else {
                    if (MyUtil.forbiddenTitles.contains(question.getTitle())) {
                        authorErrorService.addAuthorError(author, question, MyUtil.REMOVE_TEMPLATE_QUESTION + question.getTitle());
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    }
                }
            }
        }
        question.setTitle(cleanAndConvert(question.getTitle()));

        String questionText = "";
        Cell cellText = row.getCell(inputTemplate.getPositionText());
        if (cellText == null) {
            authorErrorService.addAuthorError(author, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellText.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(author, question, MyUtil.DATATYPE_ERROR);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellText.getCellType() == CellType.STRING) {
                questionText = cellText.getStringCellValue();
                if (questionText.isEmpty()) {
                    authorErrorService.addAuthorError(author, question, MyUtil.EMPTY_QUESTION_TEXT);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }
        }
        question.setText(cleanAndConvert(questionText));

        Cell cellPRTrue = row.getCell(inputTemplate.getPositionPRTrue());
        question.setWeightTrue(convertCellToDouble(cellPRTrue, question));

        Cell cellPRFalse = row.getCell( inputTemplate.getPositionPRFalse());
        question.setWeightFalse(convertCellToDouble(cellPRFalse, question));

        Cell cellResponse1 = row.getCell(inputTemplate.getPositionResponse1());
        question.setResponse1(cleanAndConvert(getValueAsString(cellResponse1, question)));
    }

    public void checkQuestionTotalPoint(Question question) {
        double total = question.getWeightResponse1() + question.getWeightResponse2() + question.getWeightResponse3() + question.getWeightResponse4();
        if (question.getWeightResponse1() == 25 && total != 100) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_4_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (isOneAnswerEqual33(question) && total > 1) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_3_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (isOneAnswerEqual50(question) && total != 0) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_2_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (total == 0 && !isOneAnswerEqual33(question) && !isOneAnswerEqual50(question)) {
            authorErrorService.addAuthorError(author, question, MyUtil.MISSING_POINTS);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else if (question.getWeightResponse1().intValue() == 100 || question.getWeightResponse2().intValue() == 100 || question.getWeightResponse3().intValue() == 100 || question.getWeightResponse4().intValue() == 100) {
            if (total != 100 && total != -200) {
                authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }
        }
//        else {
//            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_POINTS_WRONG);
//            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
//        }
    }

    private static boolean isOneAnswerEqual33(Question question) {
        return question.getWeightResponse1().intValue() == 33 || question.getWeightResponse2().intValue() == 33 || question.getWeightResponse3().intValue() == 33 || question.getWeightResponse4()
                                                                                                                                                                             .intValue() == 33;
    }

    private static boolean isOneAnswerEqual50(Question question) {
        return question.getWeightResponse1().intValue() == 50 || question.getWeightResponse2().intValue() == 50 || question.getWeightResponse3().intValue() == 50 || question.getWeightResponse4()
                                                                                                                                                                             .intValue() == 50;
    }

    public void checkTrueFalseQuestionTotalPoint(Question question) {
        double total = question.getWeightFalse() + question.getWeightTrue();
        if ((question.getWeightTrue() == 100 && total != 100) || (question.getWeightFalse() == 100 && total != 100)) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_TRUE_FALSE_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    public void checkQuestionStrings(Question question) {
        switch (question.getType()) {
            case MULTICHOICE:
                if (hasAllAnswers(question)) {
                    if (checkAllAnswersForDuplicates(question)) {
                        authorErrorService.addAuthorError(question.getAuthor(), question, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    } else if (checkAllTitlesForDuplicates(question)) {
                        authorErrorService.addAuthorError(question.getAuthor(), question, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
                        question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                    }
                } else {
                    authorErrorService.addAuthorError(question.getAuthor(), question, MyUtil.MISSING_ANSWER);
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
    }

    private static boolean hasAllAnswers(Question question) {
        return question.getResponse1() != null &&
                question.getResponse2() != null &&
                question.getResponse3() != null &&
                question.getResponse4() != null;
    }

    public String removeSpecialChars(String text) {
        text = text.replace("–", "-");
        text = text.replace("„", "\"");
        text = text.replace("”", "\"");
        text = text.replace("’", "'");
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

    public double convertCellToDouble(Cell cell, Question question) {
        double result = 0.0;
        try {
            if (cell == null) {
                question.setCrtNo(-1);
                authorErrorService.addAuthorError(author, question, MyUtil.MISSING_POINTS);
            } else {
                CellType cellType = cell.getCellType();
                if (cellType == CellType.STRING) {
                    result = Double.parseDouble(cell.getStringCellValue());
                } else if (cellType == CellType.NUMERIC) {
                    result = cell.getNumericCellValue();
                } else {
                    question.setCrtNo(cell.getRowIndex());
                    authorErrorService.addAuthorError(author, question, MyUtil.NOT_NUMERIC_COLUMN);
                }
            }
        } catch (Exception e) {
            authorErrorService.addAuthorError(author, question, MyUtil.DATATYPE_ERROR);
            String logMsg = e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            authorErrorService.addAuthorError(author, question, logMsg);
        }
        return result;
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
                    if (cell.getStringCellValue().length() > 0) {
                        count++;
                    }
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    count++;
                }
            }
        }
        return count;
    }

    public String getValueAsString(Cell cell, Question question) {
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
                authorErrorService.addAuthorError(author, question, MyUtil.MISSING_ANSWER);
            }
        } catch (Exception e) {
            authorErrorService.addAuthorError(author, question, MyUtil.DATATYPE_ERROR);
            String logMsg = e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            authorErrorService.addAuthorError(author, question, logMsg);
        }
        return result;
    }

    public boolean addQuestion(Question question) {
        try {
            questionRepository.save(question);
            return true;
        } catch (Exception e) {
            String logMsg = e.getMessage();
            if (logMsg.length() > 512) {
                logMsg = logMsg.substring(0, 512);
            }
            authorErrorService.addAuthorError(author, question, logMsg);
            return false;
        }
    }

    public boolean checkAllTitlesForDuplicates(String title) {
        List<String> allTitles = putAllTitlesToList();
        return title != null && allTitles.stream().filter(s -> s.equals(title.toLowerCase())).count() > 1;
    }

    public List<String> putAllTitlesToList() {
        List<String> allTitles = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question question : allQuestionInstances) {
            allTitles.add(question.getTitle().toLowerCase());
        }
        return allTitles;
    }

    public boolean checkAllAnswersForDuplicates(Question question) {
        List<String> allQuestionsAnswers = putAllQuestionsToList();
        if (question.getResponse1() != null && allQuestionsAnswers.stream().filter(responseText -> responseText.equals(question.getResponse1().toLowerCase())).count() > 1 ) {
            return true;
        }
        if (question.getResponse2() != null && allQuestionsAnswers.stream().filter(responseText -> responseText.equals(question.getResponse2().toLowerCase())).count() > 1 ) {
            return true;
        }
        if (question.getResponse3() != null && allQuestionsAnswers.stream().filter(responseText -> responseText.equals(question.getResponse3().toLowerCase())).count() > 1 ) {
            return true;
        }
        if (question.getResponse4() != null && allQuestionsAnswers.stream().filter(responseText -> responseText.equals(question.getResponse4().toLowerCase())).count() > 1 ) {
            return true;
        }
        return false;
    }

    public List<String> putAllQuestionsToList() {
        List<String> allAnswers = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question question : allQuestionInstances) {
            if (question.getResponse1() != null) allAnswers.add(question.getResponse1().toLowerCase());
            if (question.getResponse2() != null) allAnswers.add(question.getResponse2().toLowerCase());
            if (question.getResponse3() != null) allAnswers.add(question.getResponse3().toLowerCase());
            if (question.getResponse4() != null) allAnswers.add(question.getResponse4().toLowerCase());
        }
        return allAnswers;
    }

    public void findAllQuestions() {
        allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
    }

    /**
     * return all author questions
     * @param authorId
     * @return List<Question>
     */
    public List<Question> getQuestionsForAuthorId(Long authorId) {
        return questionRepository.findAllByAuthorId(authorId);
    }

    public List<Question> getQuestionsForAuthorName(String authorName) {
        return questionRepository.findAllByAuthor_NameContainsIgnoreCase(authorName);
    }

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
    public void checkDuplicatesQuestionsForAuthors(ArrayList<Author> authors) {
        authors.forEach(author -> {
            List<Question> questions = getQuestionsForAuthorId(author.getId());
            findAllQuestions();
            setAllQuestionsAnswers(putAllQuestionsToListExceptFromAuthor(author.getId()));
            setAllTitles(putAllTitlesToListExceptFromAuthor(author.getId()));
            questions.stream().forEach(this::checkQuestionStrings);
            logger.atInfo().addArgument(author.getName()).addArgument(questions.size())
                  .log("Author: {} - Number of questions: {}");
        });
    }

    public List<String> getAllQuestionsAnswers() {
        return allQuestionsAnswers;
    }

    public void setAllQuestionsAnswers(List<String> allQuestionsAnswers) {
        this.allQuestionsAnswers = allQuestionsAnswers;
    }

    public List<String> getAllTitles() {
        return allTitles;
    }

    public void setAllTitles(List<String> allTitles) {
        this.allTitles = allTitles;
    }
}
