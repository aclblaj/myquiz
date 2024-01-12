package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.repositories.QuestionRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
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
public class QuestionService {


    Logger logger = org.slf4j.LoggerFactory.getLogger(QuestionService.class);

    @Autowired
    AuthorService authorService;

    @Autowired
    AuthorErrorService authorErrorService;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    EncodingSevice encodingSevice;

    @Autowired
    @Qualifier("readAndParseFileTaskExecutor")
    private Executor readAndParseFileThreadPool;

    Author author;
    String initials;


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
            String authorName = authorService.getAuthorName(folder.getAbsolutePath());
            initials = authorService.extractInitials(authorName);
            author = new Author();
            author.setName(authorName);
            author.setInitials(initials);
            author = authorService.saveAuthor(author);

            // read and process files in parallel
            CompletableFuture<String> message = CompletableFuture.supplyAsync(
                    () -> this.readAndParseFirstSheetFromExcelFile(folder.getAbsolutePath()),
                    readAndParseFileThreadPool
            );

            String msg;
            try {
                msg = message.get();
                logger.info("File {} processed with result: {}", folder.getAbsolutePath(), msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Thread interrupted {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Exception {}", e.getMessage());
            }
            noFiles++;
        } else {
            logger.info("Not readable target file: {}", folder.getAbsolutePath());
            Question question = new Question();
            question.setAuthor(author);
            question.setCrtNo(-1);
            authorErrorService.addAuthorError(author, question, MyUtil.ERROR_WRONG_FILE_TYPE);
        }
        return noFiles;
    }

    private String readAndParseFirstSheetFromExcelFile(String filePath) {
        String result = "ready";
        logger.info("Start parse excel file: {}", filePath);

        try (FileInputStream fileInputStream = new FileInputStream(filePath);Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet
            Question question = new Question();
            question.setAuthor(author);
            question.setCrtNo(0);
            question.setType(QuestionType.MULTICHOICE);

            if (sheet.getLastRowNum() < 15) {
                authorErrorService.addAuthorError(author, question, MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
                logger.info(MyUtil.INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS);
                return "error parsing file";
            }

            // Iterate over rows
            for (Row row : sheet) {
                int currentRowNumber = row.getRowNum();

                question = new Question();
                question.setAuthor(author);
                question.setCrtNo(currentRowNumber);
                question.setType(QuestionType.MULTICHOICE);

                boolean isHeaderRow = false;

                if (row.getCell(3) != null) {
                    if (row.getCell(3).getCellType() == CellType.STRING && (row.getCell(3).getStringCellValue().contains("PR1") || row.getCell(3).getStringCellValue().contains("Punctaj"))) {
                        isHeaderRow = true;
                        continue; // skip header line
                    }
                }

                int noNotNull = countNotNullValues(row);
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

                checkQuestionStrings(question);

                saveQuestion(question);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "ready";
    }

    private void saveQuestion(Question question) {
        if (!question.getTitle().equals(MyUtil.SKIPPED_DUE_TO_ERROR)) {
            if (!addQuestion(question)) {
                authorErrorService.addAuthorError(author, question, MyUtil.UNALLOWED_CHARS);
            }
        }
    }

    private void convertRowToQuestion(Row row, Question question) {
        double cellNrCrtDouble = 0.0;

        Cell cellNrCrt = row.getCell(0);
        cellNrCrtDouble = convertCellToDouble(cellNrCrt, question);

        Cell cellTitlu = row.getCell(1);
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
        Cell cellText = row.getCell(2);
        if (cellText == null) {
            authorErrorService.addAuthorError(author, question, MyUtil.EMPTY_QUESTION_TEXT);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        } else {
            if (cellText.getCellType() == CellType.NUMERIC) {
                authorErrorService.addAuthorError(author, question, MyUtil.DATATYPE_ERROR);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (cellText.getCellType() == CellType.STRING) {
                questionText = cellText.getStringCellValue();
                if (questionText.length() == 0) {
                    authorErrorService.addAuthorError(author, question, MyUtil.EMPTY_QUESTION_TEXT);
                    question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
                }
            }
        }
        question.setText(cleanAndConvert(questionText));

        Cell cellPR1 = row.getCell(3);
        question.setWeightResponse1(convertCellToDouble(cellPR1, question));

        Cell cellResponse1 = row.getCell(4);
        question.setResponse1(cleanAndConvert(getValueAsString(cellResponse1, question)));

        Cell cellPR2 = row.getCell(5);
        question.setWeightResponse2(convertCellToDouble(cellPR2, question));

        Cell cellResponse2 = row.getCell(6);
        question.setResponse2(cleanAndConvert(getValueAsString(cellResponse2, question)));

        Cell cellPR3 = row.getCell(7);
        question.setWeightResponse3(convertCellToDouble(cellPR3, question));

        Cell cellResponse3 = row.getCell(8);
        question.setResponse3(cleanAndConvert(getValueAsString(cellResponse3, question)));

        Cell cellPR4 = row.getCell(9);
        question.setWeightResponse4(convertCellToDouble(cellPR4, question));

        Cell cellResponse4 = row.getCell(10);
        question.setResponse4(cleanAndConvert(getValueAsString(cellResponse4, question)));
    }

    private void checkQuestionTotalPoint(Question question) {
        double total = question.getWeightResponse1() + question.getWeightResponse2() + question.getWeightResponse3() + question.getWeightResponse4();
        if (question.getWeightResponse1() == 25 && total != 100) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_4_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
        if ((question.getWeightResponse1().intValue() == 33 || question.getWeightResponse2().intValue() == 33 || question.getWeightResponse3().intValue() == 33 || question.getWeightResponse4().intValue() == 33) && total > 1) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_3_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
        if ((question.getWeightResponse1().intValue() == 50 || question.getWeightResponse2().intValue() == 50 || question.getWeightResponse3().intValue() == 50 || question.getWeightResponse4().intValue() == 50) && total != 0) {
            authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_2_4_POINTS_WRONG);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
        if (question.getWeightResponse1().intValue() == 100 || question.getWeightResponse2().intValue() == 100 || question.getWeightResponse3().intValue() == 100 || question.getWeightResponse4().intValue() == 100) {
            if (total != 100 && total != -200) {
                authorErrorService.addAuthorError(author, question, MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }
        }
    }

    private void checkQuestionStrings(Question question) {
        if (question.getResponse1() != null && question.getResponse2() != null && question.getResponse3() != null && question.getResponse4() != null) {
            if (checkAllAnswersForDuplicates(question)) {
                authorErrorService.addAuthorError(author, question, MyUtil.REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            } else if (checkAllTitlesForDuplicates(question.getTitle())) {
                authorErrorService.addAuthorError(author, question, MyUtil.REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS);
                question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
            }
        } else {
            authorErrorService.addAuthorError(author, question, MyUtil.MISSING_ANSWER);
            question.setTitle(MyUtil.SKIPPED_DUE_TO_ERROR);
        }
    }

    private static String removeSpecialChars(String text) {
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


    private static String removeEnumerations(String text) {
        text = text.replaceAll("[A-Da-d1-4]\\.|[A-Da-d1-4]\\)", "");
        return text;
    }

    private double convertCellToDouble(Cell cell, Question question) {
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
    private String cleanAndConvert(String text) {
        if (text == null) {
            return "";
        }

        text = encodingSevice.convertToUTF8(text);
        text = removeEnumerations(text);
        text = removeSpecialChars(text);

        return text;
    }

    private int countNotNullValues(Row row) {
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

    private String getValueAsString(Cell cell, Question question) {
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

    private boolean addQuestion(Question question) {
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

    private boolean checkAllTitlesForDuplicates(String title) {
        List<String> allTitles = putAllTitlesToList();
        if (title != null && allTitles.contains(title.toLowerCase())) {
            return true;
        }
        return false;
    }

    private List<String> putAllTitlesToList() {
        List<String> allTitles = new ArrayList<>();
        List<Question> allQuestionInstances = questionRepository.findAll(Pageable.unpaged()).getContent();
        for (Question question : allQuestionInstances) {
            allTitles.add(question.getTitle().toLowerCase());
        }
        return allTitles;
    }

    private boolean checkAllAnswersForDuplicates(Question question) {
        List<String> allQuestionsAnswers = putAllQuestionsToList();
        if (question.getResponse1() != null && allQuestionsAnswers.contains(question.getResponse1().toLowerCase())) {
            return true;
        }
        if (question.getResponse2() != null && allQuestionsAnswers.contains(question.getResponse2().toLowerCase())) {
            return true;
        }
        if (question.getResponse3() != null && allQuestionsAnswers.contains(question.getResponse3().toLowerCase())) {
            return true;
        }
        if (question.getResponse4() != null && allQuestionsAnswers.contains(question.getResponse4().toLowerCase())) {
            return true;
        }
        return false;
    }

    private List<String> putAllQuestionsToList() {
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

    /**
     * return all author questions
     * @param authorId
     * @return List<Question>
     */
    public List<Question> getQuestionsForAuthorId(Long authorId) {
        return questionRepository.findAllByAuthorId(authorId);
    }
}
