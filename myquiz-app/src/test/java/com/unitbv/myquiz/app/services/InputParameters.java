package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.types.StudyYear;

@lombok.Data
public class InputParameters {
    private final String dirPath;
    private final String course;
    private final String questionBank;
    private final StudyYear studyYear;

    InputParameters(String dirPath, String course, String questionBank, StudyYear studyYear) {
        this.dirPath = dirPath;
        this.course = course;
        this.questionBank = questionBank;
        this.studyYear = studyYear;
    }
}
