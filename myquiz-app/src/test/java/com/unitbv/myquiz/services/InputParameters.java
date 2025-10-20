package com.unitbv.myquiz.services;

@lombok.Data
public class InputParameters {
    private final String dirPath;
    private final String course;
    private final String quiz;
    private final long year;

    InputParameters (String dirPath, String course, String quiz, long year) {
        this.dirPath = dirPath;
        this.course = course;
        this.quiz = quiz;
        this.year = year;
    }
}
