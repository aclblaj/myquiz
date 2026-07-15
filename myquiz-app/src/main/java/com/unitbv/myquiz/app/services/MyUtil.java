package com.unitbv.myquiz.app.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Utility class providing common constants and helper methods for the MyQuiz application.
 * Contains validation error messages, forbidden question titles, and pagination utilities.
 * This class cannot be instantiated.
 */
public final class MyUtil {

    /**
     * List of forbidden question titles that should be filtered or rejected.
     * These titles are typically template questions or invalid entries.
     */
    public static final List<String> FORBIDDEN_TITLES = List.of("Kapazität Null", "Kommunikationsmuster", "ICMP", "Roluri 2PC", "IP header", "c02_scheduler_functie");
    // error messages
    public static final String MISSING_POINTS = "Missing points - empty value for points";
    public static final String MISSING_TITLE = "Missing title";
    public static final String MISSING_ANSWER = "Missing answer";
    public static final String NOT_NUMERIC_COLUMN = "Conversion error - column contains no numeric values";
    public static final String DATATYPE_ERROR = "Datatype error";
    public static final String REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS = "Answer already exists";
    public static final String REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS = "Title already exists";
    public static final String SKIPPED_DUE_TO_ERROR = "Skipped due to error";
    public static final String BLACKLISTED_WORDS = "Blacklisted words - question contains forbidden words";
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

    public static boolean isDuplicateValidationError(String description) {
        if (description == null || description.isBlank()) {
            return false;
        }
        return description.startsWith(REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS)
                || description.startsWith(REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS);
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MyUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates a Pageable object with sorting support.
     *
     * @param pageNo        the page number (1-based indexing). Use -1 for unpaged results.
     * @param pageSize      the number of items per page. Must be positive.
     * @param sortField     the field name to sort by. If null or empty, no sorting is applied.
     * @param sortDirection the sort direction ("asc" or "desc"). If null or empty, defaults to "asc".
     * @return Pageable object configured with the specified parameters
     * @throws IllegalArgumentException if pageSize is less than 1 (when pageNo is not -1)
     */
    public static Pageable getPageable(int pageNo, int pageSize, String sortField, String sortDirection) {
        // Handle unpaged results
        if (pageNo == -1) {
            return Pageable.unpaged();
        }

        // Validate pageSize
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be at least 1");
        }

        // Create sort if sortField is provided
        if (sortField != null && !sortField.trim().isEmpty()) {
            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }
            Sort sort = Sort.by(direction, sortField);
            return PageRequest.of(pageNo - 1, pageSize, sort);
        }

        // Return without sorting if no sortField provided
        return PageRequest.of(pageNo - 1, pageSize);
    }
}
