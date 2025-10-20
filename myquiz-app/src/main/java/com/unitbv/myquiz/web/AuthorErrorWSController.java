package com.unitbv.myquiz.web;

import com.unitbv.myquiz.services.AuthorErrorService;
import com.unitbv.myquiz.services.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping(value="/errors")
public class AuthorErrorWSController {

    AuthorErrorService authorErrorService;
    AuthorService authorService;

    @Autowired
    public AuthorErrorWSController(AuthorErrorService authorErrorService, AuthorService authorService) {
        this.authorErrorService = authorErrorService;
        this.authorService = authorService;
    }

    @GetMapping(value="/")
    public String error(
            @RequestParam(value = "selectedCourse", required = false) String selectedCourse,
            @RequestParam(value = "selectedAuthor", required = false) String selectedAuthor,
            Model model
    ) {
        // Get authors for the selected course
//        List<String> authors = (selectedCourse != null && !selectedCourse.isEmpty())
//            ? authorService.getAuthorsByCourse(selectedCourse).stream().map(a -> a.getName()).distinct().toList()
//            : authorService.getAllAuthors().stream().map(a -> a.getName()).distinct().toList();
//        model.addAttribute("authors", authors);
        authorErrorService.getAuthorErrorsModel(model, selectedCourse, selectedAuthor);
        return "error-list";
    }

    @PostMapping(value="/")
    public String filteredAuthorErrors(@RequestParam("course") String selectedCourse,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        authorErrorService.getAuthorErrorsModel(model, selectedCourse, null);
        List<String> courses = authorErrorService.getAvailableCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourse", selectedCourse);
        return "error-list";
    }
}
