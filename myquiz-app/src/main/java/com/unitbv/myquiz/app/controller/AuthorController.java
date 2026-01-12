package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.*;
import com.unitbv.myquiz.api.interfaces.AuthorApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.QuizAuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/authors")
@CrossOrigin(origins = "${FRONTEND_URL}")
@Tag(name = "Authors", description = "Author management operations - Manage quiz authors and their contributions")
public class AuthorController implements AuthorApi {

    private static final Logger log = LoggerFactory.getLogger(AuthorController.class);

    private final AuthorService authorService;
    private final QuizAuthorService quizAuthorService;

    @Override
    public ResponseEntity<List<AuthorDto>> getAllAuthors() {
        log.info("Getting all authors");
        List<AuthorDto> dtos = authorService.getAllAuthors();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AuthorDto> getAuthorById(Long id) {
        log.info("Getting author by id: {}", id);
        AuthorDto dto = authorService.getAuthorById(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<AuthorDto> createAuthor(@RequestBody AuthorDto authorDto) {
        log.info("Creating new author: {}", authorDto.getName());
        AuthorDto dto = authorService.saveAuthorDto(authorDto);
        return ResponseEntity.status(201).body(dto);
    }

    @Override
    public ResponseEntity<AuthorDto> updateAuthor(Long id, @RequestBody AuthorDto authorDto) {
        log.info("Updating author id: {}", id);
        authorDto.setId(id);
        AuthorDto existingAuthorDto = authorService.saveAuthorDto(authorDto);
        return ResponseEntity.ok(existingAuthorDto);
    }

    @Override
    public ResponseEntity<Void> deleteAuthor(Long id) {
        log.info("Deleting author id: {}", id);
        AuthorDto dto = authorService.getAuthorById(id);
        if (dto == null) return ResponseEntity.notFound().build();
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AuthorFilterDto> listAuthors(@RequestBody AuthorFilterInputDto filterInput) {
        log.info("AuthorController.listAuthors - filter authors, {}", filterInput);
        AuthorFilterDto authorFilterDto = new AuthorFilterDto();
        List<String> courses = authorService.getCourseNames();
        log.info("Available courses: {}", courses);

        // Use getFirst() for better readability
        String selectedCourse = courses.stream().findFirst().orElse("");
        Object requestCourse = filterInput.getCourse();
        if (requestCourse != null) {
            selectedCourse = requestCourse.toString();
        }

        List<AuthorDto> authorDtos = new ArrayList<>();
        int pageNo = filterInput.getPage() != null ? filterInput.getPage() : 1;
        int pageSize = filterInput.getPageSize() != null ? filterInput.getPageSize() : ControllerSettings.PAGE_SIZE;
        log.info("Selected course: {}, page {}, pageSize {}", selectedCourse, pageNo, pageSize);

        Page<AuthorDto> page = authorService.findPaginatedFiltered(selectedCourse, filterInput.getAuthorId(), pageNo, pageSize, "name", "desc");
        log.info("Found {} authors for course {}", page.getTotalElements(), selectedCourse);
        String finalSelectedCourse = selectedCourse;
        page.getContent().forEach(authorDto -> authorDtos.add(
                authorService.getAuthorDTO(authorDto.getId(), finalSelectedCourse)));

        log.info("Prepared {} author DTOs for response", authorDtos.size());

        List<AuthorInfo> authorList = quizAuthorService.getAuthorDtosByCourse(selectedCourse);

        authorFilterDto.setPageNo(pageNo);
        authorFilterDto.setAuthors(authorDtos);
        authorFilterDto.setAuthorList(authorList);
        authorFilterDto.setCourses(courses);
        authorFilterDto.setSelectedCourse(selectedCourse);
        authorFilterDto.setTotalItems(page.getTotalElements());
        authorFilterDto.setTotalPages(page.getTotalPages());

        return ResponseEntity.ok(authorFilterDto);
    }

    @Override
    public ResponseEntity<AuthorDataDto> getQuestionsForAuthorName(@PathVariable String authorName) {
        log.info("AuthorController.getQuestions - authorName={}", authorName);
        AuthorDto author = authorService.getAuthorByName(authorName);
        if (author == null) return ResponseEntity.notFound().build();
        AuthorDataDto authorDataDto = authorService.prepareAuthorData(author.getName());
        return ResponseEntity.ok(authorDataDto);
    }

    @Override
    public ResponseEntity<AuthorDataDto> getQuestionsForAuthorId(@PathVariable Long authorId) {
        log.info("AuthorController.getQuestions - authorId={}", authorId);
        AuthorDto authorDto = authorService.getAuthorById(authorId);
        log.info("AuthorController.getQuestions - author={}", authorDto);
        if (authorDto == null) {
            return ResponseEntity.notFound().build();
        }
        AuthorDataDto authorDataDto = authorService.prepareAuthorData(authorDto.getName());
        return ResponseEntity.ok(authorDataDto);
    }

    @Override
    public ResponseEntity<AuthorDto> getAuthorByName(@PathVariable String name) {
        log.info("Getting author by name: {}", name);
        AuthorDto dto = authorService.getAuthorByName(name);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    /**
     * Get detailed information about an author including statistics.
     * Endpoint: GET /api/authors/{id}/details
     *
     * @param id Author ID
     * @return AuthorDetailsDto with complete author information
     */
    @GetMapping("/{id}/details")
    @Operation(
        summary = "Get Author Details",
        description = "Retrieve comprehensive information about an author including questions, quizzes, and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Author details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Author not found")
    })
    public ResponseEntity<AuthorDetailsDto> getAuthorDetails(
            @Parameter(description = "Author ID", required = true) @PathVariable Long id) {
        log.info("Getting author details for id: {}", id);
        AuthorDetailsDto details = authorService.getAuthorDetails(id);
        if (details == null || details.getAuthor() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

}
