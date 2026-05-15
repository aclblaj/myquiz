package com.unitbv.myquiz.app.specifications;

import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.api.types.QuestionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for filtering Question entities.
 * This class encapsulates the filtering logic for questions based on course, author, question bank, and question type.
 */
public final class QuestionSpecification implements Specification<Question> {

    private static final String QUESTION_BANK_AUTHOR = "questionBankAuthor";
    private static final String AUTHOR = "author";
    private static final String QUESTION_BANK = "questionBank";
    private static final String COURSE_ENTITY = "course";
    private static final String COURSE = "course";

    private final String course;
    private final Long authorId;
    private final Long questionBankId;
    private final QuestionType questionType;

    public QuestionSpecification(String course, Long authorId, Long questionBankId, QuestionType questionType) {
        this.course = course;
        this.authorId = authorId;
        this.questionBankId = questionBankId;
        this.questionType = questionType;
    }

    @Override
    public Predicate toPredicate(Root<Question> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        // Eagerly fetch related entities to avoid N+1 queries
        if (query.getResultType().equals(Question.class)) {
            var questionBankAuthorFetch = root.fetch(QUESTION_BANK_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT);
            questionBankAuthorFetch.fetch(AUTHOR, jakarta.persistence.criteria.JoinType.LEFT);
            questionBankAuthorFetch.fetch(QUESTION_BANK, jakarta.persistence.criteria.JoinType.LEFT);
            query.distinct(true);
        }

        List<Predicate> predicates = new ArrayList<>();

        // Create a single questionBankAuthorJoin if any of the filters need it
        var questionBankAuthorJoin = (course != null && !course.isEmpty()) || authorId != null || questionBankId != null
            ? root.join(QUESTION_BANK_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT)
            : null;

        if (course != null && !course.isEmpty()) {
            var questionBankJoin = questionBankAuthorJoin.join(QUESTION_BANK, jakarta.persistence.criteria.JoinType.LEFT);
            var courseJoin = questionBankJoin.join(COURSE_ENTITY, jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(cb.lower(courseJoin.get(COURSE)), course.toLowerCase()));
        }
        if (authorId != null) {
            predicates.add(cb.equal(questionBankAuthorJoin.get(AUTHOR).get("id"), authorId));
        }
        if (questionBankId != null) {
            predicates.add(cb.equal(questionBankAuthorJoin.get(QUESTION_BANK).get("id"), questionBankId));
        }
        if (questionType != null) {
            predicates.add(cb.equal(root.get("type"), questionType));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    public static QuestionSpecification byFilters(String course, Long authorId, Long questionBankId, QuestionType questionType) {
        return new QuestionSpecification(course, authorId, questionBankId, questionType);
    }

    /**
     * Filter questions by question ID.
     *
     * @param id the question ID
     * @return Specification for filtering by ID
     */
    public static Specification<Question> byId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return cb.conjunction();
            return cb.equal(root.get("id"), id);
        };
    }

    /**
     * Filter questions by QuestionBankAuthor ID.
     * This is a clear, reusable method for filtering questions belonging to a specific QuestionBankAuthor.
     *
     * @param questionBankAuthorId the QuestionBankAuthor ID
     * @return Specification for filtering by QuestionBankAuthor ID
     */
    public static Specification<Question> byQuestionBankAuthorId(Long questionBankAuthorId) {
        return (root, query, cb) -> {
            if (questionBankAuthorId == null) return cb.conjunction();
            return cb.equal(root.get(QUESTION_BANK_AUTHOR).get("id"), questionBankAuthorId);
        };
    }

    /**
     * Filter questions by author ID.
     *
     * @param authorId the author ID
     * @return Specification for filtering by author ID
     */
    public static Specification<Question> byAuthorId(Long authorId) {
        return (root, query, cb) -> {
            if (authorId == null) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTION_BANK_AUTHOR);
            return cb.equal(questionBankAuthorJoin.get(AUTHOR).get("id"), authorId);
        };
    }

    /**
     * Filter questions by question bank ID.
     *
     * @param questionBankId the question bank ID
     * @return Specification for filtering by question bank ID
     */
    public static Specification<Question> byQuestionBankId(Long questionBankId) {
        return (root, query, cb) -> {
            if (questionBankId == null) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTION_BANK_AUTHOR);
            return cb.equal(questionBankAuthorJoin.get(QUESTION_BANK).get("id"), questionBankId);
        };
    }

    /**
     * Filter questions by course name (case-insensitive).
     *
     * @param course the course name
     * @return Specification for filtering by course
     */
    public static Specification<Question> byCourse(String course) {
        return (root, query, cb) -> {
            if (course == null || course.isEmpty()) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTION_BANK_AUTHOR);
            var questionBankJoin = questionBankAuthorJoin.join(QUESTION_BANK);
            var courseJoin = questionBankJoin.join(COURSE_ENTITY);
            return cb.equal(cb.lower(courseJoin.get(COURSE)), course.toLowerCase());
        };
    }

    /**
     * Filter questions by question type.
     *
     * @param questionType the question type
     * @return Specification for filtering by question type
     */
    public static Specification<Question> byQuestionType(QuestionType questionType) {
        return (root, query, cb) -> {
            if (questionType == null) return cb.conjunction();
            return cb.equal(root.get("type"), questionType);
        };
    }

    /**
     * Filter questions by author name (case-insensitive contains).
     *
     * @param authorName the author name to search for
     * @return Specification for filtering by author name
     */
    public static Specification<Question> hasAuthorName(String authorName) {
        return (root, query, cb) -> {
            if (authorName == null || authorName.isEmpty()) return cb.conjunction();
            var questionBankAuthorJoin = root.join(QUESTION_BANK_AUTHOR);
            return cb.like(cb.lower(questionBankAuthorJoin.get(AUTHOR).get("name")), "%" + authorName.toLowerCase() + "%");
        };
    }
}
