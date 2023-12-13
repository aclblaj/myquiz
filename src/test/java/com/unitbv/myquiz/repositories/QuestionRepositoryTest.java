package com.unitbv.myquiz.repositories;

import com.unitbv.myquiz.entities.AuthorErrors;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuestionRepositoryTest {

    Logger logger = Logger.getLogger(QuestionRepositoryTest.class.getName());

    @Autowired
    QuestionRepository questionRepository;

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
        question = questionRepository.save(question);
        return question.getId();
    }


}
