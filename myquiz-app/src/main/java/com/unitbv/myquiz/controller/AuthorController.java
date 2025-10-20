package com.unitbv.myquiz.controller;

import com.unitbv.myquiz.entities.Author;
import com.unitbv.myquiz.repositories.AuthorRepository;
import com.unitbv.myquiz.services.AuthorService;
import com.unitbv.myquiz.services.MyUtil;
import com.unitbv.myquizapi.dto.AuthorDto;
import com.unitbv.myquizapi.interfaces.AuthorApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping
public class AuthorController implements AuthorApi {

    private static final Logger log = LoggerFactory.getLogger(AuthorController.class);

    @Autowired
    private AuthorRepository authorRepository;

    AuthorService authorService;

    @Autowired
    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @Override
    public ResponseEntity<List<AuthorDto>> getAllAuthors() {
        log.atInfo().log("Getting all authors");
        List<Author> authors = authorRepository.findAll();
        List<AuthorDto> dtos = authors.stream()
                                      .map(a -> new AuthorDto(a.getId(), a.getName(), a.getInitials()))
                                      .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AuthorDto> getAuthorById(Long id) {
        log.atInfo().log("Getting author by id: {}", id);
        Author author = authorRepository.findById(id).orElse(null);
        if (author == null) return ResponseEntity.notFound().build();
        AuthorDto dto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<AuthorDto> createAuthor(@RequestBody AuthorDto authorDto) {
        log.atInfo().log("Creating new author: {}", authorDto.getName());
        Author author = new Author();
        author.setName(authorDto.getName());
        author.setInitials(authorDto.getInitials());
        authorRepository.save(author);
        AuthorDto dto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
        return ResponseEntity.status(201).body(dto);
    }

    @Override
    public ResponseEntity<AuthorDto> updateAuthor(Long id, @RequestBody AuthorDto authorDto) {
        log.atInfo().log("Updating author id: {}", id);
        Author author = authorRepository.findById(id).orElse(null);
        if (author == null) return ResponseEntity.notFound().build();
        author.setName(authorDto.getName());
        author.setInitials(authorDto.getInitials());
        authorRepository.save(author);
        AuthorDto dto = new AuthorDto(author.getId(), author.getName(), author.getInitials());
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteAuthor(Long id) {
        log.atInfo().log("Deleting author id: {}", id);
        if (!authorRepository.existsById(id)) return ResponseEntity.notFound().build();
        authorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<AuthorDto>> getAuthorsByDepartment(String department) {
        log.atInfo().log("Getting authors by department: {}", department);
        // Implement department search if your Author entity supports it, else return empty list
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/author/edit/{authorId}")
    public String editAuthorForm(@PathVariable Long authorId, org.springframework.ui.Model model, HttpSession session) {
        log.atInfo().log("Editing author id: {}", authorId);
        Author author = authorRepository.findById(authorId).orElse(null);
        if (author == null) {
            return "redirect:/authors";
        }
        model.addAttribute("author", author);
        session.setAttribute("selectedAuthor", authorId);
        return "author-edit";
    }

    @GetMapping("/author/new")
    public String newAuthorForm(org.springframework.ui.Model model) {
        log.atInfo().log("Creating new author form");
        model.addAttribute("author", new Author());
        model.addAttribute("editMode", false);
        return "author-edit";
    }

    @PostMapping("/author/new")
    public String addAuthor(@ModelAttribute Author author) {
        log.atInfo().log("Adding new author: {}", author.getName());
        authorRepository.save(author);
        return "redirect:/authors";
    }

    @GetMapping("/authors")
    public String listAuthors(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            org.springframework.ui.Model model) {
        List<String> courses = authorService.getCourseNames();
        String selectedCourse = courses.size()>0 ? courses.get(0) : "";
        List<AuthorDto> authorDtos = new ArrayList<>();

        Page<Author> page = authorService.findPaginatedFiltered(selectedCourse, pageNo, MyUtil.PAGE_SIZE, "name", "desc");
        page.stream().forEach(author -> authorDtos.add(authorService.getAuthorDTO(author, selectedCourse)));

        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("authors", authorDtos);
        model.addAttribute("courses", courses);
        model.addAttribute("route", "authors/");
        model.addAttribute("selectedCourse", selectedCourse);
        return "author-list";
    }

    @PostMapping("/author/edit/{authorId}")
    public String updateAuthor(@PathVariable Long authorId, @ModelAttribute Author author) {
        log.atInfo().log("Updating author id: {}", authorId);
        Author existingAuthor = authorRepository.findById(authorId).orElse(null);
        if (existingAuthor == null) {
            return "redirect:/authors";
        }
        // Ensure the id is set correctly from path variable if missing
        if (author.getId() == null || !authorId.equals(author.getId())) {
            author.setId(authorId);
        }
        existingAuthor.setName(author.getName());
        existingAuthor.setInitials(author.getInitials());
        authorRepository.save(existingAuthor);
        return "redirect:/authors";
    }
}
