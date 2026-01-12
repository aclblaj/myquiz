package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorDataDto;
import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorErrorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterDto;
import com.unitbv.myquiz.api.dto.AuthorFilterInputDto;
import com.unitbv.myquiz.api.dto.QuestionFilterDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String renderAuthorListWithPageSize(String course, Long authorId, Integer page, Integer pageSize, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = ControllerSettings.PAGE_SIZE;
        }
        AuthorFilterInputDto filterInput = AuthorFilterInputDto.builder()
                .course(course)
                .page(page)
                .pageSize(pageSize)
                .authorId(authorId)
                .build();
        log.info("filterInput: {}", filterInput);
        AuthorFilterDto filterDto;
        try {
            filterDto = getAuthorsFilteredDto(filterInput);
        } catch (RuntimeException ex) {
            log.error("Redirecting to login due to session error: {}", ex.getMessage());
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        log.atInfo().log("Rendering authors list with filter: {}, result: {}", filterInput, filterDto);
        log.atInfo().log("Authors: {}", Arrays.toString(filterDto != null ? filterDto.getAuthors().toArray() : new AuthorDto[0]));
        if (filterDto != null) {
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, filterDto.getAuthors());
            model.addAttribute(ControllerSettings.ATTR_COURSES, filterDto.getCourses());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, filterDto.getSelectedCourse());
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, filterDto.getPageNo());
            model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, filterDto.getTotalPages());
            model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, filterDto.getTotalItems());
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
            model.addAttribute(ControllerSettings.ATTR_AUTHOR_LIST, filterDto.getAuthorList());
        } else {
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, new AuthorDto[0]);
            model.addAttribute(ControllerSettings.ATTR_COURSES, new String[0]);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, course != null ? course : "");
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 0);
            model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, 0L);
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
        }
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_AUTHOR_LIST;
    }

    @GetMapping({"/", ""})
    public String listAuthors(
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            Model model) {
        return renderAuthorListWithPageSize(course, authorId, page, pageSize, model);
    }

    @GetMapping({"/course/{course}/page/{page}", "/course/{course}", "/page/{page}"})
    public String listAuthorsByCourseAndPage(@PathVariable(value = "course", required = false) String course,
                                             @PathVariable(value = "page", required = false) Integer page,
                                             @RequestParam(value = "authorId", required = false) Long authorId,
                                             @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                             Model model) {
        return renderAuthorListWithPageSize(course, authorId, page, pageSize, model);
    }

    private AuthorFilterDto getAuthorsFilteredDto(AuthorFilterInputDto filter) {
        log.info("Getting filtered authors for input: {}", filter);
        String url = apiBaseUrl + "/authors/filter";

        HttpEntity<AuthorFilterInputDto> requestEntity = sessionService.createAuthorizedRequest(filter);
        log.info("Sending POST request to {} with filter: {}", url, filter);

        try {
            ResponseEntity<AuthorFilterDto> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, AuthorFilterDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token may be invalid or expired. Redirecting to login");
            sessionService.invalidateCurrentSession();
            throw new SecurityException("Session expired or insufficient permissions. Please log in again.");
        } catch (Exception ex) {
            log.error("Error filtering authors: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @GetMapping("/author/{name}")
    public String getAuthorByName(@PathVariable String name, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        ResponseEntity<AuthorDataDto> response = restTemplate.exchange(
                apiBaseUrl + "/authors/name/" + name + "/questions", HttpMethod.GET, entity, AuthorDataDto.class);
        AuthorDataDto authorData = response.getBody();
        prepareAuthorDataModelAttributes(model, authorData);
        return ControllerSettings.VIEW_QUESTION_LIST;
    }

    private void prepareAuthorDataModelAttributes(Model model, AuthorDataDto authorData) {
        Object loggedInUser = sessionService.getLoggedInUser();
        if (authorData != null && authorData.getAuthorDTO() != null && authorData.getQuizDto() != null) {
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorData.getAuthorDTO());
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, authorData.getAuthorsList());
            model.addAttribute(ControllerSettings.ATTR_QUIZZES, authorData.getQuizDtos());
            model.addAttribute(ControllerSettings.ATTR_COURSE, authorData.getQuizDto().getCourse());
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorData.getAuthorDTO().getId());
        } else {
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, null);
            model.addAttribute(ControllerSettings.ATTR_AUTHORS, null);
            model.addAttribute(ControllerSettings.ATTR_QUIZZES, null);
            model.addAttribute(ControllerSettings.ATTR_COURSE, null);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, null);
        }
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
    }

    @PostMapping("/author/id/questions")
    public String getQuestionsByAuthorName(
            @RequestParam(value = "author", required = false) String selAuthorId,
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "quizId", required = false) Long quizId,
            Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.info("Fetching questions for author ID: {} and course: {}", selAuthorId, course);
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        if (quizId != null) {
            ResponseEntity<QuizDto> quizResponse = restTemplate.exchange(apiBaseUrl + "/quizzes/" + quizId,
                    HttpMethod.GET, entity, QuizDto.class);
            QuizDto quizDto = quizResponse.getBody();
            model.addAttribute("quiz", quizDto);
        }

        String filterUrl = apiBaseUrl + ControllerSettings.API_QUESTIONS_FILTER;
        Map<String, Object> params = new HashMap<>();
        if (selAuthorId != null && !selAuthorId.isEmpty()) {
            params.put("author", selAuthorId);
        }
        if (course != null && !course.isEmpty()) {
            params.put("course", course);
        }
        HttpEntity<?> filterEntity = sessionService.createAuthorizedRequest();
        StringBuilder urlBuilder = new StringBuilder(filterUrl);
        if (!params.isEmpty()) {
            urlBuilder.append("?");
            params.forEach((k, v) -> urlBuilder.append(k).append("=").append(v).append("&"));
            urlBuilder.setLength(urlBuilder.length() - 1); // Remove trailing &
        }
        ResponseEntity<QuestionFilterDto> response = restTemplate.exchange(
                urlBuilder.toString(), HttpMethod.GET,
                filterEntity, QuestionFilterDto.class);
        QuestionFilterDto questionData = response.getBody();
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS, questionData != null ? questionData.getQuestions() : null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, selAuthorId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, course);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_QUESTION_LIST;
    }

    @GetMapping("/edit/{id}")
    public String editAuthorForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            prepareAuthorData(id, model);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("Author with id {} not found", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE,
                "Author not found with ID: " + id);
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden when accessing author {}: Token expired or insufficient permissions", id);
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.error("Error loading author for edit with id {}: {}", id, ex.getMessage(), ex);
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
            log.warn("Author API returned null for id: {}", id);
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

        log.debug("Sending AuthorDto to backend: {}", authorDto);
        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(authorDto);

        try {
            restTemplate.postForEntity(apiBaseUrl + ControllerSettings.API_AUTHORS, entity, AuthorDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author created successfully");
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        } catch (Exception ex) {
            log.error("Error creating author: {}", ex.getMessage(), ex);
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
            log.error("Author name is missing or blank. AuthorDto: {}", authorDto);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author name is required.");
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }
        if (authorDto.getInitials() == null || authorDto.getInitials().isBlank()) {
            log.error("Author initials are missing or blank. AuthorDto: {}", authorDto);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author initials are required.");
            model.addAttribute(ControllerSettings.ATTR_AUTHOR, authorDto);
            model.addAttribute(ControllerSettings.ATTR_EDIT_MODE, true);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_AUTHOR_EDIT;
        }

        log.info("Updating author with id={}, DTO: {}", id, authorDto);
        HttpEntity<AuthorDto> entity = sessionService.createAuthorizedRequest(authorDto);

        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_AUTHORS + "/" + id, HttpMethod.PUT, entity,
                    AuthorDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Author updated successfully");
            return ControllerSettings.VIEW_REDIRECT_AUTHORS;
        } catch (Exception ex) {
            log.error("Error updating author: {}", ex.getMessage(), ex);
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

        log.info("Deleting author with id: {}", id);
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

        log.info("Rendering new author form");
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
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            Model model) {
        return renderAuthorListWithPageSize(course, authorId, page, pageSize, model);
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
    public String showAuthorDetails(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.warn("Invalid session when accessing author details for id: {}", id);
            return redirect;
        }

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String url = apiBaseUrl + "/authors/" + id + "/details";

            log.info("Fetching author details from: {}", url);
            ResponseEntity<AuthorDetailsDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AuthorDetailsDto.class);

            AuthorDetailsDto details = response.getBody();

            if (details == null || details.getAuthor() == null) {
                log.warn("No author details found for id: {}", id);
                populateAuthorDetailsModelFallback(model, "Author details not found.");
                return ControllerSettings.VIEW_AUTHOR_DETAILS;
            }

            log.info("Successfully loaded details for author id: {}", id);
            populateAuthorDetailsModelFromDto(model, details);
            return ControllerSettings.VIEW_AUTHOR_DETAILS;

        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden for author details id {}: Token expired or insufficient permissions", id);
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Author with id {} not found", id);
            populateAuthorDetailsModelFallback(model, "Author not found.");
            return ControllerSettings.VIEW_AUTHOR_DETAILS;

        } catch (Exception ex) {
            log.error("Error fetching author details for id {}: {}", id, ex.getMessage(), ex);
            populateAuthorDetailsModelFallback(model, "Could not load author details. Please try again.");
            return ControllerSettings.VIEW_AUTHOR_DETAILS;
        }
    }

    private void populateAuthorDetailsModelFromDto(Model model, AuthorDetailsDto details) {
        model.addAttribute(ControllerSettings.ATTR_AUTHOR, details.getAuthor());
        model.addAttribute(ControllerSettings.ATTR_QUIZZES, details.getQuizzes());
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS_BY_QUIZ, details.getQuestionsByQuiz());
        model.addAttribute(ControllerSettings.ATTR_ERRORS_BY_QUIZ, details.getErrorsByQuiz());
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
    }

    private void populateAuthorDetailsModelFallback(Model model, String errorMessage) {
        model.addAttribute(ControllerSettings.ATTR_AUTHOR, new AuthorDto());
        model.addAttribute(ControllerSettings.ATTR_QUIZZES, new QuizDto[0]);
        model.addAttribute(ControllerSettings.ATTR_QUESTIONS_BY_QUIZ, new HashMap<Long, List<QuestionDto>>());
        model.addAttribute(ControllerSettings.ATTR_ERRORS_BY_QUIZ, new HashMap<Long, List<AuthorErrorDto>>());
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, errorMessage);
    }
}
