package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.util.TemplateType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface QuestionService {
    int parseExcelFilesFromFolder(File folder, int noFilesInput);
    boolean saveAuthorName(File folder);
    void setAuthor(Author author);
    String readAndParseFirstSheetFromExcelFile(String filePath);
    String processTruefalseSheet(Sheet sheet);
    String processMultichoiceSheet(Sheet sheet);
    void saveQuestion(Question question);
    void convertRowToQuestion(Row row, Question question);
    void convertRowToTrueFalseQuestion(Row row, Question question);
    void checkQuestionTotalPoint(Question question);
    void checkTrueFalseQuestionTotalPoint(Question question);
    void checkQuestionStrings(Question question);
    String removeSpecialChars(String text);
    String removeEnumerations(String text);
    double convertCellToDouble(Cell cell, Question question);
    String cleanAndConvert(String text);
    int countNotNullValues(Row row);
    String getValueAsString(Cell cell, Question question);
    boolean addQuestion(Question question);
    boolean checkAllTitlesForDuplicates(Question question);
    List<String> putAllTitlesToListExceptFromAuthor(Long authorId);
    boolean checkAllAnswersForDuplicates(Question question);
    List<String> putAllQuestionsToListExceptFromAuthor(Long authorId);
    List<Question> getQuestionsForAuthorId(Long authorId);
    List<Question> getQuestionsForAuthorName(String authorName);
    void deleteAllQuestions();
    TemplateType getTemplateType();
    void setTemplateType(TemplateType value);

    void checkDuplicatesQuestionsForAuthors(ArrayList<Author> authorsList);
}
