package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.dto.ArchiveFolderItemDto;
import com.unitbv.myquiz.api.dto.ArchiveFolderUploadResultDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.api.types.TemplateType;
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
import java.util.Arrays;
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
    private static final String INVALID_STUDY_YEAR_MESSAGE = "Please select a valid study year";
    private final SessionService sessionService;
    private final RestTemplate restTemplate;
    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

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
                coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                }
        );
        List<Object> courses = coursesResponse.getBody();
        model.addAttribute(ControllerSettings.ATTR_COURSES, courses);

        List<String> templates = List.of(TemplateType.getAllTypesAsStringArray());
        log.atInfo().addArgument(templates).log("Available templates: {}");
        model.addAttribute(ControllerSettings.ATTR_TEMPLATES, templates);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);
        return ControllerSettings.VIEW_UPLOAD_EXCEL;
    }

    @GetMapping("/archive-form")
    public String uploadArchive(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        populateArchiveUploadModel(model);
        return ControllerSettings.VIEW_UPLOAD_ARCHIVE_SINGLE;
    }

    @GetMapping("/archive-folder-form")
    public String uploadArchiveFolder(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        populateArchiveUploadModel(model);
        return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
    }

    @GetMapping("/xml-form")
    public String uploadXml(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        populateArchiveUploadModel(model);
        return ControllerSettings.VIEW_UPLOAD_XML;
    }

    private void populateArchiveUploadModel(Model model) {
        Object loggedInUser = sessionService.getLoggedInUser();

        String coursesUrl = apiBaseUrl + ControllerSettings.API_COURSES;
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

        ResponseEntity<List<Object>> coursesResponse = restTemplate.exchange(
                coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                }
        );
        List<Object> courses = coursesResponse.getBody();
        model.addAttribute(ControllerSettings.ATTR_COURSES, courses);
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, loggedInUser);

        List<String> templates = List.of(TemplateType.getAllTypesAsStringArray());
        log.atInfo().addArgument(templates).log("Available templates: {}");
        model.addAttribute(ControllerSettings.ATTR_TEMPLATES, templates);
        model.addAttribute("studyYears", StudyYear.values());
    }

    @PostMapping("/excel-file")
    public String handleExcelUpload(@RequestParam("file") MultipartFile file, @RequestParam("username") String username, @RequestParam("courseId") String courseId, @RequestParam("name") String name,
                                    @RequestParam("template") String template, Model model) {
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
            log.atError().log("403 Forbidden on excel upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Excel file upload failed: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Excel file upload failed: " + e.getMessage());
        }
        return ControllerSettings.VIEW_SUCCESS;
    }

    @PostMapping("/archive-file")
    public String handleArchiveUpload(@RequestParam("archive") MultipartFile archive, @RequestParam("courseId") String courseId, @RequestParam("questionBank") String questionBank,
                                      @RequestParam("studyYear") StudyYear studyYear, Model model) {
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
        if (questionBank == null || questionBank.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "QuestionBank name is required");
            return ControllerSettings.VIEW_SUCCESS;
        }
        if (studyYear == null) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, INVALID_STUDY_YEAR_MESSAGE);
            return ControllerSettings.VIEW_SUCCESS;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_ARCHIVE;
            log.atInfo().addArgument(uploadUrl).log("Upload URL: {}");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("archive", new MultipartFileResource(archive));
            body.add("courseId", Long.valueOf(courseId));
            body.add("questionBankName", questionBank);
            body.add("studyYear", studyYear.name());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive uploaded successfully. " + response.getBody());
            return ControllerSettings.VIEW_SUCCESS;

        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("403 Forbidden on archive upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.atError().setCause(e).addArgument(e.getStatusCode()).addArgument(e.getMessage()).log("HTTP error on archive upload: {} - {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive upload failed: " + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (IOException e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("IOException on archive upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive file upload failed: " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Unexpected error on archive upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive upload failed with unexpected error: " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        }
    }

    @PostMapping("/archive-folder")
    public String handleArchiveFolderUpload(@RequestParam("archives") MultipartFile[] archives, @RequestParam("studyYear") StudyYear studyYear, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        populateArchiveUploadModel(model);

        if (archives == null || archives.length == 0) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Please select a folder containing ZIP archives");
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }
        if (studyYear == null) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, INVALID_STUDY_YEAR_MESSAGE);
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }

        List<MultipartFile> nonEmptyArchives = Arrays.stream(archives).filter(file -> file != null && !file.isEmpty()).toList();
        if (nonEmptyArchives.isEmpty()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Please select at least one archive file");
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_ARCHIVE_FOLDER;
            log.atInfo().addArgument(uploadUrl).addArgument(nonEmptyArchives.size()).addArgument(studyYear).log("Archive folder upload request: url='{}', files={}, studyYear={}");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (MultipartFile archive : nonEmptyArchives) {
                body.add("archives", new MultipartFileResource(archive));
            }
            body.add("studyYear", studyYear.name());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);
            ResponseEntity<ArchiveFolderUploadResultDto> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, ArchiveFolderUploadResultDto.class);

            ArchiveFolderUploadResultDto result = response.getBody();
            if (result == null) {
                model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Folder upload finished with empty response");
                return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
            }

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, buildFolderStatusMessage(result));
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;

        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("403 Forbidden on archive folder upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.atError().setCause(e).addArgument(e.getStatusCode()).addArgument(e.getMessage()).log("HTTP error on archive folder upload: {} - {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive folder upload failed: " + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        } catch (IOException e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("IOException on archive folder upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Archive folder upload failed due to file read error: " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Archive folder upload failed: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Folder upload failed with unexpected error: " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }
    }

    @PostMapping("/xml-file")
    public String handleXmlUpload(@RequestParam("xml") MultipartFile xml, @RequestParam("courseId") String courseId,
                                  @RequestParam("questionBank") String questionBank, @RequestParam("studyYear") StudyYear studyYear,
                                  Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        populateArchiveUploadModel(model);

        if (xml == null || xml.isEmpty()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "XML file is required");
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
        if (courseId == null || courseId.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Course ID is required");
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
        if (questionBank == null || questionBank.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "QuestionBank name is required");
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
        if (studyYear == null) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, INVALID_STUDY_YEAR_MESSAGE);
            return ControllerSettings.VIEW_UPLOAD_XML;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_XML;

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("xml", new MultipartFileResource(xml));
            body.add("courseId", Long.valueOf(courseId));
            body.add("questionBankName", questionBank);
            body.add("studyYear", studyYear.name());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "XML uploaded successfully. " + response.getBody());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("403 Forbidden on XML upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "Session expired. Please log in again.");
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.atError().setCause(e).addArgument(e.getStatusCode()).addArgument(e.getMessage()).log("HTTP error on XML upload: {} - {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "XML upload failed: " + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_XML;
        } catch (IOException e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("IOException on XML upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "XML upload failed due to file read error: " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_XML;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Unexpected error on XML upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, "XML upload failed with unexpected error: " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
    }

    private String buildFolderStatusMessage(ArchiveFolderUploadResultDto result) {
        StringBuilder message = new StringBuilder();
        message.append("Folder processing started. Total archives: ").append(result.getTotalArchives()).append("\n");

        for (ArchiveFolderItemDto item : result.getItems()) {
            message.append(item.getIndex()).append("/").append(item.getTotal()).append(" - ").append(item.getArchiveName()).append(" [").append(item.getStatus()).append("]");
            if (item.getMessage() != null && !item.getMessage().isBlank()) {
                message.append(" - ").append(item.getMessage());
            }
            message.append("\n");
        }

        message.append("Ready. Processed ").append(result.getProcessedArchives()).append(" archive(s) from ").append(result.getTotalArchives()).append(" (Skipped: ").append(
                result.getSkippedArchives()).append(", Failed: ").append(result.getFailedArchives()).append(").");
        return message.toString();
    }
}
