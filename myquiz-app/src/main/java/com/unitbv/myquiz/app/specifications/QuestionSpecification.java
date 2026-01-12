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
 * This class encapsulates the filtering logic for questions based on course, author, quiz, and question type.
 */
public class QuestionSpecification implements Specification<Question> {

    private static final String QUIZ_AUTHOR = "quizAuthor";
    private static final String AUTHOR = "author";
    private static final String QUIZ = "quiz";

    private final String course;
    private final Long authorId;
    private final Long quizId;
    private final QuestionType questionType;

    public QuestionSpecification(String course, Long authorId, Long quizId, QuestionType questionType) {
        this.course = course;
        this.authorId = authorId;
        this.quizId = quizId;
        this.questionType = questionType;
    }

    @Override
    public Predicate toPredicate(Root<Question> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        // Eagerly fetch related entities to avoid N+1 queries
        if (query.getResultType().equals(Question.class)) {
            root.fetch(QUIZ_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT)
                .fetch(AUTHOR, jakarta.persistence.criteria.JoinType.LEFT);
            root.fetch(QUIZ_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT)
                .fetch(QUIZ, jakarta.persistence.criteria.JoinType.LEFT);
            query.distinct(true);
        }

        List<Predicate> predicates = new ArrayList<>();
        if (course != null && !course.isEmpty()) {
            var quizAuthorJoin = root.join(QUIZ_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT);
            var quizJoin = quizAuthorJoin.join(QUIZ, jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(cb.lower(quizJoin.get("course")), course.toLowerCase()));
        }
        if (authorId != null) {
            var quizAuthorJoin = root.join(QUIZ_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(quizAuthorJoin.get(AUTHOR).get("id"), authorId));
        }
        if (quizId != null) {
            var quizAuthorJoin = root.join(QUIZ_AUTHOR, jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(quizAuthorJoin.get(QUIZ).get("id"), quizId));
        }
        if (questionType != null) {
            predicates.add(cb.equal(root.get("type"), questionType));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    public static QuestionSpecification byFilters(String course, Long authorId, Long quizId, QuestionType questionType) {
        return new QuestionSpecification(course, authorId, quizId, questionType);
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
     * Filter questions by QuizAuthor ID.
     * This is a clear, reusable method for filtering questions belonging to a specific QuizAuthor.
     *
     * @param quizAuthorId the QuizAuthor ID
     * @return Specification for filtering by QuizAuthor ID
     */
    public static Specification<Question> byQuizAuthorId(Long quizAuthorId) {
        return (root, query, cb) -> {
            if (quizAuthorId == null) return cb.conjunction();
            return cb.equal(root.get(QUIZ_AUTHOR).get("id"), quizAuthorId);
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
            var quizAuthorJoin = root.join(QUIZ_AUTHOR);
            return cb.equal(quizAuthorJoin.get(AUTHOR).get("id"), authorId);
        };
    }

    /**
     * Filter questions by quiz ID.
     *
     * @param quizId the quiz ID
     * @return Specification for filtering by quiz ID
     */
    public static Specification<Question> byQuizId(Long quizId) {
        return (root, query, cb) -> {
            if (quizId == null) return cb.conjunction();
            var quizAuthorJoin = root.join(QUIZ_AUTHOR);
            return cb.equal(quizAuthorJoin.get(QUIZ).get("id"), quizId);
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
            var quizAuthorJoin = root.join(QUIZ_AUTHOR);
            var quizJoin = quizAuthorJoin.join(QUIZ);
            return cb.equal(cb.lower(quizJoin.get("course")), course.toLowerCase());
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

    // Add a static method for filtering by author name (case-insensitive contains)
    public static Specification<Question> hasAuthorName(String authorName) {
        return (root, query, cb) -> {
            if (authorName == null || authorName.isEmpty()) return cb.conjunction();
            var quizAuthorJoin = root.join(QUIZ_AUTHOR);
            return cb.like(cb.lower(quizAuthorJoin.get(AUTHOR).get("name")), "%" + authorName.toLowerCase() + "%");
        };
    }
}
