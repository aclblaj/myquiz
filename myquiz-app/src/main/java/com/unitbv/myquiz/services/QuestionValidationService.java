package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import java.util.ArrayList;

public interface QuestionValidationService {
    void checkDuplicatesQuestionsForAuthors(ArrayList<Author> authors, String course);
}

