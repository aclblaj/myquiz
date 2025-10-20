package com.unitbv.myquiz.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for AI question correction tool
 */
@Controller
public class QuestionCorrectionController {

    /**
     * Display the AI question correction tool page
     * @return the question correction template
     */
    @GetMapping("/question-correction")
    public String showQuestionCorrectionTool() {
        return "question-correction";
    }
}
