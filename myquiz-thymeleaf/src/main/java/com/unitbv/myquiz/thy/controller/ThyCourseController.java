package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.thy.service.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thymeleaf controller for Course management operations.
 * Handles course listing, filtering, creation, editing, and deletion.
 * Provides server-side rendering for course-related pages.
 */
@Controller
@RequestMapping("/courses")
public class ThyCourseController {
    private static final Logger log = LoggerFactory.getLogger(ThyCourseController.class);
    private record CoursePageResult(List<CourseDto> courses, int currentPage, int totalPages, int totalElements) {
    }

    private final RestTemplate restTemplate;
    private final SessionService sessionService;
    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyCourseController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    @GetMapping({"/", ""})
    public String listAll(Model model, @RequestParam(value = ControllerSettings.ATTR_SELECTED_COURSE_ID, required = false) Long selectedCourseId,
                          @RequestParam(value = ControllerSettings.ATTR_SELECTED_YEAR, required = false) String selectedYear,
                          @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                          @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize) {
        return renderCourseList(selectedCourseId, selectedYear, page, pageSize, model);
    }

    @PostMapping("/filter")
    public String filterCourses(@RequestParam(value = ControllerSettings.ATTR_SELECTED_COURSE_ID, required = false) Long selectedCourseId,
                                @RequestParam(value = ControllerSettings.ATTR_SELECTED_YEAR, required = false) String selectedYear,
                                @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, Model model) {
        return renderCourseList(selectedCourseId, selectedYear, page, pageSize, model);
    }

    private String renderCourseList(Long selectedCourseId, String selectedYear, Integer page, Integer pageSize, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        Object loggedInUser = sessionService.getLoggedInUser();

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            CourseDto[] allCourses = fetchAllCourses(entity);
            List<CourseDto> filteredCourses = filterCoursesInMemory(allCourses, selectedCourseId, selectedYear);
            CoursePageResult pageResult = paginateCourses(filteredCourses, pagination);
            Set<String> uniqueYears = extractUniqueYears(allCourses);

            populateCourseListModelFromData(model, selectedCourseId, selectedYear, pagination.pageSize(), pageResult, uniqueYears);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            return ControllerSettings.VIEW_COURSE_LIST;
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden: Token expired while listing courses");
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception ex) {
            log.error("Error loading course list", ex);
            populateCourseListModelFallback(model, selectedCourseId, selectedYear, pagination);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
            return ControllerSettings.VIEW_COURSE_LIST;
        }
    }

    /**
     * Fetches all courses from API and returns an empty array when API body is null.
     */
    private CourseDto[] fetchAllCourses(HttpEntity<Void> entity) {
        CourseDto[] allCourses = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES, HttpMethod.GET, entity, CourseDto[].class).getBody();
        return allCourses != null ? allCourses : new CourseDto[0];
    }

    /**
     * Applies selected course and year filters in memory.
     */
    private List<CourseDto> filterCoursesInMemory(CourseDto[] allCourses, Long selectedCourseId, String selectedYear) {
        List<CourseDto> filteredCourses = Arrays.asList(allCourses);

        if (selectedCourseId != null && selectedCourseId > 0) {
            filteredCourses = filteredCourses.stream().filter(course -> course.getId() != null && course.getId().equals(selectedCourseId)).toList();
        }
        if (selectedYear != null && !selectedYear.isBlank()) {
            filteredCourses = filteredCourses.stream().filter(course -> selectedYear.equals(course.getUniversityYear())).toList();
        }

        return filteredCourses;
    }

    /**
     * Computes paginated course slice and metadata for the current filters.
     */
    private CoursePageResult paginateCourses(List<CourseDto> filteredCourses, PaginationParams pagination) {
        int totalElements = filteredCourses.size();
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / pagination.pageSize());
        int currentPage = Math.min(pagination.page(), totalPages);
        int startIndex = Math.min((currentPage - 1) * pagination.pageSize(), totalElements);
        int endIndex = Math.min(startIndex + pagination.pageSize(), totalElements);
        List<CourseDto> paginatedCourses = filteredCourses.subList(startIndex, endIndex);
        return new CoursePageResult(paginatedCourses, currentPage, totalPages, totalElements);
    }

    /**
     * Extracts distinct non-null university years for the filter dropdown.
     */
    private Set<String> extractUniqueYears(CourseDto[] allCourses) {
        return Arrays.stream(allCourses).map(CourseDto::getUniversityYear).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Populates model with course list data from filtered/paginated result.
     */
    private void populateCourseListModelFromData(Model model, Long selectedCourseId, String selectedYear, int pageSize,
                                                 CoursePageResult pageResult, Set<String> uniqueYears) {
        model.addAttribute(ControllerSettings.ATTR_COURSES, pageResult.courses());
        model.addAttribute(ControllerSettings.ATTR_YEARS, uniqueYears);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, selectedCourseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_YEAR, selectedYear);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, pageResult.currentPage());
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, pageResult.totalPages());
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, pageResult.totalElements());
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
    }

    /**
     * Populates model with fallback/error state for course list.
     */
    private void populateCourseListModelFallback(Model model, Long selectedCourseId, String selectedYear, PaginationParams pagination) {
        model.addAttribute(ControllerSettings.ATTR_COURSES, new CourseDto[0]);
        model.addAttribute(ControllerSettings.ATTR_YEARS, Set.of());
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, selectedCourseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_YEAR, selectedYear);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, pagination.page());
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, 1);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, 0);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pagination.pageSize());
        model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_UNEXPECTED_ERROR_RETRY_LATER);
    }

    @GetMapping("/{id}")
    public String getCourseById(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            CourseDto course = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, HttpMethod.GET, entity, CourseDto.class).getBody();
            model.addAttribute(ControllerSettings.ATTR_COURSE, course);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_COURSE_DETAILS;
        } catch (HttpClientErrorException.Forbidden ex) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
    }

    @PostMapping({"", "/"})
    public String createCourse(@ModelAttribute CourseDto courseDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<CourseDto> entity = sessionService.createAuthorizedRequest(courseDto);
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES, HttpMethod.POST, entity, CourseDto.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_COURSE_CREATED_SUCCESS);
            return ControllerSettings.VIEW_REDIRECT_COURSES;
        } catch (HttpClientErrorException.Forbidden ex) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
    }

    @PutMapping("/{id}")
    public String updateCourse(@PathVariable Long id, @ModelAttribute CourseDto courseDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<CourseDto> entity = sessionService.createAuthorizedRequest(courseDto);
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, HttpMethod.PUT, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_COURSE_UPDATED_SUCCESS);
            return ControllerSettings.VIEW_REDIRECT_COURSES;
        } catch (HttpClientErrorException.Forbidden ex) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
    }

    @DeleteMapping("/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, HttpMethod.DELETE, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_COURSE_DELETED_SUCCESS);
            return ControllerSettings.VIEW_REDIRECT_COURSES;
        } catch (HttpClientErrorException.Forbidden ex) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
    }

    @GetMapping("/new")
    public String newCourse(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        model.addAttribute(ControllerSettings.ATTR_COURSE, new CourseDto());
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_COURSE_EDIT;
    }

    @GetMapping("/edit/{id}")
    public String editCourse(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            CourseDto course = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, HttpMethod.GET, entity, CourseDto.class).getBody();
            model.addAttribute(ControllerSettings.ATTR_COURSE, course);
            model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
            return ControllerSettings.VIEW_COURSE_EDIT;
        } catch (HttpClientErrorException.Forbidden ex) {
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        }
    }

    @PostMapping("/edit/{id}")
    public String saveEditedCourse(@PathVariable Long id, @ModelAttribute CourseDto courseDto, RedirectAttributes redirectAttributes) {
        return updateCourse(id, courseDto, redirectAttributes);
    }

    @GetMapping("/{id}/delete")
    public String deleteCourseView(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteCourse(id, redirectAttributes);
    }

    @PostMapping("/{id}/recompute-duplicates")
    public String recomputeCourseDuplicates(@PathVariable Long id, @RequestParam(value = ControllerSettings.ATTR_SELECTED_COURSE_ID, required = false) Long selectedCourseId,
                                            @RequestParam(value = ControllerSettings.ATTR_SELECTED_YEAR, required = false) String selectedYear,
                                            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                                            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String endpoint = UriComponentsBuilder.fromUriString(apiBaseUrl + ControllerSettings.API_COURSES + id)
                .path(ControllerSettings.API_COURSES_RECOMPUTE_DUPLICATES_SUFFIX)
                .toUriString();
        try {
            ResponseEntity<CourseDuplicateRecomputeResultDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, CourseDuplicateRecomputeResultDto.class);
            CourseDuplicateRecomputeResultDto result = response.getBody();
            if (result != null) {
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, formatRecomputeResultMessage(result));
            } else {
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_RECOMPUTE_COMPLETED);
            }
        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COURSE_NOT_FOUND_RECOMPUTE);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to recompute duplicates for course {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COULD_NOT_RECOMPUTE_DUPLICATES);
        }

        return buildCourseListRedirectUrl(selectedCourseId, selectedYear, page, pageSize);
    }

    /**
     * Formats recompute duplicates result into a readable flash message.
     */
    private String formatRecomputeResultMessage(CourseDuplicateRecomputeResultDto result) {
        return ControllerSettings.MSG_RECOMPUTE_STARTED_PREFIX
                + result.getStartedAt()
                + " | finished: "
                + result.getEndedAt()
                + " | duration: "
                + result.getDurationMs()
                + " ms"
                + " | MC: "
                + result.getMultichoiceQuestions()
                + " | TF: "
                + result.getTruefalseQuestions()
                + " | links removed: "
                + result.getDuplicateLinksRemoved()
                + " | duplicate errors removed: "
                + result.getDuplicateErrorsRemoved()
                + " | duplicate errors created: "
                + result.getDuplicateErrorsCreated();
    }

    /**
     * Builds redirect URL to course list while preserving filters and pagination context.
     */
    private String buildCourseListRedirectUrl(Long selectedCourseId, String selectedYear, Integer page, Integer pageSize) {
        PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
        StringBuilder redirectUrl = new StringBuilder(ControllerSettings.VIEW_REDIRECT_COURSES);
        String separator = "?";
        if (selectedCourseId != null && selectedCourseId > 0) {
            redirectUrl.append(separator).append(ControllerSettings.ATTR_SELECTED_COURSE_ID).append("=").append(selectedCourseId);
            separator = "&";
        }
        if (selectedYear != null && !selectedYear.isBlank()) {
            redirectUrl.append(separator).append(ControllerSettings.ATTR_SELECTED_YEAR).append("=").append(URLEncoder.encode(selectedYear, StandardCharsets.UTF_8));
            separator = "&";
        }
        if (pagination.page() > 0) {
            redirectUrl.append(separator).append(ControllerSettings.ATTR_PAGE_NUMBER).append("=").append(pagination.page());
            separator = "&";
        }
        if (pagination.pageSize() > 0) {
            redirectUrl.append(separator).append(ControllerSettings.ATTR_PAGE_SIZE).append("=").append(pagination.pageSize());
        }
        return redirectUrl.toString();
    }

    @GetMapping("/{id}/export-xml")
    public void exportCourseXml(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            response.sendRedirect(ControllerSettings.PATH_AUTH_LOGIN);
            return;
        }

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = UriComponentsBuilder.fromUriString(apiBaseUrl + ControllerSettings.API_COURSES + id)
                .path(ControllerSettings.API_COURSES_EXPORT_XML_SUFFIX)
                .toUriString();
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("application/xml; charset=UTF-8");
            String filename = ControllerSettings.FILE_COURSE_XML_PREFIX + id + ControllerSettings.FILE_COURSE_XML_SUFFIX;
            String apiDisposition = apiResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, apiDisposition != null ? apiDisposition : ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("Course with id {} not found for XML export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ControllerSettings.MSG_COURSE_NOT_FOUND_EXPORT);
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for XML export of course {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ControllerSettings.MSG_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export XML for course {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ControllerSettings.MSG_EXPORT_FAILED);
        }
    }

    @GetMapping("/default-courses")
    public String showDefaultCoursesForm(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        Object loggedInUser = sessionService.getLoggedInUser();
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
        return ControllerSettings.VIEW_DEFAULT_COURSES_FORM;
    }

    @PostMapping("/default-courses")
    public String createDefaultCourses(RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
            ResponseEntity<Integer> response = restTemplate.exchange(
                    apiBaseUrl + ControllerSettings.API_COURSES + ControllerSettings.API_COURSES_CREATE_DEFAULTS_SUFFIX,
                    HttpMethod.POST,
                    entity,
                    Integer.class
            );
            Integer createdCount = response.getBody();
            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    ControllerSettings.MSG_DEFAULT_COURSES_CREATED_PREFIX + (createdCount != null ? createdCount : 0) + ControllerSettings.MSG_DEFAULT_COURSES_CREATED_SUFFIX
            );
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Failed to create default courses: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, ControllerSettings.MSG_COULD_NOT_CREATE_DEFAULT_COURSES);
        }

        return ControllerSettings.VIEW_REDIRECT_COURSES;
    }
}
