package com.unitbv.myquiz.app.testutil;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;

/**
 * Shared constants and lightweight builders used by myquiz-app service tests.
 */
public final class ServiceTestData {
    public static final String DEFAULT_DUPLICATE_COURSE = "BD";
    public static final int DEFAULT_MAX_QUESTIONS = 200;

    public static final String DEFAULT_FOLDER_PATH = "C:\\Temp\\myquiz\\inpQ1\\";
    public static final String DEFAULT_EXCEL_PATH = "C:\\Downloads\\sablonInpQ.xlsx";

    public static final String PARSE_FOLDER_COURSE_PREFIX = "BD-TEST-";
    public static final String PARSE_FOLDER_DESCRIPTION = "Datenbanken";
    public static final String PARSE_FOLDER_QUESTION_BANK_PREFIX = "Q1-";

    public static final String SINGLE_FILE_COURSE = "BD-NetAlg";
    public static final String SINGLE_FILE_COURSE_DESCRIPTION = "BD-Network Algorithms";
    public static final String SINGLE_FILE_QUESTION_BANK = "NetAlg";

    public static final String ARCHIVE_COURSE = "26-SSI";
    public static final String ARCHIVE_COURSE_DESCRIPTION = "Securitatea sistemelor informatice";
    public static final String ARCHIVE_QUESTION_BANK = "Q1";
    public static final String ARCHIVE_FILE_NAME = "2026-SSI-inpQ1 25 intrebari - Kap. 1-4 - Erste Teil - Fragen Vorbereitung-39230.zip";

    public static final String DB_TEST_COURSE = "TEST_DB_CONN";
    public static final String DB_TEST_COURSE_DESCRIPTION = "Test Database Connectivity";
    public static final String DB_PERSIST_TEST_COURSE = "TEST_DB_CONN_PERSIST";
    public static final String DB_PERSIST_TEST_COURSE_DESCRIPTION = "Test Database Connectivity - Persistent";

    public static final String AUTHOR_NAME = "Erika Diana Mustermann";
    public static final String AUTHOR_INITIALS = "EDM";
    public static final String AUTHOR_QUESTION_BANK = "Q1";
    public static final String AUTHOR_QUESTION_BANK_COURSE = "RC";
    public static final String AUTHOR_SOURCE_FILE = "File-RC.xlsx";
    public static final StudyYear AUTHOR_STUDY_YEAR = StudyYear.Y2024_2025;

    public static final String AUTHOR_QUESTION_1_TITLE = "Title Q1";
    public static final String AUTHOR_QUESTION_1_TEXT = "Text Q1";
    public static final String AUTHOR_QUESTION_2_TITLE = "Title Q22";
    public static final String AUTHOR_QUESTION_2_TEXT = "Text Q2";

    public static final String COURSE = "RC";
    public static final String QUESTION_BANK_NAME = "Q1";
    public static final StudyYear STUDY_YEAR = StudyYear.Y2024_2025;
    public static final String SOURCE_FILE = "file.xlsx";
    public static final String QUESTION_TITLE = "Test question";
    public static final String QUESTION_TEXT = "Repository test question text";

    private ServiceTestData() {
    }

    public static CourseDtoBuilder courseDtoBuilder() {
        return new CourseDtoBuilder();
    }

    public static TestEntityFactory.QuestionBankAuthorSpec.Builder questionBankAuthorSpecBuilder() {
        return TestEntityFactory.QuestionBankAuthorSpec.builder()
                .authorName(AUTHOR_NAME)
                .initials(AUTHOR_INITIALS)
                .questionBankName(AUTHOR_QUESTION_BANK)
                .course(AUTHOR_QUESTION_BANK_COURSE)
                .studyYear(AUTHOR_STUDY_YEAR)
                .source(AUTHOR_SOURCE_FILE);
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

    public static QuestionBuilder questionBuilder() {
        return new QuestionBuilder();
    }

    public static final class CourseDtoBuilder {
        private final CourseDto courseDto = new CourseDto();

        public CourseDtoBuilder course(String course) {
            courseDto.setCourse(course);
            return this;
        }

        public CourseDtoBuilder description(String description) {
            courseDto.setDescription(description);
            return this;
        }

        public CourseDtoBuilder semester(String semester) {
            courseDto.setSemester(semester);
            return this;
        }

        public CourseDtoBuilder universityYear(String universityYear) {
            courseDto.setUniversityYear(universityYear);
            return this;
        }

        public CourseDto build() {
            return courseDto;
        }
    }

    public static final class QuestionBuilder {
        private final Question question = new Question();

        public QuestionBuilder crtNo(int crtNo) {
            question.setCrtNo(crtNo);
            return this;
        }

        public QuestionBuilder title(String title) {
            question.setTitle(title);
            return this;
        }

        public QuestionBuilder text(String text) {
            question.setText(text);
            return this;
        }

        public QuestionBuilder type(QuestionType type) {
            question.setType(type);
            return this;
        }

        public QuestionBuilder questionBankAuthor(QuestionBankAuthor questionBankAuthor) {
            question.setQuestionBankAuthor(questionBankAuthor);
            return this;
        }

        public Question build() {
            return question;
        }
    }
}


