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

        populateArchiveUploadModel(model);
        return ControllerSettings.VIEW_UPLOAD_EXCEL;
    }

    @GetMapping("/archive-form")
    public String uploadArchive(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        populateArchiveUploadModel(model);
        return ControllerSettings.VIEW_UPLOAD_ARCHIVE;
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

    /**
     * Populates upload views with common dropdown data (courses, templates, study years, logged user).
     */
    private void populateArchiveUploadModel(Model model) {
        String coursesUrl = apiBaseUrl + ControllerSettings.API_COURSES;
        HttpEntity<Void> entity = sessionService.getAuthorizationHeader();

        ResponseEntity<List<Object>> coursesResponse = restTemplate.exchange(
                coursesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                }
        );
        List<Object> courses = coursesResponse.getBody();
        model.addAttribute(ControllerSettings.ATTR_COURSES, courses != null ? courses : List.of());
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());

        List<String> templates = List.of(TemplateType.getAllTypesAsStringArray());
        log.atInfo().addArgument(templates).log("Available templates: {}");
        model.addAttribute(ControllerSettings.ATTR_TEMPLATES, templates);
        model.addAttribute(ControllerSettings.ATTR_STUDY_YEARS, StudyYear.values());
    }

    @PostMapping("/excel-file")
    public String handleExcelUpload(@RequestParam("file") MultipartFile file, @RequestParam("username") String username, @RequestParam("courseId") String courseId, @RequestParam("name") String name,
                                    @RequestParam("template") String template, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_EXCEL;

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(ControllerSettings.FIELD_FILE, new MultipartFileResource(file));
            body.add(ControllerSettings.FIELD_USERNAME, username);
            body.add(ControllerSettings.ATTR_COURSE_ID, Long.valueOf(courseId));
            body.add(ControllerSettings.FIELD_NAME, name);
            body.add(ControllerSettings.FIELD_TEMPLATE, template);

            // Create multipart request with authorization
            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);

            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_EXCEL_SUCCESS_PREFIX + response.getBody());
        } catch (HttpClientErrorException.Forbidden ex) {
            log.atError().log("403 Forbidden on excel upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Excel file upload failed: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_EXCEL_FAILED_PREFIX + e.getMessage());
        }
        return ControllerSettings.VIEW_SUCCESS;
    }

    @PostMapping("/archive-file")
    public String handleArchiveUpload(@RequestParam("archive") MultipartFile archive, @RequestParam(ControllerSettings.ATTR_COURSE_ID) String courseId, @RequestParam("questionBank") String questionBank,
                                      @RequestParam(ControllerSettings.ATTR_STUDY_YEAR) StudyYear studyYear, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        String validationError = validateArchiveUploadRequest(archive, courseId, questionBank, studyYear);
        if (validationError != null) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, validationError);
            return ControllerSettings.VIEW_SUCCESS;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_ARCHIVE;
            log.atInfo().addArgument(uploadUrl).log("Upload URL: {}");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(buildArchiveUploadBody(archive, courseId, questionBank, studyYear));
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_SUCCESS_PREFIX + response.getBody());
            return ControllerSettings.VIEW_SUCCESS;

        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("403 Forbidden on archive upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.atError().setCause(e).addArgument(e.getStatusCode()).addArgument(e.getMessage()).log("HTTP error on archive upload: {} - {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FAILED_PREFIX + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (IOException e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("IOException on archive upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_IO_FAILED_PREFIX + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Unexpected error on archive upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_UNEXPECTED_FAILED_PREFIX + e.getMessage());
            return ControllerSettings.VIEW_SUCCESS;
        }
    }

    /**
     * Validates the archive upload request and returns an error message when invalid.
     */
    private String validateArchiveUploadRequest(MultipartFile archive, String courseId, String questionBank, StudyYear studyYear) {
        if (archive == null || archive.isEmpty()) {
            return ControllerSettings.MSG_UPLOAD_ARCHIVE_REQUIRED;
        }
        if (courseId == null || courseId.isBlank()) {
            return ControllerSettings.MSG_UPLOAD_COURSE_ID_REQUIRED;
        }
        if (questionBank == null || questionBank.isBlank()) {
            return ControllerSettings.MSG_UPLOAD_QUESTION_BANK_REQUIRED;
        }
        if (studyYear == null) {
            return ControllerSettings.MSG_UPLOAD_INVALID_STUDY_YEAR;
        }
        return null;
    }

    /**
     * Builds the multipart request body for archive upload.
     */
    private MultiValueMap<String, Object> buildArchiveUploadBody(MultipartFile archive, String courseId, String questionBank, StudyYear studyYear) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(ControllerSettings.FIELD_ARCHIVE, new MultipartFileResource(archive));
        body.add(ControllerSettings.ATTR_COURSE_ID, Long.valueOf(courseId));
        body.add(ControllerSettings.ATTR_QUESTION_BANK_NAME, questionBank);
        body.add(ControllerSettings.ATTR_STUDY_YEAR, studyYear.name());
        return body;
    }

    @PostMapping("/archive-folder")
    public String handleArchiveFolderUpload(@RequestParam("archives") MultipartFile[] archives, @RequestParam(ControllerSettings.ATTR_STUDY_YEAR) StudyYear studyYear, Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        populateArchiveUploadModel(model);

        if (archives == null || archives.length == 0) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FOLDER_SELECT);
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }
        if (studyYear == null) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_INVALID_STUDY_YEAR);
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }

        List<MultipartFile> nonEmptyArchives = Arrays.stream(archives).filter(file -> file != null && !file.isEmpty()).toList();
        if (nonEmptyArchives.isEmpty()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FOLDER_ONE_FILE);
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_ARCHIVE_FOLDER;
            log.atInfo().addArgument(uploadUrl).addArgument(nonEmptyArchives.size()).addArgument(studyYear).log("Archive folder upload request: url='{}', files={}, studyYear={}");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (MultipartFile archive : nonEmptyArchives) {
                body.add(ControllerSettings.FIELD_ARCHIVES, new MultipartFileResource(archive));
            }
            body.add(ControllerSettings.ATTR_STUDY_YEAR, studyYear.name());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);
            ResponseEntity<ArchiveFolderUploadResultDto> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, ArchiveFolderUploadResultDto.class);

            ArchiveFolderUploadResultDto result = response.getBody();
            if (result == null) {
                model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FOLDER_EMPTY_RESPONSE);
                return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
            }

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, buildFolderStatusMessage(result));
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;

        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("403 Forbidden on archive folder upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.atError().setCause(e).addArgument(e.getStatusCode()).addArgument(e.getMessage()).log("HTTP error on archive folder upload: {} - {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FOLDER_FAILED_PREFIX + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        } catch (IOException e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("IOException on archive folder upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FOLDER_IO_FAILED_PREFIX + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Archive folder upload failed: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_ARCHIVE_FOLDER_UNEXPECTED_FAILED_PREFIX + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_ARCHIVE_FOLDER;
        }
    }

    @PostMapping("/xml-file")
    public String handleXmlUpload(@RequestParam("xml") MultipartFile xml, @RequestParam(ControllerSettings.ATTR_COURSE_ID) String courseId,
                                  @RequestParam("questionBank") String questionBank, @RequestParam(ControllerSettings.ATTR_STUDY_YEAR) StudyYear studyYear,
                                  Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        populateArchiveUploadModel(model);

        if (xml == null || xml.isEmpty()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_XML_REQUIRED);
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
        if (courseId == null || courseId.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_COURSE_ID_REQUIRED);
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
        if (questionBank == null || questionBank.isBlank()) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_QUESTION_BANK_REQUIRED);
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
        if (studyYear == null) {
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_INVALID_STUDY_YEAR);
            return ControllerSettings.VIEW_UPLOAD_XML;
        }

        try {
            String uploadUrl = apiBaseUrl + ControllerSettings.API_UPLOAD_XML;

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(ControllerSettings.FIELD_XML, new MultipartFileResource(xml));
            body.add(ControllerSettings.ATTR_COURSE_ID, Long.valueOf(courseId));
            body.add(ControllerSettings.ATTR_QUESTION_BANK_NAME, questionBank);
            body.add(ControllerSettings.ATTR_STUDY_YEAR, studyYear.name());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = sessionService.createMultipartRequest(body);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);

            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_XML_SUCCESS_PREFIX + response.getBody());
            return ControllerSettings.VIEW_SUCCESS;
        } catch (HttpClientErrorException.Forbidden e) {
            log.atError().log("403 Forbidden on XML upload");
            sessionService.invalidateCurrentSession();
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_SESSION_EXPIRED_LOGIN_AGAIN);
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException e) {
            log.atError().setCause(e).addArgument(e.getStatusCode()).addArgument(e.getMessage()).log("HTTP error on XML upload: {} - {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_XML_FAILED_PREFIX + e.getStatusCode() + " - " + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_XML;
        } catch (IOException e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("IOException on XML upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_XML_IO_FAILED_PREFIX + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_XML;
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Unexpected error on XML upload: {}");
            model.addAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_UPLOAD_XML_UNEXPECTED_FAILED_PREFIX + e.getMessage());
            return ControllerSettings.VIEW_UPLOAD_XML;
        }
    }

    /**
     * Builds a human-readable status log for archive-folder upload processing.
     */
    private String buildFolderStatusMessage(ArchiveFolderUploadResultDto result) {
        StringBuilder message = new StringBuilder();
        message.append(ControllerSettings.MSG_UPLOAD_FOLDER_PROCESSING_STARTED_PREFIX).append(result.getTotalArchives()).append("\n");

        for (ArchiveFolderItemDto item : result.getItems()) {
            message.append(item.getIndex()).append("/").append(item.getTotal()).append(" - ").append(item.getArchiveName()).append(" [").append(item.getStatus()).append("]");
            if (item.getMessage() != null && !item.getMessage().isBlank()) {
                message.append(" - ").append(item.getMessage());
            }
            message.append("\n");
        }

        message.append(ControllerSettings.MSG_UPLOAD_FOLDER_READY_PREFIX).append(result.getProcessedArchives())
                .append(ControllerSettings.MSG_UPLOAD_FOLDER_READY_MIDDLE).append(result.getTotalArchives())
                .append(ControllerSettings.MSG_UPLOAD_FOLDER_READY_SKIPPED).append(result.getSkippedArchives())
                .append(ControllerSettings.MSG_UPLOAD_FOLDER_READY_FAILED).append(result.getFailedArchives())
                .append(ControllerSettings.MSG_UPLOAD_FOLDER_READY_SUFFIX);
        return message.toString();
    }
}
