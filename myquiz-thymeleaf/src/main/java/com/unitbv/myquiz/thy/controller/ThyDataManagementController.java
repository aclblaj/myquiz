package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import com.unitbv.myquiz.thy.util.MultipartFileResource;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/data")
public class ThyDataManagementController {
    private static final Logger log = LoggerFactory.getLogger(ThyDataManagementController.class);

    private final SessionService sessionService;
    private final RestTemplate restTemplate;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyDataManagementController(SessionService sessionService, RestTemplate restTemplate) {
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }
        model.addAttribute(ControllerSettings.ATTR_LOGGED_IN_USER, sessionService.getLoggedInUser());
        return ControllerSettings.VIEW_DATA_MANAGEMENT;
    }

    @GetMapping("/export-sql")
    public void exportSql(HttpServletResponse response) throws IOException {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            response.sendRedirect(ControllerSettings.PATH_AUTH_LOGIN);
            return;
        }

        HttpEntity<Void> request = sessionService.createAuthorizedRequest();
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_DATA_EXPORT_SQL, HttpMethod.GET, request, byte[].class);
            writeSqlExportResponse(response, apiResponse);
        } catch (HttpClientErrorException.Forbidden e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ControllerSettings.MSG_ACCESS_DENIED);
        } catch (Exception e) {
            log.atError().setCause(e).log("SQL export failed");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ControllerSettings.MSG_EXPORT_FAILED);
        }
    }

    /**
     * Writes SQL export API response to the servlet output stream.
     */
    private void writeSqlExportResponse(HttpServletResponse response, ResponseEntity<byte[]> apiResponse) throws IOException {
        response.setContentType(ControllerSettings.CONTENT_TYPE_SQL_UTF8);
        String disposition = apiResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String defaultDisposition = ControllerSettings.HEADER_ATTACHMENT_FILENAME_PREFIX + ControllerSettings.FILE_MYQUIZ_BACKUP_SQL;
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition != null ? disposition : defaultDisposition);

        byte[] body = apiResponse.getBody();
        if (body != null) {
            StreamUtils.copy(body, response.getOutputStream());
        }
    }

    @PostMapping("/import-sql")
    public String importSql(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_SQL_FILE_REQUIRED);
            return ControllerSettings.VIEW_REDIRECT_ADMIN_DATA;
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> request = sessionService.createMultipartRequest(body);
            ResponseEntity<String> apiResponse = restTemplate.exchange(apiBaseUrl + ControllerSettings.API_DATA_IMPORT_SQL, HttpMethod.POST, request, String.class);

            redirectAttributes.addFlashAttribute(
                    ControllerSettings.ATTR_MESSAGE,
                    apiResponse.getBody() != null ? apiResponse.getBody() : ControllerSettings.MSG_SQL_BACKUP_IMPORTED_SUCCESS
            );
        } catch (HttpClientErrorException.Forbidden e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_ACCESS_DENIED_RESTORE);
        } catch (HttpClientErrorException.BadRequest e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_INVALID_SQL_BACKUP_FILE);
        } catch (Exception e) {
            log.atError().setCause(e).log("SQL import failed");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_IMPORT_FAILED_PREFIX + e.getMessage());
        }

        return ControllerSettings.VIEW_REDIRECT_ADMIN_DATA;
    }

    @GetMapping("/deleteall")
    public String deleteAllDataGet(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_DELETE_ALL_POST_ONLY);
        return ControllerSettings.VIEW_REDIRECT_ADMIN_DATA;
    }

    @PostMapping("/deleteall")
    public String deleteAllData(RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_DATA_DELETE_ALL, HttpMethod.DELETE, request, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, ControllerSettings.MSG_DATA_DELETED_SUCCESS);
        } catch (HttpClientErrorException.Forbidden e) {
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_ACCESS_DENIED_CLEANUP);
        } catch (Exception e) {
            log.atError().setCause(e).log("Delete-all failed");
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR, ControllerSettings.MSG_DELETE_ALL_FAILED_PREFIX + e.getMessage());
        }

        return ControllerSettings.VIEW_REDIRECT_ADMIN_DATA;
    }
}



