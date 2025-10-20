package com.unitbv.myquiz.web;

import com.unitbv.myquiz.config.TemplatePath;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuizAuthor;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value="/authors")
public class AuthorWSController {

    private static final Logger log = LoggerFactory.getLogger(AuthorWSController.class);
    AuthorService authorService;

    @Autowired
    public AuthorWSController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping(value="/")
    public String home(
            @RequestParam(value = "selectedCourse", required = false) String pathSelectedCourse,
            Model model
    ) {
        log.atInfo().log("AuthorWSController.home - selectedCourse={}", pathSelectedCourse);
        return "redirect:/authors";
    }

    @GetMapping("/{pageNo}")
    public String listAuthors(@PathVariable(value = "pageNo", required = false) Integer pageNo, Model model) {
        log.atInfo().log("AuthorWSController.listAuthors - pageNo={}", pageNo);
        List<AuthorDto> authorDtos = new ArrayList<>();
        if (null == pageNo || pageNo < 1) {
            pageNo = 1;
        }
        Page<Author> page =  authorService.findPaginated(pageNo, MyUtil.PAGE_SIZE, "name", "asc");
        page.stream().forEach(author -> authorDtos.add(authorService.getAuthorDTO(author, "")));
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("authors", authorDtos);
        return "author-list";
    }

    @GetMapping("/{author-id}/delete")
    public String deleteAuthor(@PathVariable(value = "author-id") Long id, RedirectAttributes redirectAttributes) {
        log.atInfo().log("AuthorWSController.deleteAuthor - id={}", id);
        List<QuizAuthor> quizAuthors = authorService.getQuizAuthorsForAuthorId(id);
        if (quizAuthors.size() > 0) {
            List<Long> idsQA = quizAuthors.stream().map(QuizAuthor::getId).toList();
            authorService.deleteQuizAuthorsByIds(idsQA);
        }
        authorService.deleteAuthorById(id);
        redirectAttributes.addFlashAttribute("message", "Autor successfully deleted: " + id);
        return "redirect:/success";
    }

    @PostMapping(value="/")
    public String filterAuthors(@RequestParam("course") String course,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        log.atInfo().log("AuthorWSController.filterAuthors - course={}", course);
        authorService.prepareFilterAuthorsModel(model, course);
        return "author-list";
    }

    @PostMapping(value = "/search")
    public String searchAuthors(@RequestParam("search") String search,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        log.atInfo().log("AuthorWSController.searchAuthors - search={}", search);
        if (search == null || search.isBlank()) {
            return "redirect:/authors/";
        }
        String authorName = search;

        Author author = authorService.getAuthorByName(authorName);
        if (author == null) {
            return "redirect:/authors/";
        }
        authorService.prepareAuthorModelData(model, authorName);

        return TemplatePath.QUESTION_LIST;
    }

    @GetMapping("/author/{authorName}")
    public String getQuestions(Model model, @PathVariable String authorName) {
        log.atInfo().log("AuthorWSController.getQuestions - authorName={}", authorName);
        Author author = authorService.getAuthorByName(authorName);
        if (author == null) {
            return "redirect:/authors/";
        }
        authorService.prepareAuthorModelData(model, authorName);

        return TemplatePath.QUESTION_LIST;
    }

}
