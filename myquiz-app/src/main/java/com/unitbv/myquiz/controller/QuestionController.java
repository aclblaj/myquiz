package com.unitbv.myquiz.controller;

import com.unitbv.myquiz.services.impl.QuestionServiceImpl;
import com.unitbv.myquiz.web.ResourceNotFoundException;
import com.unitbv.myquizapi.dto.QuestionDto;
import com.unitbv.myquizapi.interfaces.QuestionApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * REST controller for handling question-related endpoints.
 * Implements the centralized QuestionApi interface.
 */
@RestController
@RequestMapping("/api/questions") // Changed mapping to avoid conflict
public class QuestionController implements QuestionApi {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionServiceImpl questionServiceImpl;

    /**
     * Constructor for dependency injection.
     * @param questionServiceImpl the question service implementation
     */
    @Autowired
    public QuestionController(QuestionServiceImpl questionServiceImpl) {
        this.questionServiceImpl = questionServiceImpl;
    }

    @Override
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        logger.info("Getting all questions");
        try {
            List<QuestionDto> questions = questionServiceImpl.getAllQuestions();
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            logger.error("Error getting all questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<QuestionDto> getQuestionById(Long id) {
        logger.info("Getting question by id: {}", id);
        try {
            QuestionDto question = questionServiceImpl.getQuestionById(id);
            if (question != null) {
                return ResponseEntity.ok(question);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (ResourceNotFoundException e) {
            logger.warn("Question not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting question by id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<QuestionDto> createQuestion(QuestionDto questionDto) {
        logger.info("Creating new question");
        try {
            QuestionDto createdQuestion = questionServiceImpl.createQuestion(questionDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
        } catch (Exception e) {
            logger.error("Error creating question", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<QuestionDto> updateQuestion(Long id, QuestionDto questionDto) {
        logger.info("Updating question with id: {}", id);
        try {
            questionDto.setId(id);
            QuestionDto updatedQuestion = questionServiceImpl.updateQuestion(questionDto);
            if (updatedQuestion != null) {
                return ResponseEntity.ok(updatedQuestion);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (ResourceNotFoundException e) {
            logger.warn("Question not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating question with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteQuestion(Long id) {
        logger.info("Deleting question with id: {}", id);
        try {
            boolean deleted = questionServiceImpl.deleteQuestion(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (ResourceNotFoundException e) {
            logger.warn("Question not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting question with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<QuestionDto>> getQuestionsByQuizId(Long quizId) {
        logger.info("Getting questions by quiz id: {}", quizId);
        try {
            List<QuestionDto> questions = questionServiceImpl.getQuestionsByQuizId(quizId);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            logger.error("Error getting questions by quiz id: {}", quizId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Removed the /questions mapping to avoid conflict with QuestionWebController
}
