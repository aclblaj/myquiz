package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.AuthorFormDataDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Thymeleaf controller for Author management operations.
 * Handles author listing, filtering, creation, editing, and deletion.
 */
@Controller
@RequestMapping("/authors")
public class ThyAuthorController {

    private static final Logger log = LoggerFactory.getLogger(ThyAuthorController.class);

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Autowired
    public ThyAuthorController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    private String renderAuthorListWithPageSize(Long courseId, Long authorId, Long questionBankId, Integer page, Integer pageSize, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = ControllerSettings.PAGE_SIZE;
        }
        AuthorFilterRequestDto filterInput = new AuthorFilterRequestDto();
        filterInput.setCourseId(courseId);
        filterInput.setPage(page);
        filterInput.setPageSize(pageSize);
        filterInput.setAuthorId(authorId);
        filterInput.setQuestionBankId(questionBankId);
        log.atInfo().addArgument(filterInput).log("filterInput: {}");
        AuthorFilterResponseDto filterDto;
        try {
            filterDto = getAuthorsFilteredDto(filterInput);
        } catch (RuntimeException ex) {
            log.atError().addArgument(ex.getMessage()).log("Redirecting to login due to session error: {}");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        log.atInfo().log("Rendering authors list with filter: {}, result: {}", filterInput, filterDto);
        log.atInfo().log("Authors: {}", Arrays.toString(filterDto != null ? filterDto.getAuthors().toArray() : new AuthorDto[0]));
        if (filterDto != null) {
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, filterDto.getAuthors());
            model.addAttribute(ControllerSettings.ATTR_COURSES, filterDto.getCourses());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, filterDto.getSelectedCourse());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, filterDto.getSelectedCourseId());
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, filterDto.getPage() != null ? filterDto.getPage() : page);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, filterDto.getTotalPages() != null ? filterDto.getTotalPages() : 1);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, filterDto.getTotalElements() != null ? filterDto.getTotalElements() : 0L);
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, filterDto.getPageSize() != null ? filterDto.getPageSize() : pageSize);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_AUTHOR_LIST, filterDto.getAuthorOptions());
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, filterDto.getQuestionBanks());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, filterDto.getSelectedQuestionBankId());
        } else {
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, new AuthorDto[0]);
            model.addAttribute(ControllerSettings.ATTR_COURSES, new ArrayList<>());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 0);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, 0L);
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, null);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, null);
        }
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_AUTHOR_LIST;
    }

    @GetMapping({"/", ""})
    public String listAuthors(
            @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model) {
        return renderAuthorListWithPageSize(courseId, authorId, questionBankId, page, pageSize, model);
    }

    @GetMapping({"/course/{course}/page/{page}", "/course/{course}", "/page/{page}"})
    public String listAuthorsByCourseAndPage(@PathVariable(value = "course", required = false) String course,
                                             @PathVariable(value = "page", required = false) Integer page,
                                             @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
                                             @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                                             @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                             Model model) {
        return renderAuthorListWithPageSize(null, authorId, questionBankId, page, pageSize, model);
    }

    private AuthorFilterResponseDto getAuthorsFilteredDto(AuthorFilterRequestDto filter) {
        log.atInfo().addArgument(filter).log("Getting filtered authors for input: {}");
        String url = apiBaseUrl + ControllerSettings.API_AUTHORS + "/filter";

        HttpEntity<AuthorFilterRequestDto> requestEntity = sessionService.createAuthorizedRequest(filter);
        log.atInfo().addArgument(url).addArgument(filter).log("Sending POST request to {} with filter: {}");

        try {
            ResponseEntity<AuthorFilterResponseDto> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, AuthorFilterResponseDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.Forbidden ex) {
            log.atError().log("403 Forbidden: Token may be invalid or expired. Redirecting to login");
            sessionService.invalidateCurrentSession();
            throw new SecurityException("Session expired or insufficient permissions. Please log in again.");
        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(ex.getMessage()).log("Error filtering authors: {}");
            return null;
        }
    }

    @GetMapping("/author/{name}")
    public String getAuthorByName(@PathVariable String name, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        ResponseEntity<AuthorFormDataDto> response = restTemplate.exchange(
                apiBaseUrl + "/authors/name/" + name + "/questions", HttpMethod.GET, entity, AuthorFormDataDto.class);
        AuthorFormDataDto authorData = response.getBody();
        prepareAuthorDataModelAttributes(model, authorData);
        return ControllerSettings.VIEW_QUESTION_LIST;
    }

    private void prepareAuthorDataModelAttributes(Model model, AuthorFormDataDto authorData) {
        Object loggedInUser = sessionService.getLoggedInUser();
        if (authorData != null && authorData.getAuthor() != null
                && authorData.getQuestionBankDtos() != null && !authorData.getQuestionBankDtos().isEmpty()) {
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorData.getAuthor());
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, authorData.getAuthorsList());
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, authorData.getQuestionBankDtos());
            model.addAttribute(ControllerSettings.ATTR_COURSE, authorData.getQuestionBankDtos().getFirst().getCourse());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorData.getAuthor().getId());
        } else {
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, null);
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, null);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, null);
            model.addAttribute(ControllerSettings.ATTR_COURSE, null);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, null);
        }
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
    }

    @PostMapping("/author/id/questions")
    public String getQuestionsByAuthorName(
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR, required = false) String selAuthorId,
            @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.info("Fetching questions for author ID: {} and courseId: {}", selAuthorId, courseId);
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        if (questionBankId != null) {
            ResponseEntity<QuestionBankDto> questionBankResponse = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + questionBankId,
                    HttpMethod.GET, entity, QuestionBankDto.class);
            QuestionBankDto questionBankDto = questionBankResponse.getBody();
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, questionBankDto);
        }

        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? ControllerSettings.PAGE_SIZE : pageSize;
        Long normalizedAuthorId = null;
        if (selAuthorId != null && !selAuthorId.isBlank()) {
            try {
                normalizedAuthorId = Long.valueOf(selAuthorId);
            } catch (NumberFormatException ex) {
                log.warn("Ignoring non-numeric authorId filter value: {}", selAuthorId);
            }
        }
        String filterUrl = apiBaseUrl + ControllerSettings.API_QUESTIONS_FILTER;
        QuestionFilterRequestDto filterInput = new QuestionFilterRequestDto();
        filterInput.setAuthorId(normalizedAuthorId);
        filterInput.setCourseId(courseId);
        filterInput.setQuestionBank(questionBankId);
        filterInput.setPage(safePage);
        filterInput.setPageSize(safePageSize);

        HttpEntity<QuestionFilterRequestDto> filterEntity = sessionService.createAuthorizedRequest(filterInput);
        ResponseEntity<QuestionFilterResponseDto> response = restTemplate.exchange(
                filterUrl, HttpMethod.POST,
                filterEntity, QuestionFilterResponseDto.class);
        QuestionFilterResponseDto questionData = response.getBody();

        model.addAttribute(ControllerSettings.ATTR_QUESTIONS, questionData != null ? questionData.getQuestions() : null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, selAuthorId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, questionData != null ? questionData.getSelectedCourse() : null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, questionData != null && questionData.getPage() != null ? questionData.getPage() : safePage);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, questionData != null && questionData.getTotalPages() != null ? questionData.getTotalPages() : 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, questionData != null && questionData.getTotalElements() != null ? questionData.getTotalElements() : 0L);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, questionData != null && questionData.getPageSize() != null ? questionData.getPageSize() : safePageSize);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_QUESTION_LIST;
    }

    @GetMapping("/edit/{id}")
    public String editAuthorForm(@PathVariable Long id,
                                 @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                                 @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
                                 @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                                 @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                 @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            prepareAuthorData(id, model);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_AUTHORS_URL, buildAuthorsBackUrl(courseId, authorId, questionBankId, page, pageSize));
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        } catch (HttpClientErrorException.NotFound ex) {
            log.atError().addArgument(id).log("Author with id {} not found");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE,
                "Author not found with ID: " + id);
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        } catch (HttpClientErrorException.Forbidden ex) {
            log.atError().addArgument(id).log("403 Forbidden when accessing author {}: Token expired or insufficient permissions");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(id).addArgument(ex.getMessage()).log("Error loading author for edit with id {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE,
                "Error loading author: " + ex.getMessage());
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        }
    }

    private void prepareAuthorData(Long id, Model model) {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        ResponseEntity<AuthorDto> response = restTemplate.exchange(
                apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id, HttpMethod.GET, entity, AuthorDto.class);
        AuthorDto author = response.getBody();

        if (author == null) {
            log.atWarn().addArgument(id).log("Author API returned null for id: {}");
            throw new IllegalStateException("Author not found with id: " + id);
        }

        model.addAttribute(ControllerSettings.ATTR_AUTHOR, author);
    }

    @PostMapping({ "/", "" })
    public String createAuthor(@ModelAttribute AuthorDto authorDto, RedirectAttributes redirectAttributes, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);

        // Validate AuthorDto fields before sending
        if (authorDto.getName() == null || authorDto.getName().isBlank()) {
            log.error("Author name is missing or blank. AuthorDto: {}", authorDto);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author name is required.");
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, false);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
        if (authorDto.getInitials() == null || authorDto.getInitials().isBlank()) {
            log.error("Author initials are missing or blank. AuthorDto: {}", authorDto);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author initials are required.");
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, false);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }

        log.atDebug().addArgument(authorDto).log("Sending AuthorDto to backend: {}");
        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(authorDto);

        try {
            restTemplate.postForEntity(apiBaseUrl + ControllerSettings.API_AUTHORS, entity, AuthorDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author created successfully");
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(ex.getMessage()).log("Error creating author: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE,
                    "Failed to create author: " + ex.getMessage());
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, false);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
    }

    @PutMapping("/{id}")
    @PostMapping("/{id}")
    public String updateAuthor(@PathVariable Long id, @ModelAttribute AuthorDto authorDto,
            RedirectAttributes redirectAttributes, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        // Set the ID to ensure it's included in the DTO
        authorDto.setId(id);

        // Validate AuthorDto fields before sending
        if (authorDto.getName() == null || authorDto.getName().isBlank()) {
            log.atError().addArgument(authorDto).log("Author name is missing or blank. AuthorDto: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author name is required.");
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
        if (authorDto.getInitials() == null || authorDto.getInitials().isBlank()) {
            log.atError().addArgument(authorDto).log("Author initials are missing or blank. AuthorDto: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author initials are required.");
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }

        log.atInfo().addArgument(id).addArgument(authorDto).log("Updating author with id={}, DTO: {}");
        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(authorDto);

        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id, HttpMethod.PUT, entity,
                    AuthorDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author updated successfully");
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(ex.getMessage()).log("Error updating author: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE,
                    "Failed to update author: " + ex.getMessage());
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
    }

    @DeleteMapping("/{id}")
    public String deleteAuthor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.atInfo().addArgument(id).log("Deleting author with id: {}");
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id, HttpMethod.DELETE, entity,
                Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author deleted successfully");
        return ControllerSettings.VIEW_REDIRECT_AUTHORS;
    }

    @GetMapping("/new")
    public String newAuthor(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.atInfo().log("Rendering new author form");
        model.addAttribute(ControllerSettings.ATTR_AUTHOR, new AuthorDto());
        model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, false);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_AUTHOR_EDIT;
    }

    @PostMapping("/search")
    public String searchAuthors(@RequestParam("search") String search, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        ResponseEntity<AuthorDto[]> response = restTemplate.exchange(apiBaseUrl + "/authors/search?name=" + search,
                HttpMethod.GET, entity, AuthorDto[].class);
        AuthorDto[] authors = response.getBody();
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, authors);
        return ControllerSettings.VIEW_AUTHOR_LIST;
    }

    @RequestMapping(value = "/filter", method = { RequestMethod.GET, RequestMethod.POST })
    public String filterAuthorsByCourse(
            @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
            @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
            @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
            Model model) {
        return renderAuthorListWithPageSize(courseId, authorId, questionBankId, page, pageSize, model);
    }

    @GetMapping("/delete/{id}")
    @ResponseBody
    public String deleteAuthorAjax(@PathVariable Long id) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return "Not authenticated";

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id, HttpMethod.DELETE, entity,
                Void.class);
        return "Author deleted";
    }

    @GetMapping("/{id}/details")
    public String showAuthorDetails(@PathVariable Long id,
                                    @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                                    @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
                                    @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                                    @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                    @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                    Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.atWarn().addArgument(id).log("Invalid session when accessing author details for id: {}");
            return redirect;
        }

        String backToAuthorsUrl = buildAuthorsBackUrl(courseId, authorId, questionBankId, page, pageSize);
        model.addAttribute(ControllerSettings.ATTR_BACK_TO_AUTHORS_URL, backToAuthorsUrl);
        model.addAttribute(ControllerSettings.ATTR_EDIT_AUTHOR_URL, buildAuthorEditUrl(id, courseId, authorId, questionBankId, page, pageSize));

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String url = apiBaseUrl + "/authors/" + id + "/details";

            log.atInfo().addArgument(url).log("Fetching author details from: {}");
            ResponseEntity<AuthorDetailsDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AuthorDetailsDto.class);

            AuthorDetailsDto details = response.getBody();

            if (details == null || details.getAuthor() == null) {
                log.atWarn().addArgument(id).log("No author details found for id: {}");
                populateAuthorDetailsModelFallback(model, "Author details not found.");
                return ControllerSettings.VIEW_AUTHOR_DETAILS;
            }

            log.atInfo().addArgument(id).log("Successfully loaded details for author id: {}");
            populateAuthorDetailsModelFromDto(model, details);
            return ControllerSettings.VIEW_AUTHOR_DETAILS;

        } catch (HttpClientErrorException.Forbidden ex) {
            log.atError().addArgument(id).log("403 Forbidden for author details id {}: Token expired or insufficient permissions");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;

        } catch (HttpClientErrorException.NotFound ex) {
            log.atWarn().addArgument(id).log("Author with id {} not found");
            populateAuthorDetailsModelFallback(model, "Author not found.");
            return ControllerSettings.VIEW_AUTHOR_DETAILS;

        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(id).addArgument(ex.getMessage()).log("Error fetching author details for id {}: {}");
            populateAuthorDetailsModelFallback(model, "Could not load author details. Please try again.");
            return ControllerSettings.VIEW_AUTHOR_DETAILS;
        }
    }

    private void populateAuthorDetailsModelFromDto(Model model, AuthorDetailsDto details) {
        model.addAttribute(ControllerSettings.ATTR_AUTHOR, details.getAuthor());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, details.getQuestionBanks());
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS_BY_QUESTION_BANK, details.getQuestionsByQuestionBank());
        model.addAttribute(ControllerSettings.ATTR_ERRORS_BY_QUESTION_BANK, details.getErrorsByQuestionBank());
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
    }

     private void populateAuthorDetailsModelFallback(Model model, String errorMessage) {
         model.addAttribute(ControllerSettings.ATTR_AUTHOR, new AuthorDto());
         model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, new QuestionBankDto[0]);
         model.addAttribute(ControllerSettings.ATTR_QUESTIONS_BY_QUESTION_BANK, new HashMap<Long, List<QuestionDto>>());
         model.addAttribute(ControllerSettings.ATTR_ERRORS_BY_QUESTION_BANK, new HashMap<Long, List<QuestionErrorDto>>());
         model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
         model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, errorMessage);
     }

    private String buildAuthorsBackUrl(Long courseId, Long authorId, Long questionBankId, Integer page, Integer pageSize) {
        StringBuilder url = new StringBuilder("/authors");
        String separator = "?";

        if (courseId != null) {
            url.append(separator)
               .append(ControllerSettings.ATTR_COURSE_ID)
               .append("=")
               .append(courseId);
            separator = "&";
        }
        if (authorId != null && authorId > 0) {
            url.append(separator).append(ControllerSettings.ATTR_AUTHOR_ID).append("=").append(authorId);
            separator = "&";
        }
        if (questionBankId != null && questionBankId > 0) {
            url.append(separator).append(ControllerSettings.ATTR_QUESTION_BANK_ID).append("=").append(questionBankId);
            separator = "&";
        }
        if (page != null && page > 0) {
            url.append(separator).append(ControllerSettings.ATTR_PAGE_NUMBER).append("=").append(page);
            separator = "&";
        }
        if (pageSize != null && pageSize > 0) {
            url.append(separator).append(ControllerSettings.ATTR_PAGE_SIZE).append("=").append(pageSize);
        }

        return url.toString();
    }

    private String buildAuthorEditUrl(Long id, Long courseId, Long authorId, Long questionBankId, Integer page, Integer pageSize) {
        String backUrl = buildAuthorsBackUrl(courseId, authorId, questionBankId, page, pageSize);
        if (!backUrl.contains("?")) {
            return "/authors/edit/" + id;
        }
        return "/authors/edit/" + id + "?" + backUrl.substring(backUrl.indexOf('?') + 1);
    }
}
