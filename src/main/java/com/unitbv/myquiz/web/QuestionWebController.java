package com.unitbv.myquiz.web;

import com.unitbv.myquiz.dto.AuthorErrorDto;
import com.unitbv.myquiz.dto.QuestionDto;
import com.unitbv.myquiz.entities.Question;
import com.unitbv.myquiz.entities.QuestionType;
import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorServiceImpl;
import com.unitbv.myquiz.services.QuestionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value="/questions")
public class QuestionWebController {

    private static final String QUESTION_LIST = "question-list";
    private final QuestionServiceImpl questionServiceImpl;

    private final AuthorErrorService authorErrorService;

    private final AuthorServiceImpl authorServiceImpl;

    @Autowired
    public QuestionWebController(QuestionServiceImpl questionServiceImpl, AuthorErrorService authorErrorService, AuthorServiceImpl authorServiceImpl) {
        this.questionServiceImpl = questionServiceImpl;
        this.authorErrorService = authorErrorService;
        this.authorServiceImpl = authorServiceImpl;
    }

    @GetMapping("/list")
    public String listQuestions(Model model) {
        List<QuestionDto> questionDtos = new ArrayList<>();
        questionServiceImpl.getQuestionsForAuthorName("Erika").forEach(question -> questionDtos.add(new QuestionDto(question)));
        model.addAttribute("questions", questionDtos);
        model.addAttribute("authorName", "Erika");
        return "question-list";
    }

    @GetMapping("/author/{authorName}")
    public String getQuestions(Model model, @PathVariable String authorName) {
        List<QuestionDto> questionDtos = new ArrayList<>();
        List<QuestionDto> questionDtosTF = new ArrayList<>();
        List<Question> allQ = questionServiceImpl.getQuestionsForAuthorName(authorName);
        allQ.forEach(question -> {
            if (question.getType() == QuestionType.MULTICHOICE) {
                questionDtos.add(new QuestionDto(question));
            } else if (question.getType() == QuestionType.TRUEFALSE) {
                questionDtosTF.add(new QuestionDto(question));
            }
        });


        model.addAttribute("questions", questionDtos);
        model.addAttribute("questionsTF", questionDtosTF);
        model.addAttribute("authorName", authorName);

        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        authorErrorService.getErrorsForAuthorName(authorName).forEach(error -> authorErrorDtos.add(new AuthorErrorDto(error)));
        model.addAttribute("errors", authorErrorDtos);

        return QUESTION_LIST;
    }

    @GetMapping("/deleteall")
    public String deleteAll(RedirectAttributes redirectAttributes) {
        authorErrorService.deleteAll();
        questionServiceImpl.deleteAllQuestions();
        authorServiceImpl.deleteAll();

        redirectAttributes.addFlashAttribute("message", "Successfully deleted tables content.");
        return "redirect:/success";
    }

}
