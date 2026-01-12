package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.dto.QuizFilterDto;
import com.unitbv.myquiz.api.dto.QuizFilterInputDto;
import com.unitbv.myquiz.api.dto.QuizStatisticsDto;
import com.unitbv.myquiz.thy.service.SessionService;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
 * Thymeleaf controller for Quiz management operations.
 * Handles quiz listing, filtering, viewing details, and exporting quiz data.
 * Provides server-side rendering for quiz-related pages.
 */
@Controller
@RequestMapping("/quiz")
public class ThyQuizController {
    private static final Logger log = LoggerFactory.getLogger(ThyQuizController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyQuizController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Helper method to fetch all courses from the API for dropdown population
     */
    private List<CourseDto> fetchCoursesFromAPI() {
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String coursesUrl = apiBaseUrl + ControllerSettings.API_COURSES;
            ResponseEntity<List<CourseDto>> coursesResponse = restTemplate.exchange(
                    coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<List<CourseDto>>() {}
            );
            List<CourseDto> courses = coursesResponse.getBody();
            return courses != null ? courses : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch courses: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String renderQuizList(Model model, Integer page, Integer pageSize, String course) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            model.addAttribute(ControllerSettings.ATTR_QUIZ_FILTER, new QuizFilterDto());
            return redirect;
        }

        Object loggedInUser = sessionService.getLoggedInUser();
        String endpoint = apiBaseUrl + "/quizzes/filter";
        log.debug("[ThyQuizController] Using apiBaseUrl='{}', resolved endpoint='{}'", apiBaseUrl, endpoint);

        QuizFilterInputDto filterInput = new QuizFilterInputDto();
        filterInput.setPage(page != null && page > 0 ? page : 1);
        filterInput.setPageSize(pageSize != null ? pageSize : ControllerSettings.PAGE_SIZE);
        filterInput.setCourse(course);

        HttpEntity<QuizFilterInputDto> entity = sessionService.createAuthorizedRequest(filterInput);

        try {
            ResponseEntity<QuizFilterDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, QuizFilterDto.class);
            QuizFilterDto result = response.getBody();
            if (result == null) result = new QuizFilterDto();

            // Courses are now included in the QuizFilterDto response from the backend
            List<CourseDto> courses = result.getCourses();
            if (courses == null) courses = new ArrayList<>();

            model.addAttribute(ControllerSettings.ATTR_QUIZ_FILTER, result);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            model.addAttribute("jwtTokenPresent", true);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, course);
            model.addAttribute("courses", courses);
            return ControllerSettings.VIEW_QUIZ_LIST;
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("[ThyQuizController] 403 Forbidden when calling {}: Token may be invalid or expired", endpoint);
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_QUIZ_FILTER, new QuizFilterDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.error("[ThyQuizController] Failed to fetch filtered quizzes from {}: {}", endpoint, e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_QUIZ_FILTER, new QuizFilterDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load quizzes. Please try again later.");
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            model.addAttribute("jwtTokenPresent", true);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE, course);
            model.addAttribute("courses", new ArrayList<>());
            return ControllerSettings.VIEW_QUIZ_LIST;
        }
    }

    @GetMapping({"/", ""})
    public String listAllQuizzes(@RequestParam(value = "page", required = false) Integer page,
                                 @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                 @RequestParam(value = "course", required = false) String course,
                                 Model model) {
        return renderQuizList(model, page, pageSize, course);
    }

    @GetMapping("/course/name/{courseName}")
    public String getQuizzesByCourse(@PathVariable String courseName,
                                     @RequestParam(value = "page", required = false) Integer page,
                                     @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                     Model model) {
        return renderQuizList(model, page, pageSize, courseName);
    }

    @GetMapping("/{id}")
    public String getQuizById(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            ResponseEntity<QuizDto> response = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id, HttpMethod.GET, entity, QuizDto.class);
            QuizDto quiz = response.getBody();
            if (quiz == null || quiz.getId() == null) {
                log.warn("Quiz data is missing or incomplete for id {}", id);
                model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Quiz data not found or incomplete.");
                quiz = new QuizDto();
            }
            model.addAttribute(ControllerSettings.ATTR_QUIZ, quiz);
            return ControllerSettings.VIEW_QUIZ_DETAILS;
        } catch (Exception e) {
            log.error("Failed to fetch quiz with id {}: {}", id, e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load quiz. Please try again later.");
            model.addAttribute(ControllerSettings.ATTR_QUIZ, new QuizDto());
            return ControllerSettings.VIEW_QUIZ_DETAILS;
        }
    }

    @PostMapping("")
    public String createQuiz(@ModelAttribute QuizDto quizDto, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<QuizDto> entity = sessionService.createAuthorizedRequest(quizDto);
        try {
            restTemplate.postForEntity(apiBaseUrl + ControllerSettings.API_QUIZZES, entity, QuizDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz created successfully");
        } catch (Exception e) {
            log.error("Failed to create quiz", e);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not create quiz. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUIZ;
    }

    @GetMapping("/edit/{id}")
    public String editQuizForm(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        // Fetch all courses for dropdown
        List<CourseDto> allCourses = fetchCoursesFromAPI();
        model.addAttribute("courses", allCourses);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            ResponseEntity<QuizDto> response = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id, HttpMethod.GET, entity, QuizDto.class);
            QuizDto quiz = response.getBody();
            model.addAttribute(ControllerSettings.ATTR_QUIZ, quiz);
            return ControllerSettings.VIEW_QUIZ_EDITOR;
        } catch (Exception e) {
            log.error("Failed to fetch quiz for edit with id {}", id, e);
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load quiz for editing. Please try again later.");
            model.addAttribute(ControllerSettings.ATTR_QUIZ, new QuizDto());
            return ControllerSettings.VIEW_QUIZ_EDITOR;
        }
    }

    @PostMapping("/edit/{id}")
    public String updateQuiz(@PathVariable Long id, @ModelAttribute QuizDto quizDto, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<QuizDto> entity = sessionService.createAuthorizedRequest(quizDto);
        try {
            restTemplate.put(apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id, entity);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz updated successfully");
        } catch (Exception e) {
            log.error("Failed to update quiz with id {}", id, e);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not update quiz. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUIZ;
    }

    @PostMapping("/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!sessionService.containsValidVars()) {
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete quiz with id {}", id, e);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not delete quiz. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUIZ;
    }

    @PostMapping("/save")
    public String saveQuiz(@ModelAttribute QuizDto quizDto, RedirectAttributes redirectAttributes, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<QuizDto> entity = sessionService.createAuthorizedRequest(quizDto);
        try {
            if (quizDto.getId() == null) {
                restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES, HttpMethod.POST, entity, QuizDto.class);
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz created successfully");
            } else {
                restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + quizDto.getId(), HttpMethod.PUT, entity, Void.class);
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz updated successfully");
            }
        } catch (Exception ex) {
            log.error("Failed to save quiz: {}", ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not save quiz. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUIZ;
    }

    @GetMapping("/new")
    public String newQuiz(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        // Fetch all courses for dropdown
        List<CourseDto> allCourses = fetchCoursesFromAPI();
        model.addAttribute("courses", allCourses);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        model.addAttribute(ControllerSettings.ATTR_QUIZ, new QuizDto());
        return ControllerSettings.VIEW_QUIZ_EDITOR;
    }

    @GetMapping("/{id}/export-mc")
    public void exportQuizMC(@PathVariable Long id, HttpServletResponse response, HttpSession session) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id + "/export-mc";
        ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        response.setContentType("text/csv; charset=UTF-8");
        String filename = "quiz_" + id + "_mc.csv";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        byte[] body = apiResponse.getBody();
        if (body != null) {
            StreamUtils.copy(body, response.getOutputStream());
        }
    }

    @GetMapping("/{id}/export-tf")
    public void exportQuizTRUEFALSE(@PathVariable Long id, HttpServletResponse response, HttpSession session) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id + "/export-tf";
        ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        response.setContentType("text/csv; charset=UTF-8");
        String filename = "quiz_" + id + "_tf.csv";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        byte[] body = apiResponse.getBody();
        if (body != null) {
            StreamUtils.copy(body, response.getOutputStream());
        }
    }


    @GetMapping("/{id}/delete")
    public String getDeleteQuiz(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!sessionService.containsValidVars()) {
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUIZZES_SLASH + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete quiz with id {} via GET", id, e);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not delete quiz. Please try again later.");
        }
        return ControllerSettings.VIEW_REDIRECT_QUIZ;
    }

    @GetMapping("/{id}/statistics")
    public String quizStatistics(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUIZZES + "/" + id + "/statistics";
        try {
            ResponseEntity<QuizStatisticsDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, QuizStatisticsDto.class);
            QuizStatisticsDto stats = response.getBody();
            model.addAttribute("quiz", stats == null ? new QuizDto() : stats.getQuiz());
            model.addAttribute("authorStats", stats.getAuthorStats());
            return "quiz-statistics";
        } catch (Exception e) {
            log.error("Failed to fetch statistics for quiz {}: {}", id, e.getMessage(), e);
            model.addAttribute("quiz", new QuizDto());
            model.addAttribute("authorStats", new java.util.ArrayList<>());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not load statistics. Please try again later.");
            return "quiz-statistics";
        }
    }
}
