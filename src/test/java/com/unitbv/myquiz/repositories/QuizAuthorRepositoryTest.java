package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.services.MyUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuizAuthorRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(QuizAuthorRepositoryTest.class);
    @Autowired
    QuizAuthorRepository quizAuthorRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private QuizRepository quizRepository;

    @Test
    void saveQuizAuthor(){
        Author author = new Author();
        author.setName("Max Mustermann");
        author.setInitials("MM");
        authorRepository.save(author);

        Quiz quiz = new Quiz();
        quiz.setName("Q1");
        quiz.setCourse("RC");
        quizRepository.save(quiz);

        QuizAuthor quizAuthor = new QuizAuthor();
        quizAuthor.setAuthor(author);
        quizAuthor.setQuiz(quiz);
        quizAuthor.setSource("file.xlsx");

        Set<QuizError> errors = new HashSet<>();

        QuizError quizError1 = new QuizError();
        quizError1.setDescription(MyUtil.TEMPLATE_ERROR_1_4_POINTS_WRONG);
        quizError1.setQuizAuthor(quizAuthor);

        errors.add(quizError1);

        QuizError quizError2 = new QuizError();
        quizError2.setDescription(MyUtil.SKIPPED_DUE_TO_ERROR);
        quizError2.setQuizAuthor(quizAuthor);

        errors.add(quizError2);

        quizAuthor.setQuizErrors(errors);

        quizAuthor = quizAuthorRepository.save(quizAuthor);

        Long authorId = quizAuthor.getAuthor().getId();
        Long quizId = quizAuthor.getQuiz().getId();
        log.info("Author id: {}", authorId);
        log.info("Quiz id: {}", quizId);

        assertNotNull(quizAuthor.getId());

        quizAuthorRepository.delete(quizAuthor);
        author = authorRepository.findById(authorId).orElse(null);
        authorRepository.delete(author);
        quiz = quizRepository.findById(quizId).orElse(null);
        quizRepository.delete(quiz);

    }

}