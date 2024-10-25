package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.util.TemplateType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface QuestionService {
    int parseExcelFilesFromFolder(Quiz quiz, File folder, int noFilesInput);
    Author saveAuthorName(File folder);
    String readAndParseFirstSheetFromExcelFile(Quiz quiz, Author author, String filePath);
    String processTruefalseSheet(QuizAuthor quizAuthor, Sheet sheet);
    String processMultichoiceSheet(QuizAuthor quizAuthor, Sheet sheet);
    void saveQuestion(Question question);
    void convertRowToQuestion(QuizAuthor quizAuthor, Row row, Question question);
    void convertRowToTrueFalseQuestion(QuizAuthor quizAuthor, Row row, Question question);
    void checkQuestionTotalPoint(QuizAuthor quizAuthor, Question question);
    void checkTrueFalseQuestionTotalPoint(QuizAuthor quizAuthor, Question question);
    QuizError checkQuestionStrings(Question question);
    String removeSpecialChars(String text);
    String removeEnumerations(String text);
    double convertCellToDouble(QuizAuthor quizAuthor, Cell cell, Question question);
    String cleanAndConvert(String text);
    int countNotNullValues(Row row);
    String getValueAsString(QuizAuthor quizAuthor, Cell cell, Question question);
    boolean checkAllTitlesForDuplicates(Question question);
    List<String> putAllTitlesToList();
    boolean checkAllAnswersForDuplicates(Question question);
    List<String> putAllQuestionsToList();
    List<Question> getQuestionsForAuthorId(Long authorId);
    List<Question> getQuestionsForAuthorName(String authorName);
    void deleteAllQuestions();
    TemplateType getTemplateType();
    void setTemplateType(TemplateType value);
    void checkDuplicatesQuestionsForAuthors(ArrayList<Author> authorsList);
    Quiz saveQuiz(Quiz quiz);
}
