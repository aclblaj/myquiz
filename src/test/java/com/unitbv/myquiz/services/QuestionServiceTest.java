package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.repositories.QuizAuthorRepository;
import com.unitbv.myquiz.util.TemplateType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class QuestionServiceTest {

    Logger logger = LoggerFactory.getLogger(QuestionServiceTest.class);

    @Autowired
    QuestionService questionService;

    @Autowired
    EncodingSevice encodingSevice;

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    QuizAuthorRepository quizAuthorRepository;

    @Autowired
    AuthorService authorService;

    @Autowired
    QuizService quizService;

    @Test
    void parseExcelFilesFromFolder() {
        long startTime = (int) System.currentTimeMillis();
        if (encodingSevice.checkServerEncoding()) return;
        final String XLSX_DIR_WITH_FILES = "c:\\work\\_mi\\2024-RC\\inpQ1-IE\\";
        File folder = new File(XLSX_DIR_WITH_FILES);
        Quiz quiz = quizService.createQuizz("IE2-RC", "Q1", 2024L);
        questionService.setTemplateType(TemplateType.Template2023);
        authorService.setAuthorsList(new ArrayList<>());
        int result = questionService.parseExcelFilesFromFolder(quiz, folder, 0);
        logger.atInfo().addArgument(result).log("Number of parsed excel files: {}");
        long endTime = (int) System.currentTimeMillis();
        logger.atInfo().addArgument((endTime - startTime)).log("Execution time: {} ms");
        questionService.checkDuplicatesQuestionsForAuthors(authorService.getAuthorsList());
        logger.atInfo().addArgument(authorService.getAuthorsList()).log("List of imported authors: {}");
        assertNotEquals(-1, result);
    }

    @Test
    void getServerEncoding() {
        String result = encodingSevice.getServerEncoding();
        logger.atInfo().addArgument(result).log("Server encoding: {}");
        assertNotNull(result);
    }

    @Test
    void getQuestionsByAuthorId() {
        QuizAuthor quizAuthor = createQuizWithQuestions();
        List<Question> result = questionService.getQuestionsForAuthorId(quizAuthor.getAuthor().getId());
        logger.atInfo().addArgument(result.size()).log("Number of questions: {}");
        result.forEach(question -> logger.atInfo().addArgument(question).log("Question: {}"));
        assertNotNull(result);
    }

    @Test
    void getQuestionsByAuthorName() {
        QuizAuthor quizAuthor = createQuizWithQuestions();
        List<Question> result = questionService.getQuestionsForAuthorName("Diana");
        logger.atInfo().addArgument(result.size()).log("Number of questions: {}");
        result.forEach(question -> logger.atInfo().addArgument(question).log("Question: {}"));
        assertNotNull(result);
    }


    private QuizAuthor createQuizWithQuestions() {
        Author author = new Author("Erika Diana Mustermann", "EDM");

        Quiz quiz = new Quiz();
        quiz.setName("Q1");
        quiz.setCourse("RC");
        quiz.setYear(2024L);

        QuizAuthor quizAuthor = new QuizAuthor();
        quizAuthor.setAuthor(author);
        quizAuthor.setQuiz(quiz);
        quizAuthor.setSource("File-RC.xlsx");

        Set<Question> questions = new HashSet<>();

        Question question1 = new Question();
        question1.setCrtNo(1);
        question1.setTitle("Title Q1");
        question1.setText("Text Q1");
        question1.setQuizAuthor(quizAuthor);
        questions.add(question1);

        Question question2 = new Question();
        question2.setCrtNo(2);
        question2.setTitle("Title Q22");
        question2.setText("Text Q2");
        question2.setQuizAuthor(quizAuthor);
        questions.add(question2);

        quizAuthor.setQuestions(questions);

        quizAuthor = quizAuthorRepository.save(quizAuthor);

        logger.atInfo().addArgument(author).log("Author: {}");
        return quizAuthor;
    }

}