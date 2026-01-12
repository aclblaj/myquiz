package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering QuizAuthor entities.
 * Provides reusable predicates for filtering quiz-author relationships with optional eager fetching.
 *
 * @author MyQuiz Team
 * @since December 26, 2025
 */
public class QuizAuthorSpecification {

    private static final String QUIZ = "quiz";
    private static final String AUTHOR = "author";
    private static final String QUESTIONS = "questions";
    private static final String QUIZ_ERRORS = "quizErrors";

    // Private constructor to prevent instantiation
    private QuizAuthorSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by quiz ID.
     *
     * @param quizId the quiz ID
     * @return Specification for filtering by quiz ID
     */
    public static Specification<QuizAuthor> hasQuizId(Long quizId) {
        return (root, query, cb) -> {
            if (quizId == null) return cb.conjunction();
            return cb.equal(root.get(QUIZ).get("id"), quizId);
        };
    }

    /**
     * Filter by author ID.
     *
     * @param authorId the author ID
     * @return Specification for filtering by author ID
     */
    public static Specification<QuizAuthor> hasAuthorId(Long authorId) {
        return (root, query, cb) -> {
            if (authorId == null) return cb.conjunction();
            return cb.equal(root.get(AUTHOR).get("id"), authorId);
        };
    }

    /**
     * Filter by author entity.
     * Convenience method that accepts an Author entity instead of just the ID.
     *
     * @param author the author entity
     * @return Specification for filtering by author
     */
    public static Specification<QuizAuthor> hasAuthor(Author author) {
        return (root, query, cb) -> {
            if (author == null) return cb.conjunction();
            return cb.equal(root.get(AUTHOR), author);
        };
    }

    /**
     * Filter by course name (case-insensitive, contains).
     *
     * @param course the course name
     * @return Specification for filtering by course
     */
    public static Specification<QuizAuthor> hasCourse(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(QUIZ).get("course")), "%" + course.toLowerCase() + "%");
        };
    }

    /**
     * Filter by author name (case-insensitive, contains).
     *
     * @param authorName the author name
     * @return Specification for filtering by author name
     */
    public static Specification<QuizAuthor> hasAuthorName(String authorName) {
        return (root, query, cb) -> {
            if (authorName == null || authorName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(AUTHOR).get("name")), "%" + authorName.toLowerCase() + "%");
        };
    }

    /**
     * Eagerly fetch the author association to avoid N+1 queries.
     * Use this when you need author details.
     *
     * @return Specification that fetches author
     */
    public static Specification<QuizAuthor> fetchAuthor() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizAuthor.class)) {
                root.fetch(AUTHOR, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Eagerly fetch the quiz association to avoid N+1 queries.
     * Use this when you need quiz details.
     *
     * @return Specification that fetches quiz
     */
    public static Specification<QuizAuthor> fetchQuiz() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizAuthor.class)) {
                root.fetch(QUIZ, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Eagerly fetch the questions collection to avoid N+1 queries.
     * Use this when you need to access questions.
     *
     * @return Specification that fetches questions
     */
    public static Specification<QuizAuthor> fetchQuestions() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizAuthor.class)) {
                root.fetch(QUESTIONS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Eagerly fetch the quiz errors collection to avoid N+1 queries.
     * Use this when you need to access errors.
     *
     * @return Specification that fetches quiz errors
     */
    public static Specification<QuizAuthor> fetchQuizErrors() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizAuthor.class)) {
                root.fetch(QUIZ_ERRORS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Eagerly fetch both questions and quiz errors collections.
     * WARNING: This can cause Cartesian product (N×M results). Use with caution!
     * Consider using separate queries or @EntityGraph instead.
     *
     * @return Specification that fetches both questions and errors
     */
    public static Specification<QuizAuthor> fetchQuestionsAndErrors() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuizAuthor.class)) {
                root.fetch(QUESTIONS, JoinType.LEFT);
                root.fetch(QUIZ_ERRORS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param quizId the quiz ID (optional)
     * @param authorId the author ID (optional)
     * @param course the course name (optional)
     * @param authorName the author name (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<QuizAuthor> byFilters(Long quizId, Long authorId, String course, String authorName) {
        return Specification
                .where(hasQuizId(quizId))
                .and(hasAuthorId(authorId))
                .and(hasCourse(course))
                .and(hasAuthorName(authorName));
    }

    /**
     * Find QuizAuthor by quiz ID and author ID.
     * This replaces the named query method findByQuiz_IdAndAuthor_Id.
     *
     * @param quizId the quiz ID
     * @param authorId the author ID
     * @return Specification for finding by quiz and author IDs
     */
    public static Specification<QuizAuthor> byQuizAndAuthor(Long quizId, Long authorId) {
        return Specification
                .where(hasQuizId(quizId))
                .and(hasAuthorId(authorId));
    }
}

