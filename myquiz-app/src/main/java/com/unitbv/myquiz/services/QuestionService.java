package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.util.TemplateType;
import com.unitbv.myquizapi.dto.QuestionDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.util.List;

public interface QuestionService {
    int parseExcelFilesFromFolder(Quiz quiz, File folder, int noFilesInput);
    Author saveAuthorName(File folder);
    String readAndParseFirstSheetFromExcelFile(Quiz quiz, Author author, String filePath);
    String processTruefalseSheet(QuizAuthor quizAuthor, Sheet sheet);
    String processMultichoiceSheet(QuizAuthor quizAuthor, Sheet sheet);
    Question saveQuestion(Question question);  // Updated to return Question instead of void
    void convertRowToQuestion(QuizAuthor quizAuthor, Row row, Question question);
    void convertRowToTrueFalseQuestion(QuizAuthor quizAuthor, Row row, Question question);
    void checkQuestionTotalPoint(QuizAuthor quizAuthor, Question question);
    void checkTrueFalseQuestionTotalPoint(QuizAuthor quizAuthor, Question question);
    String removeSpecialChars(String text);
    String removeEnumerations(String text);
    double convertCellToDouble(QuizAuthor quizAuthor, Cell cell, Question question);
    String cleanAndConvert(String text);
    int countNotNullValues(Row row);
    String getValueAsString(QuizAuthor quizAuthor, Cell cell, Question question);
    List<Question> getQuestionsForAuthorId(Long authorId, String course);
    List<Question> getQuestionsForAuthorName(String authorName);
    void deleteAllQuestions();
    TemplateType getTemplateType();
    void setTemplateType(TemplateType value);
    Quiz saveQuiz(Quiz quiz);
    List<Question> getQuizzQuestionsForAuthor(Long id);

    // Question Editor CRUD operations
    Question findQuestionById(Long id);
    boolean deleteQuestion(Long id);
    List<Question> findAllQuestions();
    
    // REST API operations
    List<QuestionDto> getAllQuestions();
    QuestionDto getQuestionById(Long id);
    QuestionDto createQuestion(QuestionDto questionDto);
    QuestionDto updateQuestion(QuestionDto questionDto);
    List<QuestionDto> getQuestionsByQuizId(Long quizId);
    List<QuestionDto> getQuestionsByCourse(String course);

    List<QuestionDto> getQuestionsFiltered(String course, Long authorId);
    int parseExcelFilesFromFlatFolder(Quiz quiz, File folder);
}
