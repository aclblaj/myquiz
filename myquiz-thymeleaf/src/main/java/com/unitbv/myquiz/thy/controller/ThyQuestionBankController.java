package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportAuthorSectionDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankFilterResponseDto;
import com.unitbv.myquiz.api.dto.QuestionBankStatisticsDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationResult;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.thy.service.SessionService;
import com.unitbv.myquiz.thy.pagination.PaginationView;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thymeleaf controller for QuestionBank management operations.
 * Handles QuestionBank listing, filtering, viewing details, and exporting QuestionBank data.
 * Provides server-side rendering for QuestionBank-related pages.
 */
@Controller
@RequestMapping({"/question-banks"})
public class ThyQuestionBankController {
    private static final Logger log = LoggerFactory.getLogger(ThyQuestionBankController.class);

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

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        int safePage = pagination.page();
        int safePageSize = pagination.pageSize();

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
            model.addAttribute(ControllerSettings.ATTR_COURSES, courses);
            addPaginationModel(model, result, safePage, safePageSize, courseId);
            return ControllerSettings.VIEW_QUESTION_BANK_LIST;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.atError().addArgument(endpoint).log("[TheQuestionBankController] 401 Unauthorized when calling {}: Token may be invalid or expired");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(endpoint).log("[TheQuestionBankController] 403 Forbidden when calling {}: Token may be invalid or expired");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.BadRequest e) {
            log.atWarn().addArgument(endpoint).setCause(e).log("[TheQuestionBankController] 400 Bad Request when calling {}. Retrying with safe default filter.");
            try {
                QuestionBankFilterRequestDto defaultFilter = new QuestionBankFilterRequestDto();
                PaginationParams defaultPagination = PaginationSupport.normalize(null, null);
                defaultFilter.setPage(defaultPagination.page());
                defaultFilter.setPageSize(defaultPagination.pageSize());
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
                model.addAttribute(ControllerSettings.ATTR_COURSES, courses != null ? courses : new ArrayList<>());
                addPaginationModel(model, retryResult, defaultPagination.page(), defaultPagination.pageSize(), null);
                return ControllerSettings.VIEW_QUESTION_BANK_LIST;
            } catch (Exception retryEx) {
                log.atError().setCause(retryEx).addArgument(endpoint).log("[TheQuestionBankController] Retry with safe default filter failed for endpoint {}.");
            }
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(endpoint).addArgument(e.getMessage()).log("[TheQuestionBankController] Failed to fetch filtered questionBanks from {}: {}");
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COULD_NOT_LOAD_QUESTION_BANKS);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            model.addAttribute(ControllerSettings.ATTR_JWT_TOKEN_PRESENT, true);
            model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
            model.addAttribute(ControllerSettings.ATTR_COURSES, fetchCoursesFromAPI());
            addPaginationModel(model, null, safePage, safePageSize, courseId);
            return ControllerSettings.VIEW_QUESTION_BANK_LIST;
        }

        model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_FILTER, new QuestionBankFilterResponseDto());
        model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COULD_NOT_LOAD_QUESTION_BANKS);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
        model.addAttribute(ControllerSettings.ATTR_JWT_TOKEN_PRESENT, true);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_COURSES, fetchCoursesFromAPI());
        addPaginationModel(model, null, safePage, safePageSize, courseId);
        return ControllerSettings.VIEW_QUESTION_BANK_LIST;
    }

    private void addPaginationModel(Model model, QuestionBankFilterResponseDto result, int page, int pageSize, Long courseId) {
        int currentPage = result != null && result.getPage() != null ? result.getPage() : page;
        int effectivePageSize = result != null && result.getPageSize() != null ? result.getPageSize() : pageSize;
        int totalPages = result != null && result.getTotalPages() != null ? result.getTotalPages() : 0;
        long totalElements = result != null && result.getTotalElements() != null ? result.getTotalElements() : 0L;
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, currentPage);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, effectivePageSize);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, totalPages);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, totalElements);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("courseId", courseId);
        model.addAttribute(ControllerSettings.ATTR_PAGINATION,
                PaginationView.of("/question-banks", currentPage, effectivePageSize, totalPages, totalElements, filters));
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
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COURSE_NOT_FOUND_SHOWING_ALL_PREFIX + courseName + ControllerSettings.MSG_COURSE_NOT_FOUND_SHOWING_ALL_SUFFIX);
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
        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        try {
            ResponseEntity<QuestionBankDto> response = restTemplate.exchange(questionBankUrl, HttpMethod.GET, entity, QuestionBankDto.class);
            QuestionBankDto questionBankDto = response.getBody();
            if (questionBankDto == null || questionBankDto.getId() == null) {
                log.atWarn().addArgument(id).addArgument(questionBankUrl).log("QuestionBank details payload missing for id {} from {}");
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_DETAILS_UNAVAILABLE);
                return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
            }
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, questionBankDto);
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTION_BANK_URL,
                    buildQuestionBankListBackUrl(pagination.page(), pagination.pageSize(), courseId));
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_QUESTION_BANK_DETAILS;
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found (404)");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND_DELETED);
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden to questionBank with id {} (403)");
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to fetch questionBank with id {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_LOAD_FAILED);
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        }
    }

    @GetMapping("/{id}/extended")
    public String getQuestionBankExtendedById(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                               @RequestParam(value = "authorsPage", required = false) Integer authorsPage,
                                               @RequestParam(value = "mcPage", required = false) Integer mcPage,
                                               @RequestParam(value = "tfPage", required = false) Integer tfPage,
                                               @RequestParam(value = "errorsPage", required = false) Integer errorsPage,
                                               @RequestParam(value = "duplicatesPage", required = false) Integer duplicatesPage,
                                               @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId, Model model, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String questionBankUrl = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/extended";
        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        try {
            ResponseEntity<QuestionBankExportDto> response = restTemplate.exchange(questionBankUrl, HttpMethod.GET, entity, QuestionBankExportDto.class);
            QuestionBankExportDto questionBankExtended = response.getBody();
            if (questionBankExtended == null || questionBankExtended.getQuestionBank() == null || questionBankExtended.getQuestionBank().getId() == null) {
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_EXPORT_VIEW_UNAVAILABLE);
                return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
            }
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK_EXTENDED, questionBankExtended);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, questionBankExtended.getQuestionBank());
            addExtendedDetailsPagination(model, id, questionBankExtended.getAuthorSections(), pagination, courseId,
                    authorsPage, mcPage, tfPage, errorsPage, duplicatesPage);
            model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, pagination.page());
            model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pagination.pageSize());
            model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTION_BANK_URL,
                    buildQuestionBankListBackUrl(pagination.page(), pagination.pageSize(), courseId));
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_QUESTION_BANK_EXTENDED_DETAILS;
        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND_DELETED);
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to fetch extended questionBank with id {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COULD_NOT_LOAD_EXPORT_VIEW);
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        }
    }

    private void addExtendedDetailsPagination(Model model, Long questionBankId,
                                               List<QuestionBankExportAuthorSectionDto> allSections,
                                               PaginationParams parentPagination, Long courseId,
                                               Integer authorsPage, Integer mcPage, Integer tfPage,
                                               Integer errorsPage, Integer duplicatesPage) {
        List<QuestionBankExportAuthorSectionDto> sections = allSections != null ? allSections : List.of();
        PaginationResult<QuestionBankExportAuthorSectionDto> authorsResult = PaginationSupport.paginate(
                sections, authorsPage, parentPagination.pageSize());
        model.addAttribute(ControllerSettings.ATTR_AUTHOR_SECTIONS, authorsResult.items());

        Map<String, Object> authorFilters = new LinkedHashMap<>();
        authorFilters.put(ControllerSettings.ATTR_PAGE_NUMBER, parentPagination.page());
        authorFilters.put(ControllerSettings.ATTR_COURSE_ID, courseId);
        authorFilters.put("mcPage", mcPage);
        authorFilters.put("tfPage", tfPage);
        authorFilters.put("errorsPage", errorsPage);
        authorFilters.put("duplicatesPage", duplicatesPage);
        model.addAttribute("authorSectionsPagination", PaginationView.of(
                "/question-banks/" + questionBankId + "/extended", "authorsPage", ControllerSettings.ATTR_PAGE_SIZE,
                authorsResult.page(), authorsResult.pageSize(), authorsResult.totalPages(), authorsResult.totalElements(), authorFilters));

        Map<Long, PaginationView> mcPagination = new LinkedHashMap<>();
        Map<Long, PaginationView> tfPagination = new LinkedHashMap<>();
        Map<Long, PaginationView> errorPagination = new LinkedHashMap<>();
        Map<Long, PaginationView> duplicatePagination = new LinkedHashMap<>();
        Map<Long, Integer> mcTotals = new LinkedHashMap<>();
        Map<Long, Integer> tfTotals = new LinkedHashMap<>();
        Map<Long, Integer> errorTotals = new LinkedHashMap<>();
        Map<Long, Integer> duplicateTotals = new LinkedHashMap<>();
        for (QuestionBankExportAuthorSectionDto section : authorsResult.items()) {
            if (section == null || section.getAuthor() == null || section.getAuthor().getId() == null) {
                continue;
            }
            Long authorId = section.getAuthor().getId();
            mcTotals.put(authorId, section.getMultipleChoiceQuestions() != null ? section.getMultipleChoiceQuestions().size() : 0);
            tfTotals.put(authorId, section.getTrueFalseQuestions() != null ? section.getTrueFalseQuestions().size() : 0);
            errorTotals.put(authorId, section.getErrors() != null ? section.getErrors().size() : 0);
            duplicateTotals.put(authorId, section.getDuplicateQuestions() != null ? section.getDuplicateQuestions().size() : 0);
            PaginationResult<com.unitbv.myquiz.api.dto.QuestionDto> mcResult = PaginationSupport.paginate(
                    section.getMultipleChoiceQuestions(), mcPage, parentPagination.pageSize());
            PaginationResult<com.unitbv.myquiz.api.dto.QuestionDto> tfResult = PaginationSupport.paginate(
                    section.getTrueFalseQuestions(), tfPage, parentPagination.pageSize());
            PaginationResult<com.unitbv.myquiz.api.dto.QuestionErrorDto> errorResult = PaginationSupport.paginate(
                    section.getErrors(), errorsPage, parentPagination.pageSize());
            PaginationResult<com.unitbv.myquiz.api.dto.QuestionDuplicateDto> duplicateResult = PaginationSupport.paginate(
                    section.getDuplicateQuestions(), duplicatesPage, parentPagination.pageSize());

            section.setMultipleChoiceQuestions(mcResult.items());
            section.setTrueFalseQuestions(tfResult.items());
            section.setErrors(errorResult.items());
            section.setDuplicateQuestions(duplicateResult.items());

            mcPagination.put(authorId, buildExtendedPagination(questionBankId, authorId, "mcPage", parentPagination,
                    courseId, authorsPage, tfPage, errorsPage, duplicatesPage, mcResult));
            tfPagination.put(authorId, buildExtendedPagination(questionBankId, authorId, "tfPage", parentPagination,
                    courseId, authorsPage, mcPage, errorsPage, duplicatesPage, tfResult));
            errorPagination.put(authorId, buildExtendedPagination(questionBankId, authorId, "errorsPage", parentPagination,
                    courseId, authorsPage, mcPage, tfPage, duplicatesPage, errorResult));
            duplicatePagination.put(authorId, buildExtendedPagination(questionBankId, authorId, "duplicatesPage", parentPagination,
                    courseId, authorsPage, mcPage, tfPage, errorsPage, duplicateResult));
        }
        model.addAttribute("extendedMcPagination", mcPagination);
        model.addAttribute("extendedTfPagination", tfPagination);
        model.addAttribute("extendedErrorPagination", errorPagination);
        model.addAttribute("extendedDuplicatePagination", duplicatePagination);
        model.addAttribute("extendedMcTotals", mcTotals);
        model.addAttribute("extendedTfTotals", tfTotals);
        model.addAttribute("extendedErrorTotals", errorTotals);
        model.addAttribute("extendedDuplicateTotals", duplicateTotals);
    }

    private <T> PaginationView buildExtendedPagination(Long questionBankId, Long authorId, String pageParam,
                                                        PaginationParams parentPagination, Long courseId,
                                                        Integer authorsPage, Integer firstOtherPage,
                                                        Integer secondOtherPage, Integer thirdOtherPage,
                                                        PaginationResult<T> result) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put(ControllerSettings.ATTR_PAGE_NUMBER, parentPagination.page());
        filters.put(ControllerSettings.ATTR_COURSE_ID, courseId);
        filters.put("authorsPage", authorsPage);
        if ("mcPage".equals(pageParam)) {
            filters.put("tfPage", firstOtherPage);
            filters.put("errorsPage", secondOtherPage);
            filters.put("duplicatesPage", thirdOtherPage);
        } else if ("tfPage".equals(pageParam)) {
            filters.put("mcPage", firstOtherPage);
            filters.put("errorsPage", secondOtherPage);
            filters.put("duplicatesPage", thirdOtherPage);
        } else if ("errorsPage".equals(pageParam)) {
            filters.put("mcPage", firstOtherPage);
            filters.put("tfPage", secondOtherPage);
            filters.put("duplicatesPage", thirdOtherPage);
        } else {
            filters.put("mcPage", firstOtherPage);
            filters.put("tfPage", secondOtherPage);
            filters.put("errorsPage", thirdOtherPage);
        }
        return PaginationView.of("/question-banks/" + questionBankId + "/extended", pageParam,
                ControllerSettings.ATTR_PAGE_SIZE, result.page(), result.pageSize(), result.totalPages(),
                result.totalElements(), filters);
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
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_QUESTION_BANK_CREATED_SUCCESS);
        } catch (Exception e) {
            log.atError().setCause(e).log("Failed to create questionBank");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_CREATE_FAILED);
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
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND_DELETED);
            return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden to questionBank with id {} (403) during edit");
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to fetch questionBank for edit with id {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_LOAD_FOR_EDIT_FAILED);
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
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_QUESTION_BANK_UPDATED_SUCCESS);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to update questionBank with id {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_UPDATE_FAILED);
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @PostMapping("/delete/{id}")
    public String deleteQuestionBank(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_QUESTION_BANK_DELETED_SUCCESS);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to delete questionBank with id {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_DELETE_FAILED);
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
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_QUESTION_BANK_CREATED_SUCCESS);
            } else {
                restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + questionBankDto.getId(), HttpMethod.PUT, entity, Void.class);
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_QUESTION_BANK_UPDATED_SUCCESS);
            }
        } catch (Exception ex) {
            log.atError().setCause(ex).addArgument(ex.getMessage()).log("Failed to save questionBank: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_SAVE_FAILED);
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
            String filename = "questionBank_" + id + "_mc.csv";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for MC export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for MC export of questionBank {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ControllerSettings.MSG_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export MC questionBank {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ControllerSettings.MSG_EXPORT_FAILED);
        }
    }

    @GetMapping("/{id}/export-tf")
    public void exportQuestionBankTrueFalse(@PathVariable Long id, HttpServletResponse response) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/export-tf";
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("text/csv; charset=UTF-8");
            String filename = "questionBank_" + id + "_tf.csv";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for TF export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for TF export of questionBank {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ControllerSettings.MSG_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export TF questionBank {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ControllerSettings.MSG_EXPORT_FAILED);
        }
    }

    @GetMapping("/{id}/export-xml")
    public void exportQuestionBankXml(@PathVariable Long id, HttpServletResponse response) throws IOException {
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id + "/export-xml";
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("application/xml; charset=UTF-8");
            String filename = "questionBank_" + id + ".xml";
            String apiDisposition = apiResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, apiDisposition != null ? apiDisposition : ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for XML export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for XML export of questionBank {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ControllerSettings.MSG_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export XML for questionBank {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ControllerSettings.MSG_EXPORT_FAILED);
        }
    }


    @GetMapping("/{id}/delete")
    public String getDeleteQuestionBank(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        try {
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_QUESTION_BANKS_SLASH + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_QUESTION_BANK_DELETED_SUCCESS);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Failed to delete questionBank with id {} via GET");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_DELETE_FAILED);
        }
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @GetMapping("/{id}/statistics")
    public String questionBankStatistics(@PathVariable Long id,
                                         @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                         @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                                         @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                                         Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        model.addAttribute(ControllerSettings.ATTR_BACK_TO_QUESTION_BANK_URL,
                buildQuestionBankListBackUrl(pagination.page(), pagination.pageSize(), courseId));
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, courseId);
        model.addAttribute(ControllerSettings.ATTR_PAGE_NUMBER, pagination.page());
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pagination.pageSize());

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_QUESTION_BANKS + "/" + id + "/statistics";
        try {
            ResponseEntity<QuestionBankStatisticsDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, QuestionBankStatisticsDto.class);
            QuestionBankStatisticsDto stats = response.getBody();
            if (stats != null) {
                model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, stats.getQuestionBank());
                model.addAttribute(ControllerSettings.ATTR_AUTHOR_STATS, stats.getAuthorStats());
            } else {
                model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
                model.addAttribute(ControllerSettings.ATTR_AUTHOR_STATS, new ArrayList<>());
            }
            return ControllerSettings.VIEW_QUESTION_BANK_STATISTICS;
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("QuestionBank with id {} not found for statistics (404)");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_QUESTION_BANK_NOT_FOUND);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
            model.addAttribute(ControllerSettings.ATTR_AUTHOR_STATS, new ArrayList<>());
            return ControllerSettings.VIEW_QUESTION_BANK_STATISTICS;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden to statistics for questionBank {} (403)");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_ACCESS_DENIED_PLAIN);
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
            model.addAttribute(ControllerSettings.ATTR_AUTHOR_STATS, new ArrayList<>());
            return ControllerSettings.VIEW_QUESTION_BANK_STATISTICS;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to fetch statistics for questionBank {}: {}");
            model.addAttribute(ControllerSettings.ATTR_QUESTION_BANK, new QuestionBankDto());
            model.addAttribute(ControllerSettings.ATTR_AUTHOR_STATS, new ArrayList<>());
            model.addAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COULD_NOT_LOAD_STATISTICS);
            return ControllerSettings.VIEW_QUESTION_BANK_STATISTICS;
        }
    }
}
