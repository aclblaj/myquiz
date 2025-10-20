package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquizapi.dto.AuthorErrorDto;
import org.springframework.ui.Model;

import java.util.List;

public interface AuthorErrorService {
    void addAuthorError(QuizAuthor author, Question question, String description);

    List<QuizError> getErrorsForAuthorName(String authorName);

    List<QuizError> getErrors(String course);

    void setSource(String filePath);

    String getSourceFile();

    void deleteAll();

    void saveAllAuthorErrors(List<QuizError> quizErrors);

    List<QuizError>  getErrorsForQuizAuthor(Long quizAuthorId);

    void getAuthorErrorsModel(Model model, String selectedCourse, String author);

    int getComparedTo(AuthorErrorDto o1, AuthorErrorDto o2);

    List<AuthorErrorDto> sortAuthorErrorsByRow(List<AuthorErrorDto> authorErrorDtos);
    
    List<AuthorErrorDto> getErrorsByQuizId(Long quizId);

    List<String> getAvailableCourses();
}
