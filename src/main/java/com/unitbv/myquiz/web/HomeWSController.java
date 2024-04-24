package com.unitbv.myquiz.web;

import com.unitbv.myquiz.dto.AuthorDto;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.services.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeWSController {

    AuthorService authorService;

    @Autowired
    public HomeWSController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping(value="/")
    public String home(Model model) {
        List<AuthorDto> authorDtos = new ArrayList<>();
        List<Author> allAuthors = authorService.getAllAuthors();
        allAuthors.forEach(author -> authorDtos.add(authorService.getAuthorDTO(author)));
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", authorDtos.size());
        model.addAttribute("authors", authorDtos);
        return "author-list";
    }

    @GetMapping(value = "/about")
    public String about(Model model) {
        String version = System.getProperty("java.runtime.version");
        String os = System.getProperty("os.name");
        model.addAttribute("version", version + " on " + os);
        return "about";
    }

}
