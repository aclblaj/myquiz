package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.util.TemplateType;

import java.io.File;

public interface QuizService {
    Quiz createQuizz(String course, String quizz, long year);

    void deleteAll();
}
