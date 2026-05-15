package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankStatisticsDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thymeleaf controller for QuestionBank management operations.
 * Handles QuestionBank listing, filtering, viewing details, and exporting QuestionBank data.
 * Provides server-side rendering for QuestionBank-related pages.
 */
@Controller
@RequestMapping({"/question-banks"})
public class ThyQuestionBankController {
    private static final Logger log = LoggerFactory.getLogger(ThyQuestionBankController.class);
    private static final String ATTR_COURSES = "courses";
    private static final String ATTR_AUTHOR_STATS = "authorStats";
    private static final String QUESTION_BANK_STATISTICS = "question-bank-statistics";
    private static final String MESSAGE_SESSION_EXPIRED = "Session expired. Please log in again.";
    private static final String PREFIX_QUESTION_BANK_FILE = "questionBank_";
    private static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment; filename=";
    private static final String ERROR_QUESTION_BANK_NOT_FOUND = "Question bank not found";
    private static final String ERROR_ACCESS_DENIED = "Access denied";
    private static final String ERROR_EXPORT_FAILED = "Export failed";
    private static final String ERROR_LOAD_QUESTION_BANKS = "Could not load question banks. Please try again later.";

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyQuestionBankController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Helper method to fetch all courses from the API for dropdown population
     */
    private List<CourseDto> fetchCoursesFromAPI() {
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            // Remove trailing slash from API_COURSES constant for proper URL construction
            String coursesUrl = apiBaseUrl + "/courses";
            ResponseEntity<List<CourseDto>> coursesResponse = restTemplate.exchange(
                    coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                    }
            );
            List<CourseDto> courses = coursesResponse.getBody();
            return courses != null ? courses : new ArrayList<>();
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("Access denied when fetching courses (403)");
            return new ArrayList<>();
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().log("Courses endpoint not found (404)");
            return new ArrayList<>();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Failed to fetch courses: {}");
            return new ArrayList<>();
        }
    }

    private String renderQuestionBankList(Model model, Integer page, Integer pageSize, Long courseId) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            return redirect;
        }

        Object loggedInUser = sessionService.getLoggedInUser();
        String endpoint = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_FILTER;
        log.atDebug().addArgument(apiBaseUrl).addArgument(endpoint).log("[ThyQuestionBankController] Using apiBaseUrl='{}', resolved endpoint='{}'");

        int safePage = page != null && page > 0 ? page : 1;
        int safePageSize = pageSize != null && pageSize > 0 ? pageSize : ControllerSettings.PAGE_SIZE;

        QuestionBankFilterRequestDto filterInput = new QuestionBankFilterRequestDto();
        filterInput.setPage(safePage);
        filterInput.setPageSize(safePageSize);
        filterInput.setCourseId(courseId);

        HttpEntity<QuestionBankFilterRequestDto> entity = sessionService.createAuthorizedRequest(filterInput);

        try {
            ResponseEntity<QuestionBankFilterResponseDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, QuestionBankFilterResponseDto.class);
            QuestionBankFilterResponseDto result = response.getBody();
            if (result == null) result = new QuestionBankFilterResponseDto();

            // Courses are now included in the QuestionBankFilterResponseDto response from the backend
            List<CourseDto> courses = result.getCourses();
            if (courses == null) courses = new ArrayList<>();

            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, result);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            model.addAttribute(ControllerSettings.ATTR_JWT_TOKEN_PRESENT, true);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ATTR_COURSES, courses);
            return ControllerSettings.VIEW_QUESTION_BANK_LIST;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.atError().addArgument(endpoint).log("[TheQuestionBankController] 401 Unauthorized when calling {}: Token may be invalid or expired");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, MESSAGE_SESSION_EXPIRED);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(endpoint).log("[TheQuestionBankController] 403 Forbidden when calling {}: Token may be invalid or expired");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, MESSAGE_SESSION_EXPIRED);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.BadRequest e) {
            log.atWarn().addArgument(endpoint).setCause(e).log("[TheQuestionBankController] 400 Bad Request when calling {}. Retrying with safe default filter.");
            try {
                QuestionBankFilterRequestDto defaultFilter = new QuestionBankFilterRequestDto();
                defaultFilter.setPage(1);
                defaultFilter.setPageSize(ControllerSettings.PAGE_SIZE);
                defaultFilter.setCourseId(null);

                HttpEntity<QuestionBankFilterRequestDto> defaultEntity = sessionService.createAuthorizedRequest(defaultFilter);
                ResponseEntity<QuestionBankFilterResponseDto> retryResponse = restTemplate.exchange(endpoint, HttpMethod.POST, defaultEntity, QuestionBankFilterResponseDto.class);
                QuestionBankFilterResponseDto retryResult = retryResponse.getBody();
                if (retryResult == null) {
                    retryResult = new QuestionBankFilterResponseDto();
                }

                List<CourseDto> courses = retryResult.getCourses();
                if (courses == null) {
                    courses = fetchCoursesFromAPI();
                }

                model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, retryResult);
                model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
                model.addAttribute(ControllerSettings.ATTR_JWT_TOKEN_PRESENT, true);
                model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, null);
                model.addAttribute(ATTR_COURSES, courses != null ? courses : new ArrayList<>());
                return ControllerSettings.VIEW_QUESTION_BANK_LIST;
            } catch (Exception retryEx) {
                log.atError().setCause(retryEx).addArgument(endpoint).log("[TheQuestionBankController] Retry with safe default filter failed for endpoint {}.");
            }
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(endpoint).addArgument(e.getMessage()).log("[TheQuestionBankController] Failed to fetch filtered questionBanks from {}: {}");
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ERROR_LOAD_QUESTION_BANKS);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            model.addAttribute(ControllerSettings.ATTR_JWT_TOKEN_PRESENT, true);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ATTR_COURSES, fetchCoursesFromAPI());
            return ControllerSettings.VIEW_QUESTION_BANK_LIST;
        }

        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
        model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ERROR_LOAD_QUESTION_BANKS);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
        model.addAttribute(ControllerSettings.ATTR_JWT_TOKEN_PRESENT, true);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ATTR_COURSES, fetchCoursesFromAPI());
        return ControllerSettings.VIEW_QUESTION_BANK_LIST;
    }

    @GetMapping({"/", ""})
    public String listAllQuestionBanks(@RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                       @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                       @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId, Model model) {
        return renderQuestionBankList(model, page, pageSize, courseId);
    }

    @GetMapping("/course/id/{courseId}")
    public String getQuestionBanksByCourseId(@PathVariable Long courseId, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                             @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        return renderQuestionBankList(model, page, pageSize, courseId);
    }

    @GetMapping("/course/name/{courseName}")
    public String getQuestionBanksByCourseName(@PathVariable String courseName,
                                               @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                               Model model) {
        Long resolvedCourseId = fetchCoursesFromAPI().stream()
                .filter(c -> c.getId() != null && c.getCourse() != null && c.getCourse().trim().equalsIgnoreCase(courseName != null ? courseName.trim() : ""))
                .map(CourseDto::getId)
                .findFirst()
                .orElse(null);

        if (resolvedCourseId == null && courseName != null && !courseName.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Course '" + courseName + "' was not found. Showing all question banks.");
        }
        return renderQuestionBankList(model, page, pageSize, resolvedCourseId);
    }

    @GetMapping("/{id}")
    public String getQuestionBankById(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                      @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                      @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId, Model model, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String questionBankUrl = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id;
        try {
            ResponseEntity<QuestionBankDto> response = restTemplate.exchange(questionBankUrl, HttpMethod.GET, entity, QuestionBankDto.class);
            QuestionBankDto questionBankDto = response.getBody();
            if (questionBankDto == null || questionBankDto.getId() == null) {
                log.atWarn().addArgument(id).addArgument(questionBankUrl).log("QuestionBank details payload missing for id {} from {}");
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "QuestionBank details are not available right now. Please refresh and try again.");
                return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
            }
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, questionBankDto);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTION_BANK_URL, buildQuestionBankListBackUrl(page, pageSize, courseId));
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_QUESTION_BANK_DETAILS;
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found (404)");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "QuestionBank not found. It may have been deleted.");
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden to questionBank with id {} (403)");
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, MESSAGE_SESSION_EXPIRED);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to fetch questionBank with id {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load questionBank. Please try again later.");
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        }
    }

    @GetMapping("/{id}/extended")
    public String getQuestionBankExtendedById(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                              @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                              @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId, Model model, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String questionBankUrl = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/extended";
        try {
            ResponseEntity<QuestionBankExportDto> response = restTemplate.exchange(questionBankUrl, HttpMethod.GET, entity, QuestionBankExportDto.class);
            QuestionBankExportDto questionBankExtended = response.getBody();
            if (questionBankExtended == null || questionBankExtended.getQuestionBank() == null || questionBankExtended.getQuestionBank().getId() == null) {
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "QuestionBank export view is not available right now. Please refresh and try again.");
                return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
            }
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_EXTENDED, questionBankExtended);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, questionBankExtended.getQuestionBank());
            model.addAttribute(ControllerSettings.ATTR_AUTHOR_SECTIONS, questionBankExtended.getAuthorSections());
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTION_BANK_URL, buildQuestionBankListBackUrl(page, pageSize, courseId));
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return "question-bank-extended-details";
        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "QuestionBank not found. It may have been deleted.");
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, MESSAGE_SESSION_EXPIRED);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to fetch extended questionBank with id {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load the export view. Please try again later.");
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        }
    }

    private String buildQuestionBankListBackUrl(Integer page, Integer pageSize, Long courseId) {
        StringBuilder url = new StringBuilder("/question-banks");
        String separator = "?";

        if (courseId != null && courseId > 0) {
            url.append(separator).append(ControllerSettings.ATTR_COURSE_ID).append("=").append(courseId);
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

    @PostMapping("")
    public String createQuestionBank(@ModelAttribute QuestionBankDto questionBankDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<QuestionBankDto> entity = sessionService.createAuthorizedRequest(questionBankDto);
        try {
            restTemplate.postForEntity(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS, entity, QuestionBankDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Question Bank created successfully");
        } catch (Exception e) {
            log.atError().setCause(e).log("Failed to create questionBank");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not create question bank. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @GetMapping("/edit/{id}")
    public String editQuestionBankForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        // Fetch all courses for dropdown
        List<CourseDto> allCourses = fetchCoursesFromAPI();
        model.addAttribute(ControllerSettings.ATTR_COURSES, allCourses);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            ResponseEntity<QuestionBankDto> response = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id, HttpMethod.GET, entity, QuestionBankDto.class);
            QuestionBankDto questionBankDto = response.getBody();
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, questionBankDto);
            return ControllerSettings.QUESTION_BANK_EDITOR;
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found (404) during edit");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Question Bank not found. It may have been deleted.");
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden to questionBank with id {} (403) during edit");
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, MESSAGE_SESSION_EXPIRED);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to fetch questionBank for edit with id {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load questionBank for editing. Please try again later.");
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        }
    }

    @PostMapping("/edit/{id}")
    public String updateQuestionBank(@PathVariable Long id, @ModelAttribute QuestionBankDto questionBankDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<QuestionBankDto> entity = sessionService.createAuthorizedRequest(questionBankDto);
        try {
            restTemplate.put(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id, entity);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Question Bank updated successfully");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to update questionBank with id {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not update question bank. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @PostMapping("/delete/{id}")
    public String deleteQuestionBank(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!sessionService.containsValidVars()) {
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Question bank deleted successfully!");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to delete questionBank with id {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not delete question bank. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @PostMapping("/save")
    public String saveQuestionBank(@ModelAttribute QuestionBankDto questionBankDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<QuestionBankDto> entity = sessionService.createAuthorizedRequest(questionBankDto);
        try {
            if (questionBankDto.getId() == null) {
                restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS, HttpMethod.POST, entity, QuestionBankDto.class);
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Question Bank created successfully");
            } else {
                restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + questionBankDto.getId(), HttpMethod.PUT, entity, Void.class);
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Question Bank updated successfully");
            }
        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(ex.getMessage()).log("Failed to save questionBank: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not save question bank. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @GetMapping("/new")
    public String newQuestionBank(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        // Fetch all courses for dropdown
        List<CourseDto> allCourses = fetchCoursesFromAPI();
        model.addAttribute(ControllerSettings.ATTR_COURSES, allCourses);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
        return ControllerSettings.QUESTION_BANK_EDITOR;
    }

    @GetMapping("/{id}/export-mc")
    public void exportQuestionBankMC(@PathVariable Long id, HttpServletResponse response) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/export-mc";
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("text/csv; charset=UTF-8");
            String filename = PREFIX_QUESTION_BANK_FILE + id + "_mc.csv";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_ATTACHMENT + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for MC export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ERROR_QUESTION_BANK_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for MC export of questionBank {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ERROR_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export MC questionBank {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_EXPORT_FAILED);
        }
    }

    @GetMapping("/{id}/export-tf")
    public void exportQuestionBankTrueFalse(@PathVariable Long id, HttpServletResponse response) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/export-tf";
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("text/csv; charset=UTF-8");
            String filename = PREFIX_QUESTION_BANK_FILE + id + "_tf.csv";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_ATTACHMENT + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for TF export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ERROR_QUESTION_BANK_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for TF export of questionBank {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ERROR_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export TF questionBank {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_EXPORT_FAILED);
        }
    }

    @GetMapping("/{id}/export-xml")
    public void exportQuestionBankXml(@PathVariable Long id, HttpServletResponse response) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/export-xml";
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("application/xml; charset=UTF-8");
            String filename = PREFIX_QUESTION_BANK_FILE + id + ".xml";
            String apiDisposition = apiResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, apiDisposition != null ? apiDisposition : CONTENT_DISPOSITION_ATTACHMENT + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for XML export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ERROR_QUESTION_BANK_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for XML export of questionBank {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ERROR_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export XML for questionBank {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ERROR_EXPORT_FAILED);
        }
    }


    @GetMapping("/{id}/delete")
    public String getDeleteQuestionBank(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!sessionService.containsValidVars()) {
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Question bank deleted successfully!");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to delete questionBank with id {} via GET");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not delete question bank. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @GetMapping("/{id}/statistics")
    public String questionBankStatistics(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS + "/" + id + "/statistics";
        try {
            ResponseEntity<QuestionBankStatisticsDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, QuestionBankStatisticsDto.class);
            QuestionBankStatisticsDto stats = response.getBody();
            if (stats != null) {
                model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, stats.getQuestionBank());
                model.addAttribute(ATTR_AUTHOR_STATS, stats.getAuthorStats());
            } else {
                model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
                model.addAttribute(ATTR_AUTHOR_STATS, new ArrayList<>());
            }
            return QUESTION_BANK_STATISTICS;
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for statistics (404)");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "QuestionBank not found.");
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
            model.addAttribute(ATTR_AUTHOR_STATS, new ArrayList<>());
            return QUESTION_BANK_STATISTICS;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden to statistics for questionBank {} (403)");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Access denied.");
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
            model.addAttribute(ATTR_AUTHOR_STATS, new ArrayList<>());
            return QUESTION_BANK_STATISTICS;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to fetch statistics for questionBank {}: {}");
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
            model.addAttribute(ATTR_AUTHOR_STATS, new ArrayList<>());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load statistics. Please try again later.");
            return QUESTION_BANK_STATISTICS;
        }
    }
}

