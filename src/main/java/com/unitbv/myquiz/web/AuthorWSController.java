package com.unitbv.myquiz.web;

import com.unitbv.myquiz.dto.AuthorDto;
import com.unitbv.myquiz.services.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value="/authors")
public class AuthorWSController {

    AuthorService authorService;

    @Autowired
    public AuthorWSController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping()
    public String listAuthors(Model model) {
        List<AuthorDto> authorDtos = new ArrayList<>();
        authorService.getAllAuthors().forEach(author -> authorDtos.add(new AuthorDto(author)));
        model.addAttribute("authors", authorDtos);
        return "author-list";
    }


}
