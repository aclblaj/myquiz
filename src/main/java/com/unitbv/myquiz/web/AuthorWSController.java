package com.unitbv.myquiz.web;

import com.unitbv.myquiz.dto.AuthorDto;
import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.entities.QuizAuthor;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping(value="/")
    public String home(Model model) {
        List<AuthorDto> authorDtos = new ArrayList<>();
        int pageNo = 1;
        Page<Author> page =  authorService.findPaginated(pageNo, MyUtil.PAGE_SIZE, "name", "desc");
        page.stream().forEach(author -> authorDtos.add(authorService.getAuthorDTO(author)));
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("authors", authorDtos);
        return "author-list";
    }

    @GetMapping("/{pageNo}")
    public String listAuthors(@PathVariable(value = "pageNo", required = false) Integer pageNo, Model model) {
        List<AuthorDto> authorDtos = new ArrayList<>();
        if (null == pageNo || pageNo < 1) {
            pageNo = 1;
        }
        Page<Author> page =  authorService.findPaginated(pageNo, MyUtil.PAGE_SIZE, "name", "asc");
        page.stream().forEach(author -> authorDtos.add(authorService.getAuthorDTO(author)));
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("authors", authorDtos);
        return "author-list";
    }

    @GetMapping("/{author-id}/delete")
    public String deleteAuthor(@PathVariable(value = "author-id") Long id, RedirectAttributes redirectAttributes) {
        List<QuizAuthor> quizAuthors = authorService.getQuizAuthorsForAuthorId(id);
        if (quizAuthors.size() > 0) {
            List<Long> idsQA = quizAuthors.stream().map(QuizAuthor::getId).toList();
            authorService.deleteQuizAuthorsByIds(idsQA);
        }
        authorService.deleteAuthorById(id);
        redirectAttributes.addFlashAttribute("message", "Autor successfully deleted: " + id);
        return "redirect:/success";
    }


}
