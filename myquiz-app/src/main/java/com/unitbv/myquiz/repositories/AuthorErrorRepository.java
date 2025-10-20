package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface AuthorErrorRepository extends PagingAndSortingRepository<QuizError, Long>, CrudRepository<QuizError, Long> {
    void deleteAll();
    @Query("SELECT e FROM QuizError e LEFT JOIN FETCH e.quizAuthor qa LEFT JOIN FETCH qa.author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%'))")
    List<QuizError> findAllByQuizAuthor_Author_NameContainsIgnoreCase(@Param("authorName") String authorName);

    @Query("SELECT e FROM QuizError e LEFT JOIN FETCH e.quizAuthor qa LEFT JOIN FETCH qa.author a WHERE qa.id = :quizAuthorId")
    List<QuizError> findByQuizAuthorId(@Param("quizAuthorId") Long quizAuthorId);
//    List<QuizError> findAllByOrderByQuizAuthor_Author_NameAsc();
//    List<QuizError> findAllByQuizAuthor_Quiz_CourseContainsIgnoreCase(String course);

//    List<QuizError> findAllByQuizAuthor_Id(Long quizAuthorId);
}
