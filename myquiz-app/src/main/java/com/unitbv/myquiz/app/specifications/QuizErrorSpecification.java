package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.QuizError;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering QuizError entities.
 * Provides reusable predicates for filtering quiz errors with optional eager fetching.
 *
 * @author MyQuiz Team
 * @since December 28, 2025
 */
public class QuizErrorSpecification {

    private static final String QUIZ_AUTHOR = "quizAuthor";
    private static final String DESCRIPTION = "description";
    private static final String ROW_NUMBER = "rowNumber";

    // Private constructor to prevent instantiation
    private QuizErrorSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by error ID.
     *
     * @param id the error ID
     * @return Specification for filtering by ID
     */
    public static Specification<QuizError> hasId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return cb.conjunction();
            return cb.equal(root.get("id"), id);
        };
    }

    /**
     * Filter by QuizAuthor ID.
     *
     * @param quizAuthorId the QuizAuthor ID
     * @return Specification for filtering by QuizAuthor ID
     */
    public static Specification<QuizError> hasQuizAuthorId(Long quizAuthorId) {
        return (root, query, cb) -> {
            if (quizAuthorId == null) return cb.conjunction();
            return cb.equal(root.get(QUIZ_AUTHOR).get("id"), quizAuthorId);
        };
    }

    /**
     * Filter by description (case-insensitive, contains).
     *
     * @param description the error description
     * @return Specification for filtering by description
     */
    public static Specification<QuizError> hasDescription(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(DESCRIPTION)), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Filter by row number.
     *
     * @param rowNumber the row number
     * @return Specification for filtering by row number
     */
    public static Specification<QuizError> hasRowNumber(Integer rowNumber) {
        return (root, query, cb) -> {
            if (rowNumber == null) return cb.conjunction();
            return cb.equal(root.get(ROW_NUMBER), rowNumber);
        };
    }

    /**
     * Filter by quiz ID (through QuizAuthor).
     *
     * @param quizId the quiz ID
     * @return Specification for filtering by quiz ID
     */
    public static Specification<QuizError> hasQuizId(Long quizId) {
        return (root, query, cb) -> {
            if (quizId == null) return cb.conjunction();
            return cb.equal(root.get(QUIZ_AUTHOR).get("quiz").get("id"), quizId);
        };
    }

    /**
     * Filter by author ID (through QuizAuthor).
     *
     * @param authorId the author ID
     * @return Specification for filtering by author ID
     */
    public static Specification<QuizError> hasAuthorId(Long authorId) {
        return (root, query, cb) -> {
            if (authorId == null) return cb.conjunction();
            return cb.equal(root.get(QUIZ_AUTHOR).get("author").get("id"), authorId);
        };
    }

    /**
     * Filter by course pattern (through QuizAuthor and Quiz).
     * Supports LIKE patterns with wildcards (%, _).
     *
     * @param coursePattern the course pattern (e.g., "%Operating%", can include SQL wildcards)
     * @return Specification for filtering by course pattern
     */
    public static Specification<QuizError> hasCoursePattern(String coursePattern) {
        return (root, query, cb) -> {
            if (coursePattern == null || coursePattern.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(QUIZ_AUTHOR).get("quiz").get("course")), coursePattern.toLowerCase());
        };
    }

    /**
     * Filter by author name pattern (through QuizAuthor and Author).
     * Supports LIKE patterns with wildcards (%, _).
     *
     * @param authorPattern the author name pattern (e.g., "%Smith%", can include SQL wildcards)
     * @return Specification for filtering by author name pattern
     */
    public static Specification<QuizError> hasAuthorNamePattern(String authorPattern) {
        return (root, query, cb) -> {
            if (authorPattern == null || authorPattern.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(QUIZ_AUTHOR).get("author").get("name")), authorPattern.toLowerCase());
        };
    }

    /**
     * Eagerly fetch the quizAuthor association to avoid N+1 queries.
     * Use this when you need quiz author details.
     *
     * @return Specification that fetches quiz author
     */
    public static Specification<QuizError> fetchQuizAuthor() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizError.class)) {
                root.fetch(QUIZ_AUTHOR, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Eagerly fetch the quizAuthor with quiz and author.
     * Use this when you need complete details.
     *
     * @return Specification that fetches quiz author with related entities
     */
    public static Specification<QuizError> fetchQuizAuthorWithDetails() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizError.class)) {
                var quizAuthorFetch = root.fetch(QUIZ_AUTHOR, JoinType.LEFT);
                quizAuthorFetch.fetch("quiz", JoinType.LEFT);
                quizAuthorFetch.fetch("author", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param quizAuthorId the QuizAuthor ID (optional)
     * @param description the description (optional)
     * @param rowNumber the row number (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<QuizError> byFilters(Long quizAuthorId, String description, Integer rowNumber) {
        return Specification
                .where(hasQuizAuthorId(quizAuthorId))
                .and(hasDescription(description))
                .and(hasRowNumber(rowNumber));
    }

    /**
     * Find all errors for a specific QuizAuthor.
     *
     * @param quizAuthorId the QuizAuthor ID
     * @return Specification for finding by QuizAuthor
     */
    public static Specification<QuizError> byQuizAuthor(Long quizAuthorId) {
        return hasQuizAuthorId(quizAuthorId);
    }

    /**
     * Find errors by course pattern and author name pattern.
     * This is a convenience method for filtering errors by both criteria.
     *
     * @param coursePattern the course pattern (optional, can include SQL wildcards like %)
     * @param authorPattern the author name pattern (optional, can include SQL wildcards like %)
     * @return Specification combining course and author pattern filters
     */
    public static Specification<QuizError> byCourseAndAuthor(String coursePattern, String authorPattern) {
        return Specification
                .where(hasCoursePattern(coursePattern))
                .and(hasAuthorNamePattern(authorPattern));
    }

    /**
     * Find errors by author ID and quiz ID.
     * This is a convenience method for filtering errors by both criteria.
     *
     * @param authorId the author ID
     * @param quizId the quiz ID
     * @return Specification combining author ID and quiz ID filters
     */
    public static Specification<QuizError> byAuthorAndQuiz(Long authorId, Long quizId) {
        return Specification
                .where(hasAuthorId(authorId))
                .and(hasQuizId(quizId));
    }
}

