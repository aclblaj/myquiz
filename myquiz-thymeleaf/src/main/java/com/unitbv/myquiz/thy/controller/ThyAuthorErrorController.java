package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorErrorFilterDto;
import com.unitbv.myquiz.api.dto.AuthorErrorFilterInputDto;
import com.unitbv.myquiz.api.dto.QuizFilterDto;
import com.unitbv.myquiz.thy.service.SessionService;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Thymeleaf controller for Author Error management operations.
 * Handles error listing, filtering, and viewing for quiz authors.
 * Provides server-side rendering for error-related pages.
 */
@Controller
@RequestMapping("/errors")
public class ThyAuthorErrorController {

    private static final Logger log = LoggerFactory.getLogger(ThyAuthorErrorController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyAuthorErrorController(SessionService sessionService, RestTemplate restTemplate) {
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
    }

    @GetMapping({"","/"})
    public String showAuthorError(
            @RequestParam(value = ControllerSettings.ATTR_SELECTED_COURSE, required = false) String selectedCourse,
            @RequestParam(value = ControllerSettings.ATTR_SELECTED_AUTHOR, required = false) String selectedAuthor,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false, defaultValue = "10") Integer pageSize,
            Model model) {

        log.info("Showing author error list for course: {} and author: {}, page: {}, page size: {}",
                         selectedCourse, selectedAuthor, pageNumber, pageSize);
        // Default to page 1 when not specified (e.g., when accessing from fragments menu)
        Integer safePage = (pageNumber != null && pageNumber > 0) ? pageNumber : 1;

        // Delegate to common render method
        return renderAuthorErrorList(selectedCourse, selectedAuthor, safePage, pageSize, model);
    }

    // GET mapping with path parameters for direct linking from author list
    @GetMapping("/filter/{course}/{author}/{page}/{pageSize}")
    public String filterErrorsWithPathParams(
            @PathVariable("course") String course,
            @PathVariable("author") String author,
            @PathVariable("page") Integer page,
            @PathVariable("pageSize") Integer pageSize,
            Model model) {

        return renderAuthorErrorList(course, author, page, pageSize, model);
    }

    // POST-based filter to mirror authors filtering
    @PostMapping(value = "/filter", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String filterErrors(@RequestParam(value = ControllerSettings.ATTR_SELECTED_COURSE, required = false) String selectedCourse,
                               @RequestParam(value = ControllerSettings.ATTR_SELECTED_AUTHOR, required = false) String selectedAuthor,
                               @RequestParam(value = ControllerSettings.ATTR_CURRENT_PAGE, required = false) Integer page,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false, defaultValue = "10") Integer pageSize,
                               Model model) {

        return renderAuthorErrorList(selectedCourse, selectedAuthor, page, pageSize, model);
    }

    private String renderAuthorErrorList(
            String selectedCourse,
            String selectedAuthor,
            Integer page,
            Integer pageSize,
            Model model
    ) {
        log.info("Rendering author error list for course: {} and author: {}, page: {}, pageSize: {}",
                         selectedCourse, selectedAuthor, page, pageSize);

        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            model.addAttribute(ControllerSettings.ATTR_QUIZ_FILTER, new QuizFilterDto());
            return redirect;
        }

        Object loggedInUser = sessionService.getLoggedInUser();

        int safePage = page != null && page > 0 ? page : 1;
        int safePageSize = pageSize != null && pageSize > 0 ? pageSize : ControllerSettings.PAGE_SIZE;

        AuthorErrorFilterInputDto input = new AuthorErrorFilterInputDto();
        if ("All Courses".equals(selectedCourse)) {
            selectedCourse = null;
        }
        if ("All Authors".equals(selectedAuthor)) {
            selectedAuthor = null;
        }
        input.setSelectedCourse(selectedCourse);
        input.setSelectedAuthor(selectedAuthor);
        input.setPage(safePage);
        input.setPageSize(safePageSize);

        HttpEntity<AuthorErrorFilterInputDto> requestEntity = sessionService.createAuthorizedRequest(input);

        try {
            ResponseEntity<AuthorErrorFilterDto> response = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_ERRORS_FILTER,
                    org.springframework.http.HttpMethod.POST,
                    requestEntity,
                    AuthorErrorFilterDto.class
            );
            AuthorErrorFilterDto dto = response.getBody();
            if (dto != null) {
                populateModelFromDto(model, dto, safePage, safePageSize);
            } else {
                populateModelFallback(model, safePage, safePageSize);
            }
        } catch (HttpClientErrorException.Forbidden ex403) {
            log.error("403 Forbidden filtering author errors. Token expired or insufficient permissions");
            sessionService.invalidateCurrentSession();
            populateModelFallback(model, safePage, safePageSize);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.error("Error filtering author errors: {}", ex.getMessage(), ex);
            // Keep UX consistent even on errors
            populateModelFallback(model, safePage, safePageSize);
        }

        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
        return ControllerSettings.VIEW_ERROR_LIST;
    }

    private void populateModelFromDto(Model model, AuthorErrorFilterDto dto, int page, int pageSize) {
        model.addAttribute(ControllerSettings.ATTR_ERRORS, dto.getAuthorErrors());
        model.addAttribute(ControllerSettings.ATTR_COURSES, dto.getCourses());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, dto.getCourse());
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, dto.getAuthorNames());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR, dto.getAuthorName());
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, dto.getTotalPages() != null ? dto.getTotalPages() : 1);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, dto.getPageSize() != null ? dto.getPageSize() : pageSize);
    }

    private void populateModelFallback(Model model, int page, int pageSize) {
        model.addAttribute(ControllerSettings.ATTR_ERRORS, null);
        model.addAttribute(ControllerSettings.ATTR_COURSES, null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, null);
        model.addAttribute(ControllerSettings.ATTR_AUTHORS, null);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR, null);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 1);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
    }
}
