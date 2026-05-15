package com.unitbv.myquiz.app.testutil;

import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;

/**
 * Shared test data constants and preset spec builders used across integration tests.
 */
public final class TestFixtureData {
    public static final String AUTHOR_NAME = "Max Mustermann";
    public static final String AUTHOR_INITIALS = "MM";
    public static final String QUESTION_BANK_NAME = "Q1";
    public static final String COURSE = "RC";
    public static final StudyYear STUDY_YEAR = StudyYear.Y2024_2025;
    public static final String SOURCE_FILE = "file.xlsx";

    public static final String QUESTION_TITLE = "Test question";
    public static final String QUESTION_TEXT = "Repository test question text";

    private TestFixtureData() {
    }

    public static TestEntityFactory.QuestionBankAuthorSpec.Builder questionBankAuthorSpecBuilder() {
        return TestEntityFactory.QuestionBankAuthorSpec.builder()
                .authorName(AUTHOR_NAME)
                .initials(AUTHOR_INITIALS)
                .questionBankName(QUESTION_BANK_NAME)
                .course(COURSE)
                .studyYear(STUDY_YEAR)
                .source(SOURCE_FILE);
    }

    public static TestEntityFactory.QuestionSpec.Builder questionSpecBuilder() {
        return TestEntityFactory.QuestionSpec.builder()
                .authorName(AUTHOR_NAME)
                .initials(AUTHOR_INITIALS)
                .questionBankName(QUESTION_BANK_NAME)
                .course(COURSE)
                .studyYear(STUDY_YEAR)
                .source(SOURCE_FILE)
                .type(QuestionType.MULTICHOICE)
                .title(QUESTION_TITLE)
                .text(QUESTION_TEXT);
    }
}

