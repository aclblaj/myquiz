package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering QuestionBankAuthor entities.
 * Provides reusable predicates for filtering question-bank-author relationships with optional eager fetching.
 */
public final class QuestionBankAuthorSpecification {

    private static final String QUESTIONBANK = "questionBank";
    private static final String AUTHOR = "author";
    private static final String QUESTIONS = "questions";
    private static final String ID = "id";
    private static final String COURSE_ENTITY = "course";
    private static final String COURSE = "course";
    private static final String NAME = "name";

    // Private constructor to prevent instantiation
    private QuestionBankAuthorSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by question bank ID.
     *
     * @param questionBankId the question bank ID
     * @return Specification for filtering by question bank ID
     */
    public static Specification<QuestionBankAuthor> hasQuestionBankId(Long questionBankId) {
        return (root, query, cb) -> {
            if (questionBankId == null) return cb.conjunction();
            return cb.equal(root.get(QUESTIONBANK).get(ID), questionBankId);
        };
    }

    /**
     * Filter by author ID.
     *
     * @param authorId the author ID
     * @return Specification for filtering by author ID
     */
    public static Specification<QuestionBankAuthor> hasAuthorId(Long authorId) {
        return (root, query, cb) -> {
            if (authorId == null) return cb.conjunction();
            return cb.equal(root.get(AUTHOR).get(ID), authorId);
        };
    }

    /**
     * Filter by author entity.
     * Convenience method that accepts an Author entity instead of just the ID.
     *
     * @param author the author entity
     * @return Specification for filtering by author
     */
    public static Specification<QuestionBankAuthor> hasAuthor(Author author) {
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
    public static Specification<QuestionBankAuthor> hasCourse(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(QUESTIONBANK).get(COURSE_ENTITY).get(COURSE)), "%" + course.toLowerCase() + "%");
        };
    }

    /**
     * Filter by author name (case-insensitive, contains).
     *
     * @param authorName the author name
     * @return Specification for filtering by author name
     */
    public static Specification<QuestionBankAuthor> hasAuthorName(String authorName) {
        return (root, query, cb) -> {
            if (authorName == null || authorName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(AUTHOR).get(NAME)), "%" + authorName.toLowerCase() + "%");
        };
    }

    /**
     * Eagerly fetch the author association to avoid N+1 queries.
     * Use this when you need author details.
     *
     * @return Specification that fetches author
     */
    public static Specification<QuestionBankAuthor> fetchAuthor() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuestionBankAuthor.class)) {
                root.fetch(AUTHOR, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Eagerly fetch the question bank association to avoid N+1 queries.
     * Use this when you need question bank details.
     *
     * @return Specification that fetches question bank
     */
    public static Specification<QuestionBankAuthor> fetchQuestionBank() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuestionBankAuthor.class)) {
                root.fetch(QUESTIONBANK, JoinType.LEFT);
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
    public static Specification<QuestionBankAuthor> fetchQuestions() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuestionBankAuthor.class)) {
                root.fetch(QUESTIONS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param questionBankId the question bank ID (optional)
     * @param authorId the author ID (optional)
     * @param course the course name (optional)
     * @param authorName the author name (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<QuestionBankAuthor> byFilters(Long questionBankId, Long authorId, String course, String authorName) {
        return Specification
                .where(hasQuestionBankId(questionBankId))
                .and(hasAuthorId(authorId))
                .and(hasCourse(course))
                .and(hasAuthorName(authorName));
    }

    /**
     * Find QuestionBankAuthor by question bank ID and author ID.
     * This replaces the named query method findByQuestionBank_IdAndAuthor_Id.
     *
     * @param questionBankId the question bank ID
     * @param authorId the author ID
     * @return Specification for finding by question bank and author IDs
     */
    public static Specification<QuestionBankAuthor> byQuestionBankAndAuthor(Long questionBankId, Long authorId) {
        return Specification
                .where(hasQuestionBankId(questionBankId))
                .and(hasAuthorId(authorId));
    }
}

