package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuizAuthor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface QuizAuthorRepository extends PagingAndSortingRepository<QuizAuthor, Long>, CrudRepository<QuizAuthor, Long> {
    List<QuizAuthor> findByAuthorId(Long authorId);

    Long countByAuthor(Author author);

    List<QuizAuthor> findQuizAuthorByQuizId(Long quizId);

    List<QuizAuthor> findByAuthor_NameContainsIgnoreCase(String authorName);

    List<QuizAuthor> findAllByQuiz_CourseContainsIgnoreCase(String course);

    void deleteAllByQuizId(Long id);

    @Query("SELECT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.questions WHERE qa.quiz.course LIKE %:course%")
    List<QuizAuthor> findAllWithQuestionsByQuizCourseContainsIgnoreCase(@Param("course") String course);

    @Query("SELECT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.questions WHERE qa.author.name LIKE %:authorName%")
    List<QuizAuthor> findAllWithQuestionsByAuthorNameContainsIgnoreCase(@Param("authorName") String authorName);

    @Query("SELECT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.questions WHERE qa.author.id = :authorId")
    List<QuizAuthor> findWithQuestionsByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.quizErrors WHERE qa.author.id = :authorId")
    List<QuizAuthor> findWithQuizErrorsByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT DISTINCT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.questions LEFT JOIN FETCH qa.quizErrors WHERE qa.quiz.course LIKE %:course%")
    List<QuizAuthor> findAllWithQuestionsAndQuizErrorsByQuizCourseContainsIgnoreCase(@Param("course") String course);

    @Query("SELECT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.quizErrors WHERE qa.quiz.id = :quizId")
    List<QuizAuthor> findWithQuizErrorsByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.questions WHERE qa.quiz.id = :quizId")
    List<QuizAuthor> findWithQuestionsByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT DISTINCT qa FROM QuizAuthor qa LEFT JOIN FETCH qa.questions LEFT JOIN FETCH qa.quizErrors WHERE qa.quiz.id = :quizId")
    List<QuizAuthor> findWithQuestionsAndQuizErrorsByQuizId(@Param("quizId") Long quizId);
}
