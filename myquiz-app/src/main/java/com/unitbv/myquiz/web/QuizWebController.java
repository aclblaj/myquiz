package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Quiz;
import com.unitbv.myquiz.services.QuizService;
import com.unitbv.myquizapi.dto.QuizDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class QuizWebController {
    @Autowired
    private QuizService quizService;

    @GetMapping("/quiz/course/{courseName}")
    public String getQuizzesByCourse(@PathVariable("courseName") String courseName, Model model) {
        List<QuizDto> quizzes = quizService.getQuizzesByCourse(courseName);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("courseName", courseName);
        return "quiz-course";
    }
}

