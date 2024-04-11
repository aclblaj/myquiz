package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.AuthorError;
import com.unitbv.myquiz.entities.Question;

import java.util.List;

public interface AuthorErrorService {
    void addAuthorError(Author author, Question question, String description);

    List<AuthorError> getErrorsForAuthorName(String authorName);

    List<AuthorError> getErrors();

    void setSource(String filePath);

    void deleteAll();
}
