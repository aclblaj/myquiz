package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class QuestionRepositoryTest {

    Logger logger = Logger.getLogger(QuestionRepositoryTest.class.getName());

    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    private QuizAuthorRepository quizAuthorRepository;

    @Test
    void findById() {
        long idQuestion = addTestQuestion();
        Question question = questionRepository.findById(idQuestion).get();
        assertEquals(idQuestion, question.getId());
        removeTestQuestion(idQuestion);
        question = questionRepository.findById(idQuestion).orElse(null);
        assertNull(question);
    }

    private void removeTestQuestion(long idQuestion) {
        questionRepository.delete(questionRepository.findById(idQuestion).get());
    }

    private long addTestQuestion() {
        Question question = new Question();
        question.setTitle("Test question");

        Author author = new Author();
        author.setName("Max Mustermann");
        author.setInitials("MM");

        Quiz quiz = new Quiz();
        quiz.setName("Q1");
        quiz.setCourse("RC");

        QuizAuthor quizAuthor = new QuizAuthor();
        quizAuthor.setAuthor(author);
        quizAuthor.setQuiz(quiz);
        quizAuthor.setSource("file.xlsx");

        quizAuthor = quizAuthorRepository.save(quizAuthor);

        question.setQuizAuthor(quizAuthor);

        question = questionRepository.save(question);
        return question.getId();
    }


}
