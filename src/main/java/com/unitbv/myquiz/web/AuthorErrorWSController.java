package com.unitbv.myquiz.web;

import com.unitbv.myquiz.dto.AuthorErrorDto;
import com.unitbv.myquiz.entities.QuizError;
import com.unitbv.myquiz.services.AuthorErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value="/errors")
public class AuthorErrorWSController {

    AuthorErrorService authorErrorService;

    @Autowired
    public AuthorErrorWSController(AuthorErrorService authorErrorService) {
        this.authorErrorService = authorErrorService;
    }

    @GetMapping
    public String error(Model model) {
        List<AuthorErrorDto> authorErrorDtos = new ArrayList<>();
        for (QuizError error : authorErrorService.getErrors()) {
            authorErrorDtos.add(new AuthorErrorDto(error));
        }

        Map<String, List<AuthorErrorDto>> errorsByAuthor = new HashMap<>();
        for (AuthorErrorDto error : authorErrorDtos) {
            errorsByAuthor.computeIfAbsent(error.getAuthorName(), k -> new ArrayList<>()).add(error);
        }

        model.addAttribute("errorsByAuthor", errorsByAuthor);
        model.addAttribute("errors", authorErrorDtos);

        return "error-list";
    }
}
