package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.Quiz;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering Quiz entities.
 * Provides reusable predicates for filtering quizzes with optional eager fetching.
 *
 * @author MyQuiz Team
 * @since December 28, 2025
 */
public class QuizSpecification {

    private static final String QUIZ_AUTHORS = "quizAuthors";
    private static final String COURSE = "course";
    private static final String NAME = "name";
    private static final String YEAR = "year";

    // Private constructor to prevent instantiation
    private QuizSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by quiz ID.
     *
     * @param id the quiz ID
     * @return Specification for filtering by ID
     */
    public static Specification<Quiz> hasId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return cb.conjunction();
            return cb.equal(root.get("id"), id);
        };
    }

    /**
     * Filter by quiz name (case-insensitive, contains).
     *
     * @param name the quiz name
     * @return Specification for filtering by name
     */
    public static Specification<Quiz> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(NAME)), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact quiz name (case-insensitive).
     *
     * @param name the quiz name
     * @return Specification for filtering by exact name
     */
    public static Specification<Quiz> hasNameExact(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return cb.conjunction();
            return cb.equal(cb.lower(root.get(NAME)), name.toLowerCase());
        };
    }

    /**
     * Filter by course name (case-insensitive, contains).
     *
     * @param course the course name
     * @return Specification for filtering by course
     */
    public static Specification<Quiz> hasCourse(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(COURSE)), "%" + course.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact course name (case-insensitive).
     *
     * @param course the course name
     * @return Specification for filtering by exact course
     */
    public static Specification<Quiz> hasCourseExact(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            return cb.equal(cb.lower(root.get(COURSE)), course.toLowerCase());
        };
    }

    /**
     * Filter by year.
     *
     * @param year the year
     * @return Specification for filtering by year
     */
    public static Specification<Quiz> hasYear(Long year) {
        return (root, query, cb) -> {
            if (year == null) return cb.conjunction();
            return cb.equal(root.get(YEAR), year);
        };
    }

    /**
     * Filter by QuizAuthor ID.
     * Finds quizzes that have a specific QuizAuthor.
     *
     * @param quizAuthorId the QuizAuthor ID
     * @return Specification for filtering by QuizAuthor ID
     */
    public static Specification<Quiz> hasQuizAuthorId(Long quizAuthorId) {
        return (root, query, cb) -> {
            if (quizAuthorId == null) return cb.conjunction();
            var quizAuthorJoin = root.join(QUIZ_AUTHORS, JoinType.LEFT);
            return cb.equal(quizAuthorJoin.get("id"), quizAuthorId);
        };
    }

    /**
     * Eagerly fetch the quizAuthors collection to avoid N+1 queries.
     * Use this when you need to access quiz authors.
     *
     * @return Specification that fetches quiz authors
     */
    public static Specification<Quiz> fetchQuizAuthors() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(Quiz.class)) {
                root.fetch(QUIZ_AUTHORS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param name the quiz name (optional)
     * @param course the course name (optional)
     * @param year the year (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<Quiz> byFilters(String name, String course, Long year) {
        return Specification
                .where(hasName(name))
                .and(hasCourse(course))
                .and(hasYear(year));
    }

    /**
     * Find quiz by name, course, and year.
     * Exact match for all three parameters.
     *
     * @param name the quiz name
     * @param course the course name
     * @param year the year
     * @return Specification for finding by exact name, course, and year
     */
    public static Specification<Quiz> byNameAndCourseAndYear(String name, String course, Long year) {
        return Specification
                .where(hasNameExact(name))
                .and(hasCourseExact(course))
                .and(hasYear(year));
    }

    /**
     * Get all quizzes for a specific course (exact match).
     *
     * @param course the course name
     * @return Specification for finding by exact course
     */
    public static Specification<Quiz> byCourse(String course) {
        return hasCourseExact(course);
    }

    /**
     * Find quiz by QuizAuthor ID.
     * Returns quizzes that have a specific QuizAuthor.
     *
     * @param quizAuthorId the QuizAuthor ID
     * @return Specification for finding by QuizAuthor ID
     */
    public static Specification<Quiz> byQuizAuthorId(Long quizAuthorId) {
        return hasQuizAuthorId(quizAuthorId);
    }
}

