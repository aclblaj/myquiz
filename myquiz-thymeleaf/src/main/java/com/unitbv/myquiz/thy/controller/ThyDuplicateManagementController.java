package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.dto.DuplicateRecomputeHistoryDto;
import com.unitbv.myquiz.api.dto.DuplicateStatisticsDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankInfo;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.DuplicateComparisonStrategy;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Thymeleaf controller for duplicate management operations.
 * Handles duplicate comparison, statistics display, clearing, and recompute history.
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
            loadHistory(model);
            return ControllerSettings.VIEW_DUPLICATE_RECOMPUTE;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).log("Error loading duplicate management page");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_LOADING_PAGE_RETRY);
            populateFilterModelDefaults(model, courseId, questionBankId, authorId);
            return ControllerSettings.VIEW_DUPLICATE_RECOMPUTE;
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
            } else if ("recompute".equals(action)) {
                handleRecomputeAction(courseId, strategy, questionBankId, authorId, model);
            } else if ("statistics".equals(action)) {
                handleStatisticsAction(courseId, questionBankId, authorId, model);
            } else if ("clear".equals(action)) {
                handleClearAction(courseId, questionBankId, authorId, model);
            }

            loadHistory(model);
            return ControllerSettings.VIEW_DUPLICATE_RECOMPUTE;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).addArgument(action).log("Error processing {} action for courseId '{}'");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_PROCESSING_ACTION_RETRY);
            populateFilterModelDefaults(model, courseId, questionBankId, authorId);
            return ControllerSettings.VIEW_DUPLICATE_RECOMPUTE;
        }
    }

    /**
     * Saves the current recompute result to history.
     * The result fields are passed as hidden form fields from the results panel.
     */
    @PostMapping("/history/save")
    public String saveToHistory(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId,
            @RequestParam String strategy,
            @RequestParam(defaultValue = "0") int totalQuestions,
            @RequestParam(defaultValue = "0") int multichoiceQuestions,
            @RequestParam(defaultValue = "0") int truefalseQuestions,
            @RequestParam(defaultValue = "0") int duplicateLinksRemoved,
            @RequestParam(defaultValue = "0") int duplicateErrorsRemoved,
            @RequestParam(defaultValue = "0") int duplicateErrorsCreated,
            @RequestParam(required = false) String startedAt,
            @RequestParam(required = false) String endedAt,
            @RequestParam(defaultValue = "0") long durationMs,
            RedirectAttributes redirectAttributes) {

        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            DuplicateRecomputeHistoryDto dto = new DuplicateRecomputeHistoryDto();
            dto.setCourseId(courseId);
            dto.setCourseName(courseName);
            dto.setQuestionBankId(questionBankId);
            dto.setAuthorId(authorId);
            dto.setStrategy(strategy);
            dto.setTotalQuestions(totalQuestions);
            dto.setMultichoiceQuestions(multichoiceQuestions);
            dto.setTruefalseQuestions(truefalseQuestions);
            dto.setDuplicateLinksRemoved(duplicateLinksRemoved);
            dto.setDuplicateErrorsRemoved(duplicateErrorsRemoved);
            dto.setDuplicateErrorsCreated(duplicateErrorsCreated);
            dto.setDurationMs(durationMs);

            HttpEntity<DuplicateRecomputeHistoryDto> entity = sessionService.createAuthorizedRequest(dto);
            String endpoint = getCoursesApiBaseUrl() + ControllerSettings.API_COURSES_RECOMPUTE_HISTORY_SUFFIX;
            restTemplate.exchange(endpoint, HttpMethod.POST, entity, DuplicateRecomputeHistoryDto.class);

            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_RECOMPUTE_SAVE_HISTORY_SUCCESS);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).log("Error saving recompute history entry");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_RECOMPUTE_SAVE_HISTORY_FAILED);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(ControllerSettings.PATH_DUPLICATE_MANAGEMENT);
        if (courseId != null) builder.queryParam(ControllerSettings.ATTR_COURSE_ID, courseId);
        if (questionBankId != null) builder.queryParam(ControllerSettings.ATTR_QUESTION_BANK_ID, questionBankId);
        if (authorId != null) builder.queryParam(ControllerSettings.ATTR_AUTHOR_ID, authorId);
        return "redirect:" + builder.toUriString();
    }

    /**
     * Deletes a recompute history entry by ID.
     */
    @PostMapping("/history/{id}/delete")
    public String deleteHistoryEntry(
            @PathVariable Long id,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long authorId,
            RedirectAttributes redirectAttributes) {

        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String endpoint = getCoursesApiBaseUrl() + ControllerSettings.API_COURSES_RECOMPUTE_HISTORY_SUFFIX + "/" + id;
            restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_HISTORY_ENTRY_DELETED_SUCCESS);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_HISTORY_ENTRY_NOT_FOUND);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error deleting recompute history entry id='{}'");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_HISTORY_ENTRY_DELETE_FAILED);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(ControllerSettings.PATH_DUPLICATE_MANAGEMENT);
        if (courseId != null) builder.queryParam(ControllerSettings.ATTR_COURSE_ID, courseId);
        if (questionBankId != null) builder.queryParam(ControllerSettings.ATTR_QUESTION_BANK_ID, questionBankId);
        if (authorId != null) builder.queryParam(ControllerSettings.ATTR_AUTHOR_ID, authorId);
        return "redirect:" + builder.toUriString();
    }

    // ---- Filter model population ----

    private void populateFilterModel(Model model, Long courseId, Long questionBankId, Long authorId) {
        List<CourseDto> courses = loadCourses();
        model.addAttribute(ControllerSettings.ATTR_COURSES, courses);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_STRATEGY, DuplicateComparisonStrategy.STRING_EQUALITY.getAlgorithmName());
        model.addAttribute(ControllerSettings.ATTR_AVAILABLE_STRATEGIES, DuplicateComparisonStrategy.values());

        List<QuestionBankInfo> questionBanks = new ArrayList<>();
        if (courseId != null) {
            questionBanks = loadQuestionBanksForCourse(courseId);
        }
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, questionBanks);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);

        List<AuthorDto> authors = new ArrayList<>();
        if (questionBankId != null) {
            authors = loadAuthorsForQuestionBank(questionBankId, courseId);
        }
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_AUTHORS, authors);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
    }

    private void populateFilterModelDefaults(Model model, Long courseId, Long questionBankId, Long authorId) {
        model.addAttribute(ControllerSettings.ATTR_COURSES, List.of());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_STRATEGY, DuplicateComparisonStrategy.STRING_EQUALITY.getAlgorithmName());
        model.addAttribute(ControllerSettings.ATTR_AVAILABLE_STRATEGIES, DuplicateComparisonStrategy.values());
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANKS, List.of());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_QUESTION_BANK_ID, questionBankId);
        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_AUTHORS, List.of());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_AUTHOR_ID, authorId);
    }

    private void loadHistory(Model model) {
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            String endpoint = getCoursesApiBaseUrl() + ControllerSettings.API_COURSES_RECOMPUTE_HISTORY_SUFFIX;
            ResponseEntity<DuplicateRecomputeHistoryDto[]> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, entity, DuplicateRecomputeHistoryDto[].class);
            DuplicateRecomputeHistoryDto[] arr = response.getBody();
            model.addAttribute(ControllerSettings.ATTR_RECOMPUTE_HISTORY, arr != null ? Arrays.asList(arr) : List.of());
        } catch (Exception e) {
            log.atWarn().setCause(e).log("Could not load recompute history");
            model.addAttribute(ControllerSettings.ATTR_RECOMPUTE_HISTORY, List.of());
        }
    }

    // ---- Action handlers ----

    private void handleRecomputeAction(Long courseId, String strategy, Long questionBankId, Long authorId, Model model) {
        log.atInfo().addArgument(courseId).addArgument(questionBankId).addArgument(authorId).addArgument(strategy)
                .log("Starting duplicate recompute for courseId='{}', qbId={}, authorId={} with strategy '{}'");
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCoursesApiBaseUrl())
                    .path(ControllerSettings.API_COURSES_RECOMPUTE_WITH_STRATEGY_SUFFIX)
                    .queryParam("strategy", strategy);
            appendScopeParams(builder, courseId, questionBankId, authorId);
            String endpoint = builder.toUriString();

            ResponseEntity<CourseDuplicateRecomputeResultDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, CourseDuplicateRecomputeResultDto.class);
            CourseDuplicateRecomputeResultDto result = response.getBody();

            if (result != null) {
                model.addAttribute(ControllerSettings.ATTR_RECOMPUTE_RESULT, result);
                model.addAttribute(ControllerSettings.ATTR_PENDING_STRATEGY, strategy);
                model.addAttribute(ControllerSettings.ATTR_PENDING_COURSE_ID, courseId);
                model.addAttribute(ControllerSettings.ATTR_PENDING_QUESTION_BANK_ID, questionBankId);
                model.addAttribute(ControllerSettings.ATTR_PENDING_AUTHOR_ID, authorId);
                model.addAttribute(ControllerSettings.ATTR_MESSAGE, buildRecomputeMessage(result));
            } else {
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_FAILED_RECOMPUTE_DUPLICATES);
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error recomputing duplicates for courseId '{}'");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_PROCESSING_ACTION_RETRY);
        }
    }

    private void handleStatisticsAction(Long courseId, Long questionBankId, Long authorId, Model model) {
        log.atInfo().addArgument(courseId).addArgument(questionBankId).addArgument(authorId)
                .log("Fetching duplicate statistics for courseId='{}', qbId={}, authorId={}");
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCoursesApiBaseUrl())
                    .path(ControllerSettings.API_COURSES_DUPLICATE_STATISTICS_SUFFIX);
            appendScopeParams(builder, courseId, questionBankId, authorId);
            String endpoint = builder.toUriString();

            ResponseEntity<DuplicateStatisticsDto> response = restTemplate.exchange(endpoint, HttpMethod.GET, entity, DuplicateStatisticsDto.class);
            DuplicateStatisticsDto statistics = response.getBody();

            if (statistics != null) {
                model.addAttribute(ControllerSettings.ATTR_STATISTICS, statistics);
                model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_STATISTICS_RETRIEVED_FOR_PREFIX + resolveScopeLabel(courseId, questionBankId, authorId));
            } else {
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_FAILED_RETRIEVE_STATISTICS);
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error retrieving statistics for courseId '{}'");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_RETRIEVING_STATISTICS);
        }
    }

    private void handleClearAction(Long courseId, Long questionBankId, Long authorId, Model model) {
        log.atInfo().addArgument(courseId).addArgument(questionBankId).addArgument(authorId)
                .log("Clearing duplicate links for courseId='{}', qbId={}, authorId={}");
        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCoursesApiBaseUrl())
                    .path(ControllerSettings.API_COURSES_CLEAR_DUPLICATES_SUFFIX);
            appendScopeParams(builder, courseId, questionBankId, authorId);
            String endpoint = builder.toUriString();

            ResponseEntity<Integer> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Integer.class);
            Integer clearedCount = response.getBody();

            if (clearedCount != null && clearedCount >= 0) {
                model.addAttribute(ControllerSettings.ATTR_CLEAR_RESULT, clearedCount);
                model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_SUCCESS_CLEARED_PREFIX + clearedCount + ControllerSettings.MSG_DUPLICATE_LINKS_FOR_PREFIX + resolveScopeLabel(courseId, questionBankId, authorId));
            } else {
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_FAILED_CLEAR_DUPLICATES);
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(courseId).log("Error clearing duplicates for courseId '{}'");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_ERROR_CLEARING_DUPLICATES);
        }
    }

    // ---- Helper methods ----

    private void appendScopeParams(UriComponentsBuilder builder, Long courseId, Long questionBankId, Long authorId) {
        if (questionBankId != null) {
            builder.queryParam(ControllerSettings.ATTR_QUESTION_BANK_ID, questionBankId);
        }
        if (authorId != null) {
            builder.queryParam(ControllerSettings.ATTR_AUTHOR_ID, authorId);
        }
        if (questionBankId == null && courseId != null) {
            builder.queryParam(ControllerSettings.ATTR_COURSE_ID, courseId);
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
            String endpoint = apiBaseUrl.trim().replaceAll("/$", "") + ControllerSettings.API_AUTHORS_FILTER;
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
