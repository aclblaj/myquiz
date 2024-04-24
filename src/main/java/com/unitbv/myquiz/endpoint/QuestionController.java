package com.unitbv.myquiz.endpoint;

import com.unitbv.myquiz.dto.QuestionDto;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.services.QuestionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api")
@RestController("/question")
public class QuestionController {

    Logger logger = LoggerFactory.getLogger(QuestionController.class.getName());

    QuestionServiceImpl questionServiceImpl;

    @Autowired
    public QuestionController(QuestionServiceImpl questionServiceImpl) {
        this.questionServiceImpl = questionServiceImpl;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    @GetMapping("/{authorId}")
    public List<QuestionDto> getQuestionByAuthorId(@PathVariable Long authorId) {
        logger.atInfo().addArgument(authorId)
              .log("getQuestionByAuthorId: {}");
        List<Question> questions;
        List<QuestionDto> questionDtos = new ArrayList<>();
        if (null !=authorId) {
            questions = questionServiceImpl.getQuestionsForAuthorId(authorId);
            for (Question question : questions) {
                questionDtos.add(new QuestionDto(question));
            }
        }
        return questionDtos;
    }

    @GetMapping("/{authorName}")
    public List<QuestionDto> getQuestionByAuthorName(@PathVariable String authorName) {
        logger.atInfo().addArgument(authorName)
              .log("getQuestionByAuthorName: {}");
        List<Question> questions;
        List<QuestionDto> questionDtos = new ArrayList<>();
        if (null !=authorName) {
            questions = questionServiceImpl.getQuestionsForAuthorName(authorName);
            for (Question question : questions) {
                questionDtos.add(new QuestionDto(question));
            }
        }
        return questionDtos;
    }

}
