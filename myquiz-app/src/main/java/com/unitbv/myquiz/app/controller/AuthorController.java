package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.AuthorFormDataDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.AuthorUpsertDto;
import com.unitbv.myquiz.api.dto.CourseInfo;
import com.unitbv.myquiz.api.dto.QuestionBankInfo;
import com.unitbv.myquiz.api.interfaces.AuthorApi;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.FilterOptionsService;
import com.unitbv.myquiz.app.services.QuestionBankAuthorService;
import com.unitbv.myquiz.app.services.QuestionBankService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/authors")
@CrossOrigin(origins = "${FRONTEND_URL}")
@Tag(name = "Authors", description = "Author management operations - Manage questionBank authors and their contributions")
public class AuthorController implements AuthorApi {

    private static final Logger log = LoggerFactory.getLogger(AuthorController.class);

    private final AuthorService authorService;
    private final QuestionBankAuthorService questionBankAuthorService;
    private final QuestionBankService questionBankService;
    private final CourseService courseService;
    private final FilterOptionsService filterOptionsService;

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
    public ResponseEntity<AuthorDto> createAuthor(@Valid @RequestBody AuthorUpsertDto authorUpsertDto) {
        log.info("Creating new author: {}", authorUpsertDto.getName());
        AuthorDto dto = authorService.saveAuthorDto(authorUpsertDto.toAuthorDto(null));
        return ResponseEntity.status(201).body(dto);
    }

    @Override
    public ResponseEntity<AuthorDto> updateAuthor(Long id, @Valid @RequestBody AuthorUpsertDto authorUpsertDto) {
        log.info("Updating author id: {}", id);
        AuthorDto dto = authorService.saveAuthorDto(authorUpsertDto.toAuthorDto(id));
        return ResponseEntity.ok(dto);
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
    public ResponseEntity<AuthorFilterResponseDto> listAuthors(@Valid @RequestBody AuthorFilterRequestDto filterInput) {
        log.info("AuthorController.listAuthors - filter authors, {}", filterInput);
        if (filterInput == null) {
            filterInput = new AuthorFilterRequestDto();
        }
        AuthorFilterResponseDto authorFilterDto = new AuthorFilterResponseDto();
        List<CourseInfo> courses = courseService.getAllCourses().stream().map(CourseInfo::from).toList();
        log.info("Available courses: {}", courses);

        Long selectedCourseId = filterInput.getCourseId();
        String selectedCourse = null;
        if (selectedCourseId != null) {
            selectedCourse = courseService.getCourseName(selectedCourseId);
        } else if (filterInput.getCourse() != null) {
            selectedCourse = filterInput.getCourse();
            String selectedCourseName = selectedCourse;
            selectedCourseId = courses.stream()
                    .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(selectedCourseName))
                    .map(CourseInfo::getId)
                    .findFirst()
                    .orElse(null);
        }

        PaginationParams pagination = PaginationSupport.normalize(filterInput.getPage(), filterInput.getPageSize());
        int pageNo = pagination.page();
        int pageSize = pagination.pageSize();
        Long questionBankId = filterInput.getQuestionBankId();
        log.info("Selected courseId: {}, selected course: {}, questionBankId: {}, page {}, pageSize {}", selectedCourseId, selectedCourse, questionBankId, pageNo, pageSize);

        Page<AuthorDto> page = authorService.findPaginatedFiltered(
                selectedCourse, filterInput.getAuthorId(), questionBankId, pageNo, pageSize, "name", "asc");
        log.info("Found {} authors for course {} and questionBankId {}", page.getTotalElements(), selectedCourse, questionBankId);
        List<AuthorDto> authorDtos = page.getContent();

        log.info("Prepared {} author DTOs for response", authorDtos.size());

        List<AuthorInfo> authorList = filterOptionsService.resolveAuthorOptions(questionBankId, selectedCourse);

        // Get question banks list for the selected course
        List<QuestionBankInfo> questionBanks = new ArrayList<>();
        if (selectedCourse != null && !selectedCourse.isEmpty()) {
            questionBanks = questionBankService.getQuestionBankInfoByCourse(selectedCourse);
        }

        authorFilterDto.setPage(pageNo);
        authorFilterDto.setAuthors(authorDtos);
        authorFilterDto.setAuthorOptions(authorList);
        authorFilterDto.setCourses(courses);
        authorFilterDto.setSelectedCourse(selectedCourse);
        authorFilterDto.setSelectedCourseId(selectedCourseId);
        authorFilterDto.setQuestionBanks(questionBanks);
        authorFilterDto.setSelectedQuestionBankId(questionBankId);
        authorFilterDto.setTotalElements(page.getTotalElements());
        authorFilterDto.setTotalPages(page.getTotalPages());

        return ResponseEntity.ok(authorFilterDto);
    }

    @Override
    public ResponseEntity<AuthorFormDataDto> getQuestionsForAuthorName(String authorName) {
        log.info("AuthorController.getQuestions - authorName={}", authorName);
        AuthorDto author = authorService.getAuthorByName(authorName);
        if (author == null) return ResponseEntity.notFound().build();
        AuthorFormDataDto authorDataDto = authorService.prepareAuthorData(author.getName());
        return ResponseEntity.ok(authorDataDto);
    }

    @Override
    public ResponseEntity<AuthorFormDataDto> getQuestionsForAuthorId(Long authorId) {
        log.info("AuthorController.getQuestions - authorId={}", authorId);
        AuthorDto authorDto = authorService.getAuthorById(authorId);
        log.info("AuthorController.getQuestions - author={}", authorDto);
        if (authorDto == null) {
            return ResponseEntity.notFound().build();
        }
        AuthorFormDataDto authorDataDto = authorService.prepareAuthorData(authorDto.getName());
        return ResponseEntity.ok(authorDataDto);
    }

    @Override
    public ResponseEntity<AuthorDto> getAuthorByName(String name) {
        log.info("Getting author by name: {}", name);
        AuthorDto dto = authorService.getAuthorByName(name);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<AuthorDetailsDto> getAuthorDetails(Long id) {
        log.info("Getting author details for id: {}", id);
        AuthorDetailsDto details = authorService.getAuthorDetails(id);
        if (details == null || details.getAuthor() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

}
