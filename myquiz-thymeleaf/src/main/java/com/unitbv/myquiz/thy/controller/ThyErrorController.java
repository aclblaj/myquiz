package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.QuestionErrorFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterResponseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Thymeleaf controller for error list operations.
 * Handles error list display, filtering, and pagination from the UI perspective.
 */
@Controller
@RequestMapping(ControllerSettings.API_ERRORS)
public class ThyErrorController {

    private static final Logger log = LoggerFactory.getLogger(ThyErrorController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyErrorController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Display error list with optional filtering and pagination.
     */
    @GetMapping({"", "/"})
    public String listErrors(@RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE) Integer page,
                             @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                             @RequestParam(value = ControllerSettings.ATTR_AUTHOR, required = false) String author,
                             @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                             @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        log.info("Listing errors - page: {}, courseId: {}, author: {}, questionBankId: {}, pageSize: {}", page, courseId, author, questionBankId, pageSize);
        return renderErrorList(page, courseId, author, questionBankId, pageSize, model);
    }

    /**
     * Filter errors via POST.
     */
    @PostMapping("/filter")
    public String filterErrors(@RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE) Integer page,
                               @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                               @RequestParam(value = ControllerSettings.ATTR_AUTHOR, required = false) String author,
                               @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        log.info("Filtering errors - page: {}, courseId: {}, author: {}, questionBankId: {}, pageSize: {}", page, courseId, author, questionBankId, pageSize);
        return renderErrorList(page, courseId, author, questionBankId, pageSize, model);
    }

    /**
     * Resolve a specific error.
     */
    @PostMapping("/{id}/resolve")
    public String resolveError(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                               @RequestParam(value = ControllerSettings.ATTR_AUTHOR, required = false) String author,
                               @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, RedirectAttributes redirectAttributes) {
        log.info("Resolving error with id: {}", id);
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_ERRORS + "/" + id + "/resolve", HttpMethod.PUT, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, ControllerSettings.MSG_ERROR_RESOLVED_SUCCESS);
            log.info("Error {} resolved successfully", id);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Error not found with id: {}", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.warn("Permission denied for resolving error {}", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_RESOLVE_FORBIDDEN);
        } catch (Exception ex) {
            log.error("Error resolving error with id: {}", id, ex);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_RESOLVE_FAILED);
        }

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        return "redirect:" + buildErrorsBackUrl(courseId, author, questionBankId, pagination.page(), pagination.pageSize());
    }

    /**
     * Delete a specific error.
     */
    @PostMapping("/{id}/delete")
    public String deleteError(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                              @RequestParam(value = ControllerSettings.ATTR_AUTHOR, required = false) String author,
                              @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                              @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                              @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, RedirectAttributes redirectAttributes) {
        log.info("Deleting error with id: {}", id);
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_ERRORS + "/" + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, ControllerSettings.MSG_ERROR_DELETED_SUCCESS);
            log.info("Error {} deleted successfully", id);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Error not found with id: {}", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.warn("Permission denied for deleting error {}", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_DELETE_FORBIDDEN);
        } catch (Exception ex) {
            log.error("Error deleting error with id: {}", id, ex);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_DELETE_FAILED);
        }

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        return "redirect:" + buildErrorsBackUrl(courseId, author, questionBankId, pagination.page(), pagination.pageSize());
    }

    /**
     * Internal method to render error list with filtering and pagination.
     */
    private String renderErrorList(Integer page, Long courseId, String author, Long questionBankId, Integer pageSize, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            model.addAttribute(ControllerSettings.ATTR_QUESTION_ERRORS, new ArrayList<>());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_LOGIN_REQUIRED_TO_VIEW_ERRORS);
            return ControllerSettings.VIEW_ERROR_LIST;
        }

        // Normalize empty strings to null
        if (author != null && author.trim().isEmpty()) {
            author = null;
        }

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        int safePage = pagination.page();
        int safePageSize = pagination.pageSize();

        Object loggedInUser = sessionService.getLoggedInUser();
        try {
            String filterUrl = apiBaseUrl + ControllerSettings.API_ERRORS_FILTER;
            QuestionErrorFilterRequestDto filterInputDto = new QuestionErrorFilterRequestDto();
            filterInputDto.setSelectedCourseId(courseId);
            filterInputDto.setSelectedAuthor(author);
            filterInputDto.setSelectedQuestionBankId(questionBankId);
            filterInputDto.setPage(safePage);
            filterInputDto.setPageSize(safePageSize);

            log.info("Calling backend with filter: {}", filterInputDto);
            HttpEntity<QuestionErrorFilterRequestDto> requestEntity = sessionService.createAuthorizedRequest(filterInputDto);

            ResponseEntity<QuestionErrorFilterResponseDto> response = restTemplate.exchange(filterUrl, HttpMethod.POST, requestEntity, QuestionErrorFilterResponseDto.class);

            QuestionErrorFilterResponseDto filterDto = response.getBody();
            if (filterDto == null) {
                log.error("API returned null for QuestionErrorFilterResponseDto");
                populateErrorListModelFallback(model, safePage, safePageSize, courseId, author, questionBankId);
                return ControllerSettings.VIEW_ERROR_LIST;
            }

            populateErrorListModel(model, filterDto, safePage, safePageSize, courseId, author, questionBankId);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_ERRORS_URL, buildErrorsBackUrl(courseId, author, questionBankId, safePage, safePageSize));
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            return ControllerSettings.VIEW_ERROR_LIST;

        } catch (HttpClientErrorException.Forbidden ex) {
            log.warn("Permission denied when loading errors");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_VIEW_FORBIDDEN);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_ERRORS_URL, buildErrorsBackUrl(courseId, author, questionBankId, safePage, safePageSize));
            return ControllerSettings.VIEW_ERROR_LIST;
        } catch (Exception ex) {
            log.error("Error loading errors: {}", ex.getMessage(), ex);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_LOAD_FAILED);
            populateErrorListModelFallback(model, safePage, safePageSize, courseId, author, questionBankId);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_ERRORS_URL, buildErrorsBackUrl(courseId, author, questionBankId, safePage, safePageSize));
            return ControllerSettings.VIEW_ERROR_LIST;
        }
    }

    /**
     * Populate model with error list data.
     */
    private void populateErrorListModel(Model model, QuestionErrorFilterResponseDto filterDto, Integer page, Integer pageSize, Long courseId, String author, Long questionBankId) {
        model.addAttribute(ControllerSettings.ATTR_QUESTION_ERRORS, filterDto.getQuestionErrors() != null ? filterDto.getQuestionErrors() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_ERRORS_BY_AUTHOR, filterDto.getQuestionErrorsByAuthor());
        model.addAttribute(ControllerSettings.ATTR_COURSES, filterDto.getCourses() != null ? filterDto.getCourses() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_AUTHOR_NAMES, filterDto.getAuthorNames() != null ? filterDto.getAuthorNames() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, filterDto.getQuestionBanks() != null ? filterDto.getQuestionBanks() : new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, filterDto.getCourse());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, filterDto.getSelectedCourseId() != null ? filterDto.getSelectedCourseId() : courseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR, author);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID_ERRORS, questionBankId);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, filterDto.getPage() != null ? filterDto.getPage() : page);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, filterDto.getPageSize() != null ? filterDto.getPageSize() : pageSize);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, filterDto.getTotalPages() != null ? filterDto.getTotalPages() : 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, filterDto.getTotalElements() != null ? filterDto.getTotalElements() : 0L);
    }

    /**
     * Fallback model population when API call fails.
     */
    private void populateErrorListModelFallback(Model model, Integer page, Integer pageSize, Long courseId, String author, Long questionBankId) {
        model.addAttribute(ControllerSettings.ATTR_QUESTION_ERRORS, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_ERRORS_BY_AUTHOR, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_COURSES, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_AUTHOR_NAMES, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, new ArrayList<>());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR, author != null ? author : "");
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID_ERRORS, questionBankId);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 0);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, 0L);
    }

    private String buildErrorsBackUrl(Long courseId, String author, Long questionBankId, Integer page, Integer pageSize) {
        StringBuilder url = new StringBuilder(ControllerSettings.API_ERRORS);
        String sep = "?";
        if (courseId != null) {
            url.append(sep).append(ControllerSettings.ATTR_COURSE_ID).append("=").append(courseId);
            sep = "&";
        }
        if (author != null && !author.isBlank()) {
            url.append(sep).append(ControllerSettings.ATTR_AUTHOR).append("=").append(URLEncoder.encode(author.trim(), StandardCharsets.UTF_8));
            sep = "&";
        }
        if (questionBankId != null && questionBankId > 0) {
            url.append(sep).append(ControllerSettings.ATTR_QUESTION_BANK_ID).append("=").append(questionBankId);
            sep = "&";
        }
        if (page != null && page > 0) {
            url.append(sep).append(ControllerSettings.ATTR_PAGE_NUMBER).append("=").append(page);
            sep = "&";
        }
        if (pageSize != null && pageSize > 0) {
            url.append(sep).append(ControllerSettings.ATTR_PAGE_SIZE).append("=").append(pageSize);
        }
        return url.toString();
    }
}
