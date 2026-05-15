package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.api.types.StudyYear;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for filtering QuestionBank entities.
 * Provides reusable predicates for filtering question banks with optional eager fetching.
 */
public final class QuestionBankSpecification {

    private static final String QUESTIONBANK_AUTHORS = "questionBankAuthors";
    private static final String COURSE_ENTITY = "course";
    private static final String COURSE = "course";
    private static final String NAME = "name";
    private static final String STUDY_YEAR = "studyYear";
    private static final String ID = "id";

    // Private constructor to prevent instantiation
    private QuestionBankSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Filter by question bank ID.
     *
     * @param id the question bank ID
     * @return Specification for filtering by ID
     */
    public static Specification<QuestionBank> hasId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return cb.conjunction();
            return cb.equal(root.get(ID), id);
        };
    }

    /**
     * Filter by question bank name (case-insensitive, contains).
     *
     * @param name the question bank name
     * @return Specification for filtering by name
     */
    public static Specification<QuestionBank> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get(NAME)), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact question bank name (case-insensitive).
     *
     * @param name the question bank name
     * @return Specification for filtering by exact name
     */
    public static Specification<QuestionBank> hasNameExact(String name) {
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
    public static Specification<QuestionBank> hasCourse(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            var courseJoin = root.join(COURSE_ENTITY, JoinType.LEFT);
            return cb.like(cb.lower(courseJoin.get(COURSE)), "%" + course.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact course name (case-insensitive).
     *
     * @param course the course name
     * @return Specification for filtering by exact course
     */
    public static Specification<QuestionBank> hasCourseExact(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            var courseJoin = root.join(COURSE_ENTITY, JoinType.LEFT);
            return cb.equal(cb.lower(courseJoin.get(COURSE)), course.toLowerCase());
        };
    }

    /**
     * Filter by study year.
     *
     * @param studyYear the study year
     * @return Specification for filtering by study year
     */
    public static Specification<QuestionBank> hasStudyYear(StudyYear studyYear) {
        return (root, query, cb) -> {
            if (studyYear == null) return cb.conjunction();
            return cb.equal(root.get(STUDY_YEAR), studyYear);
        };
    }

    /**
     * Filter by QuestionBankAuthor ID.
     * Finds question banks that have a specific QuestionBankAuthor.
     *
     * @param questionBankAuthorId the QuestionBankAuthor ID
     * @return Specification for filtering by QuestionBankAuthor ID
     */
    public static Specification<QuestionBank> hasQuestionBankAuthorId(Long questionBankAuthorId) {
        return (root, query, cb) -> {
            if (questionBankAuthorId == null) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTIONBANK_AUTHORS, JoinType.LEFT);
            return cb.equal(questionBankAuthorJoin.get(ID), questionBankAuthorId);
        };
    }

    /**
     * Eagerly fetch the questionBankAuthors collection to avoid N+1 queries.
     * Use this when you need to access question bank authors.
     *
     * @return Specification that fetches question bank authors
     */
    public static Specification<QuestionBank> fetchQuestionBankAuthors() {
        return (root, query, cb) -> {
            if (query.getResultType().equals(QuestionBank.class)) {
                root.fetch(QUESTIONBANK_AUTHORS, JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    /**
     * Combined filter by multiple criteria.
     * This is a convenience method for common filtering scenarios.
     *
     * @param name the question bank name (optional)
     * @param course the course name (optional)
     * @param studyYear the study year (optional)
     * @return Specification combining all non-null filters
     */
    public static Specification<QuestionBank> byFilters(String name, String course, StudyYear studyYear) {
        return Specification
                .where(hasName(name))
                .and(hasCourse(course))
                .and(hasStudyYear(studyYear));
    }

    /**
     * Find question bank by name, course, and study year.
     * Exact match for all three parameters.
     *
     * @param name the question bank name
     * @param course the course name
     * @param studyYear the study year
     * @return Specification for finding by exact name, course, and year
     */
    public static Specification<QuestionBank> byNameAndCourseAndStudyYear(String name, String course, StudyYear studyYear) {
        return Specification
                .where(hasNameExact(name))
                .and(hasCourseExact(course))
                .and(hasStudyYear(studyYear));
    }

    /**
     * Get all question banks for a specific course (exact match).
     *
     * @param course the course name
     * @return Specification for finding by exact course
     */
    public static Specification<QuestionBank> byCourse(String course) {
        return hasCourseExact(course);
    }

    /**
     * Find question bank by QuestionBankAuthor ID.
     * Returns question banks that have a specific QuestionBankAuthor.
     *
     * @param questionBankAuthorId the QuestionBankAuthor ID
     * @return Specification for finding by QuestionBankAuthor ID
     */
    public static Specification<QuestionBank> byQuestionBankAuthorId(Long questionBankAuthorId) {
        return hasQuestionBankAuthorId(questionBankAuthorId);
    }

    /**
     * Filter by course entity ID.
     * Used when deleting a course to find all question banks that reference it.
     *
     * @param courseId the course ID (foreign key)
     * @return Specification for filtering by course ID
     */
    public static Specification<QuestionBank> byCourseId(Long courseId) {
        return (root, query, cb) -> {
            if (courseId == null) return cb.conjunction();
            var courseJoin = root.join(COURSE_ENTITY, JoinType.LEFT);
            return cb.equal(courseJoin.get(ID), courseId);
        };
    }
}

