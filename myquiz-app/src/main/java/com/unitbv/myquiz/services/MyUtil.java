package com.unitbv.myquiz.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class MyUtil {
    // array list with forbidden titles
    public static final List<String> forbiddenTitles = List.of("Kapazität Null", "Kommunikationsmuster", "ICMP", "Roluri 2PC", "IP header", "c02_scheduler_functie");

    // error messages
    public static final String MISSING_POINTS = "Missing points - empty value for points";
    public static final String MISSING_TITLE = "Missing title";
    public static final String MISSING_ANSWER = "Missing answer";
    public static final String NOT_NUMERIC_COLUMN = "Conversion error - column contains no numeric values";
    public static final String DATATYPE_ERROR = "Datatype error";
    public static final String REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS = "Answer already exists";
    public static final String REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS = "Title already exists";

    public static final String SKIPPED_DUE_TO_ERROR = "Skipped due to error";
    public static final String TEMPLATE_ERROR_1_4_POINTS_WRONG = "Template error - wrong sum for 1/4 points";
    public static final String TEMPLATE_ERROR_2_4_POINTS_WRONG = "Template error - wrong sum for 2/4 points";
    public static final String TEMPLATE_ERROR_3_4_POINTS_WRONG = "Template error - wrong sum for 3/4 points";
    public static final String TEMPLATE_ERROR_4_4_POINTS_WRONG = "Template error - wrong sum for 4/4 points";
    public static final String TEMPLATE_ERROR_POINTS_WRONG = "Template error - cannot check question points";
    public static final String TEMPLATE_ERROR_TRUE_FALSE_POINTS_WRONG = "Template error - wrong sum for true false points";

    public static final String UNALLOWED_CHARS = "Save error - chars not supported";
    public static final String EMPTY_QUESTION_TEXT = "Empty question text";
    public static final String REMOVE_TEMPLATE_QUESTION = "Template question - to be removed";
    public static final String TITLE_NOT_STRING = "Title not string";
    public static final String MISSING_VALUES_LESS_THAN_6 = "Missing values - the row with true false question contains less than 6 values";
    public static final String MISSING_VALUES_LESS_THAN_11 = "Missing values - less than 11";
    public static final String INCOMPLETE_ASSIGNMENT_LESS_THAN_15_QUESTIONS = "Incomplete assignment - less than 15 questions";
    public static final String ERROR_WRONG_FILE_TYPE = "Wrong file type";

    public static final String USER_NAME_NOT_DETECTED = "Max Mustermann";

    public static final int PAGE_SIZE = 20;
    public static Pageable getPageable(int pageNo, int pageSize, String sortField, String sortDirection) {
        return PageRequest.of(pageNo - 1, pageSize);
    }
}
