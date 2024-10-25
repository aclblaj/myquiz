package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.entities.Question;

import java.util.List;

public interface AuthorErrorService {
    void addAuthorError(QuizAuthor author, Question question, String description);

    List<QuizError> getErrorsForAuthorName(String authorName);

    List<QuizError> getErrors();

    void setSource(String filePath);

    String getSourceFile();

    void deleteAll();

    void saveAllAuthorErrors(List<QuizError> quizErrors);
}
