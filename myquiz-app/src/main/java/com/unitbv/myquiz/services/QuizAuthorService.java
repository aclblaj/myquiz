package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.QuizAuthor;

import java.util.List;

public interface QuizAuthorService {
    void deleteAll();
    List<QuizAuthor> getQuizAuthorsForAuthorName(String authorName);
}
