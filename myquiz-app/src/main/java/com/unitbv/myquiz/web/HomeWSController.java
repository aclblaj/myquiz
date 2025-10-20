package com.unitbv.myquiz.web;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.MyUtil;
import com.unitbv.myquizapi.dto.AuthorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeWSController {

    private static final Logger log = LoggerFactory.getLogger(AuthorWSController.class);

    AuthorService authorService;

    @Autowired
    public HomeWSController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping(value="/")
    public String home(Model model) {
        log.atInfo().log("HomeWSController.home - redirect to authors");
        return "redirect:/authors";
    }

    @GetMapping(value = "/about")
    public String about(Model model) {
        String version = System.getProperty("java.runtime.version");
        String os = System.getProperty("os.name");
        model.addAttribute("version", version + " on " + os);
        return "about";
    }

}
