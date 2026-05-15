package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankInfo;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Thymeleaf controller for duplicate management operations.
 * Handles duplicate comparison, statistics display, and clearing.
 * Supports filtering by course, question bank, or author.
 */
@Controller
@RequestMapping("/duplicate-management")
public class ThyDuplicateManagementController {

    private static final Logger log = LoggerFactory.getLogger(ThyDuplicateManagementController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyDuplicateManagementController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    @GetMapping
    public String showDuplicateManagement(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId,
            Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            populateFilterModel(model, courseId, questionBankId, authorId);
            return "duplicate-recompute";
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute("errorMessage", "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).log("Error loading duplicate management page");
            model.addAttribute("errorMessage", "Error loading page. Please try again.");
            populateFilterModelDefaults(model, courseId, questionBankId, authorId);
            return "duplicate-recompute";
        }
    }

    @PostMapping("/recompute")
    public String recomputeDuplicates(
            @RequestParam(required = false) Long courseId,
            @RequestParam String strategy,
            @RequestParam String action,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId,
            Model model) {

        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            populateFilterModel(model, courseId, questionBankId, authorId);

            if ("loadCascade".equals(action)) {
                // Just reload page with cascading filter data – already done by populateFilterModel
                return "duplicate-recompute";
            } else if ("recompute".equals(action)) {
                handleRecomputeAction(courseId, strategy, questionBankId, authorId, model);
            } else if ("statistics".equals(action)) {
                handleStatisticsAction(courseId, questionBankId, authorId, model);
            } else if ("clear".equals(action)) {
                handleClearAction(courseId, questionBankId, authorId, model);
            }

            return "duplicate-recompute";
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute("errorMessage", "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).addArgument(action).log("Error processing {} action for courseId '{}'");
            model.addAttribute("errorMessage", "Error processing action. Please try again.");
            populateFilterModelDefaults(model, courseId, questionBankId, authorId);
            return "duplicate-recompute";
        }
    }

    // ---- Filter model population ----

    private void populateFilterModel(Model model, Long courseId, Long questionBankId, Long authorId) {
        List<CourseDto> courses = loadCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("selectedStrategy", "levenshtein");

        List<QuestionBankInfo> questionBanks = new ArrayList<>();
        if (courseId != null) {
            questionBanks = loadQuestionBanksForCourse(courseId);
        }
        model.addAttribute("questionBanks", questionBanks);
        model.addAttribute("selectedQuestionBankId", questionBankId);

        List<AuthorDto> authors = new ArrayList<>();
        if (questionBankId != null) {
            authors = loadAuthorsForQuestionBank(questionBankId, courseId);
        }
        model.addAttribute("questionBankAuthors", authors);
        model.addAttribute("selectedAuthorId", authorId);
    }

    private void populateFilterModelDefaults(Model model, Long courseId, Long questionBankId, Long authorId) {
        model.addAttribute("courses", List.of());
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("selectedStrategy", "levenshtein");
        model.addAttribute("questionBanks", List.of());
        model.addAttribute("selectedQuestionBankId", questionBankId);
        model.addAttribute("questionBankAuthors", List.of());
        model.addAttribute("selectedAuthorId", authorId);
    }

    // ---- Action handlers ----

    private void handleRecomputeAction(Long courseId, String strategy, Long questionBankId, Long authorId, Model model) {
        log.atInfo().addArgument(courseId).addArgument(questionBankId).addArgument(authorId).addArgument(strategy)
                .log("Starting duplicate recompute for courseId='{}', qbId={}, authorId={} with strategy '{}'");
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCoursesApiBaseUrl())
                    .path("/recompute-with-strategy")
                    .queryParam("strategy", strategy);
            appendScopeParams(builder, courseId, questionBankId, authorId);
            String endpoint = builder.toUriString();

            ResponseEntity<CourseDuplicateRecomputeResultDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, CourseDuplicateRecomputeResultDto.class);
            CourseDuplicateRecomputeResultDto result = response.getBody();

            if (result != null) {
                model.addAttribute("recomputeResult", result);
                model.addAttribute("message", buildRecomputeMessage(result));
            } else {
                model.addAttribute("errorMessage", "Failed to recompute duplicates.");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute("errorMessage", "Session expired. Please log in again.");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error recomputing duplicates for courseId '{}'");
            model.addAttribute("errorMessage", "Error processing recompute. Please try again.");
        }
    }

    private void handleStatisticsAction(Long courseId, Long questionBankId, Long authorId, Model model) {
        log.atInfo().addArgument(courseId).addArgument(questionBankId).addArgument(authorId)
                .log("Fetching duplicate statistics for courseId='{}', qbId={}, authorId={}");
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCoursesApiBaseUrl())
                    .path("/duplicate-statistics");
            appendScopeParams(builder, courseId, questionBankId, authorId);
            String endpoint = builder.toUriString();

            ResponseEntity<DuplicateStatisticsDto> response = restTemplate.exchange(endpoint, HttpMethod.GET, entity, DuplicateStatisticsDto.class);
            DuplicateStatisticsDto statistics = response.getBody();

            if (statistics != null) {
                model.addAttribute("statistics", statistics);
                model.addAttribute("message", "Statistics retrieved for: " + resolveScopeLabel(courseId, questionBankId, authorId));
            } else {
                model.addAttribute("errorMessage", "Failed to retrieve statistics.");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute("errorMessage", "Session expired. Please log in again.");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error retrieving statistics for courseId '{}'");
            model.addAttribute("errorMessage", "Error retrieving statistics. Please try again.");
        }
    }

    private void handleClearAction(Long courseId, Long questionBankId, Long authorId, Model model) {
        log.atInfo().addArgument(courseId).addArgument(questionBankId).addArgument(authorId)
                .log("Clearing duplicate links for courseId='{}', qbId={}, authorId={}");
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCoursesApiBaseUrl())
                    .path("/clear-duplicates");
            appendScopeParams(builder, courseId, questionBankId, authorId);
            String endpoint = builder.toUriString();

            ResponseEntity<Integer> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Integer.class);
            Integer clearedCount = response.getBody();

            if (clearedCount != null && clearedCount >= 0) {
                model.addAttribute("clearResult", clearedCount);
                model.addAttribute("message", "Successfully cleared " + clearedCount + " duplicate links for: " + resolveScopeLabel(courseId, questionBankId, authorId));
            } else {
                model.addAttribute("errorMessage", "Failed to clear duplicates.");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute("errorMessage", "Session expired. Please log in again.");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error clearing duplicates for courseId '{}'");
            model.addAttribute("errorMessage", "Error clearing duplicates. Please try again.");
        }
    }

    // ---- Helper methods ----

    private void appendScopeParams(UriComponentsBuilder builder, Long courseId, Long questionBankId, Long authorId) {
        if (questionBankId != null) {
            builder.queryParam("questionBankId", questionBankId);
        }
        if (authorId != null) {
            builder.queryParam("authorId", authorId);
        }
        if (questionBankId == null && courseId != null) {
            builder.queryParam("courseId", courseId);
        }
    }

    private String resolveScopeLabel(Long courseId, Long questionBankId, Long authorId) {
        if (authorId != null && questionBankId != null) {
            return "author #" + authorId + " in question bank #" + questionBankId;
        } else if (questionBankId != null) {
            return "question bank #" + questionBankId;
        } else {
            return "course #" + courseId;
        }
    }

    private String buildRecomputeMessage(CourseDuplicateRecomputeResultDto result) {
        return "Recompute started: " + result.getStartedAt() + " | finished: " + result.getEndedAt() +
                " | duration: " + result.getDurationMs() + " ms | MC: " + result.getMultichoiceQuestions() +
                " | TF: " + result.getTruefalseQuestions() + " | links removed: " + result.getDuplicateLinksRemoved() +
                " | duplicate errors removed: " + result.getDuplicateErrorsRemoved() +
                " | duplicate errors created: " + result.getDuplicateErrorsCreated();
    }

    private List<CourseDto> loadCourses() {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        ResponseEntity<CourseDto[]> response = restTemplate.exchange(
                getCoursesApiBaseUrl(),
                HttpMethod.GET,
                entity,
                CourseDto[].class
        );
        CourseDto[] courseDtos = response.getBody();
        if (courseDtos == null) return List.of();

        return Arrays.stream(courseDtos)
                .filter(course -> course.getId() != null)
                .distinct()
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                        a.getCourse() != null ? a.getCourse() : "",
                        b.getCourse() != null ? b.getCourse() : ""
                ))
                .toList();
    }


    private List<QuestionBankInfo> loadQuestionBanksForCourse(Long courseId) {
        try {
            HttpEntity<QuestionBankFilterRequestDto> entity = sessionService.createAuthorizedRequest(
                    buildQbFilterInput(courseId));
            String endpoint = apiBaseUrl.trim().replaceAll("/$", "") + ControllerSettings.API_QUESTION_BANKS_FILTER;
            ResponseEntity<QuestionBankFilterResponseDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, QuestionBankFilterResponseDto.class);
            QuestionBankFilterResponseDto result = response.getBody();
            if (result == null || result.getQuestionBanks() == null) return List.of();
            return result.getQuestionBanks().stream()
                    .map(qb -> new QuestionBankInfo(qb.getId(), qb.getName(), qb.getCourse()))
                    .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()))
                    .toList();
        } catch (Exception e) {
            log.atWarn().setCause(e).addArgument(courseId).log("Could not load question banks for courseId '{}'");
            return List.of();
        }
    }

    private QuestionBankFilterRequestDto buildQbFilterInput(Long courseId) {
        QuestionBankFilterRequestDto input = new QuestionBankFilterRequestDto();
        if (courseId != null) {
            input.setCourseId(courseId);
        }
        input.setPage(1);
        input.setPageSize(500);
        return input;
    }

    private List<AuthorDto> loadAuthorsForQuestionBank(Long questionBankId, Long courseId) {
        try {
            AuthorFilterRequestDto filterInput = new AuthorFilterRequestDto();
            filterInput.setQuestionBankId(questionBankId);
            filterInput.setCourseId(courseId);
            filterInput.setPage(1);
            filterInput.setPageSize(500);

            HttpEntity<AuthorFilterRequestDto> entity = sessionService.createAuthorizedRequest(filterInput);
            String endpoint = apiBaseUrl.trim().replaceAll("/$", "") + ControllerSettings.API_AUTHORS + "/filter";
            ResponseEntity<AuthorFilterResponseDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, AuthorFilterResponseDto.class);
            AuthorFilterResponseDto result = response.getBody();
            if (result == null || result.getAuthors() == null) return List.of();
            return result.getAuthors().stream()
                    .filter(a -> a.getId() != null)
                    .sorted((a, b) -> {
                        String na = a.getName() != null ? a.getName() : "";
                        String nb = b.getName() != null ? b.getName() : "";
                        return String.CASE_INSENSITIVE_ORDER.compare(na, nb);
                    })
                    .toList();
        } catch (Exception e) {
            log.atWarn().setCause(e).addArgument(questionBankId).log("Could not load authors for questionBank '{}'");
            return List.of();
        }
    }

    private String getCoursesApiBaseUrl() {
        String baseUrl = apiBaseUrl != null ? apiBaseUrl.trim() : "";
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String coursePath = ControllerSettings.API_COURSES;
        if (coursePath.endsWith("/")) {
            coursePath = coursePath.substring(0, coursePath.length() - 1);
        }

        return baseUrl + coursePath;
    }
}

