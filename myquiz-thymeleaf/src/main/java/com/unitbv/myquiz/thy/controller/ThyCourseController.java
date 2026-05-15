package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseDuplicateRecomputeResultDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Thymeleaf controller for Course management operations.
 * Handles course listing, filtering, creation, editing, and deletion.
 * Provides server-side rendering for course-related pages.
 */
@Controller
@RequestMapping("/courses")
public class ThyCourseController {
    private static final Logger log = LoggerFactory.getLogger(ThyCourseController.class);
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
        if (redirect != null) return redirect;

        Object loggedInUser = sessionService.getLoggedInUser();

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = ControllerSettings.PAGE_SIZE;
        }

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

        // Get all courses (since we don't have a filter API endpoint, we'll filter in memory)
        CourseDto[] allCourses = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES, org.springframework.http.HttpMethod.GET, entity, CourseDto[].class).getBody();

        if (allCourses == null) {
            allCourses = new CourseDto[0];
        }

        // Filter courses based on criteria
        java.util.List<CourseDto> filteredCourses = java.util.Arrays.asList(allCourses);

        if (selectedCourseId != null && selectedCourseId > 0) {
            filteredCourses = filteredCourses.stream().filter(course -> course.getId() != null && course.getId().equals(selectedCourseId)).toList();
        }

        if (selectedYear != null && !selectedYear.isEmpty()) {
            filteredCourses = filteredCourses.stream().filter(course -> course.getUniversityYear() != null && course.getUniversityYear().equals(selectedYear)).toList();
        }

        // Calculate pagination
        int totalElements = filteredCourses.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);

        java.util.List<CourseDto> paginatedCourses = filteredCourses.subList(startIndex, endIndex);

        // Get unique years for filter dropdown
        java.util.Set<String> uniqueYears = java.util.Arrays.stream(allCourses).map(CourseDto::getUniversityYear).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());

        // Set model attributes
        model.addAttribute(ControllerSettings.ATTR_COURSES, paginatedCourses);
        model.addAttribute(ControllerSettings.ATTR_YEARS, uniqueYears);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_COURSE_ID, selectedCourseId);
        model.addAttribute(ControllerSettings.ATTR_SELECTED_YEAR, selectedYear);
        model.addAttribute(ControllerSettings.ATTR_CURRENT_PAGE, page);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_PAGES, totalPages);
        model.addAttribute(ControllerSettings.ATTR_TOTAL_ELEMENTS, totalElements);
        model.addAttribute(ControllerSettings.ATTR_PAGE_SIZE, pageSize);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);

        return ControllerSettings.VIEW_COURSE_LIST;
    }

    @GetMapping("/{id}")
    public String getCourseById(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        CourseDto course = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, org.springframework.http.HttpMethod.GET, entity, CourseDto.class).getBody();
        model.addAttribute(ControllerSettings.ATTR_COURSE, course);
        return ControllerSettings.VIEW_COURSE_DETAILS;
    }

    @PostMapping({"", "/"})
    public String createCourse(@ModelAttribute CourseDto courseDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<CourseDto> entity = sessionService.createAuthorizedRequest(courseDto);
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES, org.springframework.http.HttpMethod.POST, entity, CourseDto.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Course created successfully");
        return ControllerSettings.VIEW_REDIRECT_COURSES;
    }

    @PutMapping("/{id}")
    public String updateCourse(@PathVariable Long id, @ModelAttribute CourseDto courseDto, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<CourseDto> entity = sessionService.createAuthorizedRequest(courseDto);
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, org.springframework.http.HttpMethod.PUT, entity, Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Course updated successfully");
        return ControllerSettings.VIEW_REDIRECT_COURSES;
    }

    @DeleteMapping("/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, org.springframework.http.HttpMethod.DELETE, entity, Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Course deleted successfully");
        return ControllerSettings.VIEW_REDIRECT_COURSES;
    }

    @GetMapping("/new")
    public String newCourse(Model model) {
        model.addAttribute(ControllerSettings.ATTR_COURSE, new CourseDto());
        return ControllerSettings.VIEW_COURSE_EDIT;
    }

    @GetMapping("/edit/{id}")
    public String editCourse(@PathVariable Long id, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        CourseDto course = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, org.springframework.http.HttpMethod.GET, entity, CourseDto.class).getBody();
        model.addAttribute(ControllerSettings.ATTR_COURSE, course);
        return ControllerSettings.VIEW_COURSE_EDIT;
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
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String endpoint = apiBaseUrl + ControllerSettings.API_COURSES + id + "/recompute-duplicates";
        try {
            ResponseEntity<CourseDuplicateRecomputeResultDto> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, CourseDuplicateRecomputeResultDto.class);
            CourseDuplicateRecomputeResultDto result = response.getBody();
            if (result != null) {
                redirectAttributes.addFlashAttribute(
                        ControllerSettings.ATTR_MESSAGE,
                        "Recompute started: " + result.getStartedAt() + " | finished: " + result.getEndedAt() + " | duration: " + result.getDurationMs() + " ms" + " | MC: " + result.getMultichoiceQuestions() + " | TF: " + result.getTruefalseQuestions() + " | links removed: " + result.getDuplicateLinksRemoved() + " | duplicate errors removed: " + result.getDuplicateErrorsRemoved() + " | duplicate errors created: " + result.getDuplicateErrorsCreated()
                );
            } else {
                redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Recompute completed.");
            }
        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Course not found for duplicate recompute.");
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            sessionService.invalidateCurrentSession();
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to recompute duplicates for course {}: {}");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MSG, "Could not recompute duplicates. Please try again later.");
        }

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
        if (page != null && page > 0) {
            redirectUrl.append(separator).append(ControllerSettings.ATTR_PAGE_NUMBER).append("=").append(page);
            separator = "&";
        }
        if (pageSize != null && pageSize > 0) {
            redirectUrl.append(separator).append(ControllerSettings.ATTR_PAGE_SIZE).append("=").append(pageSize);
        }
        return redirectUrl.toString();
    }

    @GetMapping("/{id}/export-xml")
    public void exportCourseXml(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            response.sendRedirect("/auth/login");
            return;
        }

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        String url = apiBaseUrl + ControllerSettings.API_COURSES + id + "/export-xml";
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            response.setContentType("application/xml; charset=UTF-8");
            String filename = "course_" + id + "_all_questionBanks.xml";
            String apiDisposition = apiResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, apiDisposition != null ? apiDisposition : "attachment; filename=" + filename);
            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.atWarn().addArgument(id).log("Course with id {} not found for XML export (404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Course not found");
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().addArgument(id).log("Access forbidden for XML export of course {} (403)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).addArgument(e.getMessage()).log("Failed to export XML for course {}: {}");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Export failed");
        }
    }
}
