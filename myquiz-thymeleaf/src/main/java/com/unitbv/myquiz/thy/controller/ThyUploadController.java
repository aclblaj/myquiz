package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import com.unitbv.myquiz.thy.util.MultipartFileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Thymeleaf controller for file upload operations.
 * Handles Excel file uploads for question import and archive uploads.
 * Supports single and batch file uploads with template type selection.
 * Provides server-side rendering for upload-related pages.
 */
@RequestMapping("/uploads")
@Controller
public class ThyUploadController {
    private static final Logger log = LoggerFactory.getLogger(ThyUploadController.class);

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    private final SessionService sessionService;
    private final RestTemplate restTemplate;

    @Autowired
    public ThyUploadController(SessionService sessionService, RestTemplate restTemplate) {
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/excel-form")
    public String uploadExcel(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        Object loggedInUser = sessionService.getLoggedInUser();

        String coursesUrl = apiBaseUrl + ControllerSettings.API_COURSES;
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

        ResponseEntity<List<Object>> coursesResponse = restTemplate.exchange(
                coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
        );
        List<Object> courses = coursesResponse.getBody();
        model.addAttribute("courses", courses);

        List<String> templates = List.of(TemplateType.getAllTypesAsStringArray());
        log.info("Available templates: {}", templates);
        model.addAttribute("templates", templates);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
        return "upload-excel";
    }

    @GetMapping("/archive-form")
    public String uploadArchive(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        Object loggedInUser = sessionService.getLoggedInUser();

        String coursesUrl = apiBaseUrl + ControllerSettings.API_COURSES;
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

        ResponseEntity<List<Object>> coursesResponse = restTemplate.exchange(
                coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
        );
        List<Object> courses = coursesResponse.getBody();
        model.addAttribute("courses", courses);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);

        List<String> templates = List.of(TemplateType.getAllTypesAsStringArray());
        log.info("Available templates: {}", templates);
        model.addAttribute("templates", templates);
        return "upload-archive";
    }

    @PostMapping("/excel-file")
    public String handleExcelUpload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("username") String username,
                                    @RequestParam("courseId") String courseId,
                                    @RequestParam("name") String name,
                                    @RequestParam("template") String template,
                                    Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_EXCEL;

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));
            body.add("username", username);
            body.add("courseId", Long.valueOf(courseId));
            body.add("name", name);
            body.add("template", template);

            // Create multipart request with authorization
            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);

            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Excel file uploaded successfully. " + response.getBody());
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("403 Forbidden on excel upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.error("Excel file upload failed: {}", e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Excel file upload failed: " + e.getMessage());
        }
        return ControllerSettings.VIEW_SUCCESS;
    }

    @PostMapping("/archive-file")
    public String handleArchiveUpload(@RequestParam("archive") MultipartFile archive,
                                      @RequestParam("courseId") String courseId,
                                      @RequestParam("quiz") String quiz,
                                      @RequestParam("year") Long year,
                                      Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        // Validate required fields
        if (archive == null || archive.isEmpty()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive file is required");
            return ControllerSettings.VIEW_SUCCESS;
        }
        if (courseId == null || courseId.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Course ID is required");
            return ControllerSettings.VIEW_SUCCESS;
        }
        if (quiz == null || quiz.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Quiz name is required");
            return ControllerSettings.VIEW_SUCCESS;
        }
        if (year == null || year < 2000 || year > 2100) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Year must be between 2000 and 2100");
            return ControllerSettings.VIEW_SUCCESS;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_ARCHIVE;
            log.info("Upload URL: {}", uploadUrl);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("archive", new MultipartFileResource(archive));
            body.add("courseId", Long.valueOf(courseId));
            body.add("quiz", quiz);
            body.add("year", year);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive uploaded successfully. " + response.getBody());
            return ControllerSettings.VIEW_SUCCESS;

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden on archive upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error on archive upload: {} - {}", e.getStatusCode(), e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_MESSAGE,
                "Archive upload failed: " + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (IOException e) {
            log.error("IOException on archive upload: {}", e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive file upload failed: " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (Exception e) {
            log.error("Unexpected error on archive upload: {}", e.getMessage(), e);
            model.addAttribute(ControllerSettings.ATTR_MESSAGE,
                "Archive upload failed with unexpected error: " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        }
    }
}
