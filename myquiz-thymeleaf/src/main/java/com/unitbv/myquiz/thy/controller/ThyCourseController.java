package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.HttpEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thymeleaf controller for Course management operations.
 * Handles course listing, filtering, creation, editing, and deletion.
 * Provides server-side rendering for course-related pages.
 */
@Controller
@RequestMapping("/courses")
public class ThyCourseController {
    private static final Logger log = LoggerFactory.getLogger(ThyCourseController.class);

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Autowired
    public ThyCourseController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    @GetMapping({"/", ""})
    public String listAll(Model model,
                         @RequestParam(value = "selectedCourse", required = false) String selectedCourse,
                         @RequestParam(value = "selectedYear", required = false) String selectedYear,
                         @RequestParam(value = "currentPage", required = false) Integer currentPage,
                         @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return renderCourseList(selectedCourse, selectedYear, currentPage, pageSize, model);
    }

    @PostMapping("/filter")
    public String filterCourses(
            @RequestParam(value = "selectedCourse", required = false) String selectedCourse,
            @RequestParam(value = "selectedYear", required = false) String selectedYear,
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            Model model) {
        return renderCourseList(selectedCourse, selectedYear, currentPage, pageSize, model);
    }

    private String renderCourseList(String selectedCourse, String selectedYear, Integer currentPage, Integer pageSize, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        Object loggedInUser = sessionService.getLoggedInUser();

        if (currentPage == null || currentPage < 1) {
            currentPage = 1;
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

        if (selectedCourse != null && !selectedCourse.isEmpty()) {
            filteredCourses = filteredCourses.stream()
                    .filter(course -> course.getCourse().toLowerCase().contains(selectedCourse.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (selectedYear != null && !selectedYear.isEmpty()) {
            filteredCourses = filteredCourses.stream()
                    .filter(course -> course.getUniversityYear() != null && course.getUniversityYear().toString().equals(selectedYear))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Calculate pagination
        int totalElements = filteredCourses.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);

        java.util.List<CourseDto> paginatedCourses = filteredCourses.subList(startIndex, endIndex);

        // Get unique years for filter dropdown
        java.util.Set<String> uniqueYears = java.util.Arrays.stream(allCourses)
                .map(CourseDto::getUniversityYear)
                .filter(year -> year != null)
                .collect(java.util.stream.Collectors.toSet());

        // Set model attributes
        model.addAttribute(ControllerSettings.ATTR_COURSES, paginatedCourses);
        model.addAttribute("years", uniqueYears);
        model.addAttribute("selectedCourse", selectedCourse);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", pageSize);
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
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<CourseDto> entity = sessionService.createAuthorizedRequest(courseDto);
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, org.springframework.http.HttpMethod.PUT, entity, Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Course updated successfully");
        return ControllerSettings.VIEW_REDIRECT_COURSES;
    }

    @GetMapping("/{id}/delete")
    public String deleteCourseView(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();
        restTemplate.exchange(apiBaseUrl + ControllerSettings.API_COURSES + id, org.springframework.http.HttpMethod.DELETE, entity, Void.class);
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "Course deleted successfully");
        return ControllerSettings.VIEW_REDIRECT_COURSES;
    }
}
