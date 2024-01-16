package com.unitbv.myquiz.services;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.repositories.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class QuestionServiceTest {

    Logger logger = org.slf4j.LoggerFactory.getLogger(QuestionServiceTest.class);

    @Autowired
    QuestionService questionService;

    @Autowired
    EncodingSevice encodingSevice;

    @Autowired
    AuthorRepository authorRepository;

    @Test
    void parseExcelFilesFromFolderITSecQ1() {
        long startTime = (int) System.currentTimeMillis();
        if (encodingSevice.checkServerEncoding()) return;
        final String XLSX_DIR_WITH_FILES = "C:\\Temp\\inpQ1\\";
        String folderPath = XLSX_DIR_WITH_FILES;
        File folder = new File(folderPath);
        int result = questionService.parseExcelFilesFromFolder(folder, 0);
        logger.info("Number of parsed excel files: {}", result);
        long endTime = (int) System.currentTimeMillis();
        logger.info("Execution time: {} ms", (endTime - startTime));
        assertNotEquals(0, result);
    }

    @Test
    void getServerEncoding() {
        String result = encodingSevice.getServerEncoding();
        logger.info("Server encoding: {}", result);
        assertNotNull(result);
    }

    @Test
    void getAuthorQuestions() {

        Author author = new Author("Erika Mustermann", "EM");

        Set<Question> questions = new HashSet<>();

        Question question1 = new Question();
        question1.setAuthor(author);
        question1.setCrtNo(1);
        question1.setTitle("Title 1");
        question1.setText("Text 1");
        questions.add(question1);

        Question question2 = new Question();
        question2.setAuthor(author);
        question2.setCrtNo(2);
        question2.setTitle("Title 2");
        question2.setText("Text 2");
        questions.add(question2);

        author.setQuestions(questions);
        author = authorRepository.save(author);
        logger.info("Author: {}", author);

        List<Question> result = questionService.getQuestionsForAuthorId(author.getId());
        logger.info("Number of questions: {}", result.size());
        result.forEach(question -> logger.info("Question: {}", question));
        assertNotNull(result);
    }

    @Test
    void getAuthorQuestionsByName() {
        Author author = new Author("Erika Mustermann", "EM");

        Set<Question> questions = new HashSet<>();

        Question question1 = new Question();
        question1.setAuthor(author);
        question1.setCrtNo(1);
        question1.setTitle("Title 1");
        question1.setText("Text 1");
        questions.add(question1);

        Question question2 = new Question();
        question2.setAuthor(author);
        question2.setCrtNo(2);
        question2.setTitle("Title 2");
        question2.setText("Text 2");
        questions.add(question2);

        author.setQuestions(questions);
        author = authorRepository.save(author);
        logger.info("Author: {}", author);

        List<Question> result = questionService.getQuestionsForAuthorName("Diana");
        logger.info("Number of questions: {}", result.size());
        result.forEach(question -> logger.info("Question: {}", question));
        assertNotNull(result);
    }
}