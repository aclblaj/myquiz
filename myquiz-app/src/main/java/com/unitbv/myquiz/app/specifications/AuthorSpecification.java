package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.Author;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering Author entities.
 * Provides reusable predicates for filtering authors with optional eager fetching.
 */
public final class AuthorSpecification {

    private static final String QUESTION_BANK_AUTHORS = "questionBankAuthors";
    private static final String NAME = "name";
    private static final String INITIALS = "initials";
    private static final String ID = "id";
    private static final String QUESTION_BANK = "questionBank";
    private static final String COURSE_ENTITY = "course";
    private static final String COURSE = "course";

    // Private constructor to prevent instantiation
    private AuthorSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by author ID.
     *
     * @param id the author ID
     * @return Specification for filtering by ID
     */
    public static Specification<Author> hasId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return cb.conjunction();
            return cb.equal(root.get(ID), id);
        };
    }

    /**
     * Filter by author name (case-insensitive, contains).
     *
     * @param name the author name
     * @return Specification for filtering by name
     */
    public static Specification<Author> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(NAME)), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact author name (case-insensitive).
     *
     * @param name the author name
     * @return Specification for filtering by exact name
     */
    public static Specification<Author> hasNameExact(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return cb.conjunction();
            return cb.equal(cb.lower(root.get(NAME)), name.toLowerCase());
        };
    }

    /**
     * Filter by initials (case-insensitive, contains).
     *
     * @param initials the author initials
     * @return Specification for filtering by initials
     */
    public static Specification<Author> hasInitials(String initials) {
        return (root, query, cb) -> {
            if (initials == null || initials.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(INITIALS)), "%" + initials.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact initials (case-insensitive).
     *
     * @param initials the author initials
     * @return Specification for filtering by exact initials
     */
    public static Specification<Author> hasInitialsExact(String initials) {
        return (root, query, cb) -> {
            if (initials == null || initials.isEmpty()) return cb.conjunction();
            return cb.equal(cb.lower(root.get(INITIALS)), initials.toLowerCase());
        };
    }

    /**
     * Filter authors by course through their questionBankAuthors relationship.
     * This finds authors who have contributed to question banks in the specified course.
     *
     * @param course the course name
     * @return Specification for filtering authors by course
     */
    public static Specification<Author> hasCourse(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTION_BANK_AUTHORS, JoinType.LEFT);
            var questionBankJoin = questionBankAuthorJoin.join(QUESTION_BANK, JoinType.LEFT);
            var courseJoin = questionBankJoin.join(COURSE_ENTITY, JoinType.LEFT);
            query.distinct(true);
            return cb.equal(cb.lower(courseJoin.get(COURSE)), course.toLowerCase());
        };
    }

    /**
     * Filter authors by question bank ID through their questionBankAuthors relationship.
     *
     * @param questionBankId the question bank ID
     * @return Specification for filtering authors by question bank
     */
    public static Specification<Author> hasQuestionBankId(Long questionBankId) {
        return (root, query, cb) -> {
            if (questionBankId == null) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTION_BANK_AUTHORS, JoinType.LEFT);
            var questionBankJoin = questionBankAuthorJoin.join(QUESTION_BANK, JoinType.LEFT);
            query.distinct(true);
            return cb.equal(questionBankJoin.get(ID), questionBankId);
        };
    }

    /**
     * Eagerly fetch the questionBankAuthors collection to avoid N+1 queries.
     *
     * @return Specification that fetches question bank authors
     */
    public static Specification<Author> fetchQuestionBankAuthors() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(Author.class)) {
                root.fetch(QUESTION_BANK_AUTHORS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param name the author name (optional)
     * @param initials the author initials (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<Author> byFilters(String name, String initials) {
        return Specification
                .where(hasName(name))
                .and(hasInitials(initials));
    }

    /**
     * Find author by exact name.
     *
     * @param name the author name
     * @return Specification for finding by exact name
     */
    public static Specification<Author> byName(String name) {
        return hasNameExact(name);
    }

    /**
     * Find author by exact initials.
     *
     * @param initials the author initials
     * @return Specification for finding by exact initials
     */
    public static Specification<Author> byInitials(String initials) {
        return hasInitialsExact(initials);
    }

    /**
     * Find authors who have contributed to a specific course.
     *
     * @param course the course name
     * @return Specification for finding authors by course
     */
    public static Specification<Author> byCourse(String course) {
        return hasCourse(course);
    }

    /**
     * Find authors who have contributed to a specific question bank.
     *
     * @param questionBankId the question bank ID
     * @return Specification for finding authors by question bank
     */
    public static Specification<Author> byQuestionBank(Long questionBankId) {
        return hasQuestionBankId(questionBankId);
    }
}
