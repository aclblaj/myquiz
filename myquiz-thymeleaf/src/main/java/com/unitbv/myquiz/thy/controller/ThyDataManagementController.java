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
    private static final String ATTR_ERROR = "error";
    private static final String REDIRECT_DATA_DASHBOARD = "redirect:/admin/data";

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
        return "admin/data-management";
    }

    @GetMapping("/export-sql")
    public void exportSql(HttpServletResponse response) throws IOException {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            response.sendRedirect("/auth/login");
            return;
        }

        HttpEntity<Void> request = sessionService.createAuthorizedRequest();
        try {
            ResponseEntity<byte[]> apiResponse = restTemplate.exchange(apiBaseUrl + "/data/export-sql", HttpMethod.GET, request, byte[].class);

            response.setContentType("application/sql; charset=UTF-8");
            String disposition = apiResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition != null ? disposition : "attachment; filename=myquiz_backup.sql");

            byte[] body = apiResponse.getBody();
            if (body != null) {
                StreamUtils.copy(body, response.getOutputStream());
            }
        } catch (HttpClientErrorException.Forbidden e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        } catch (Exception e) {
            log.atError().setCause(e).log("SQL export failed");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Export failed");
        }
    }

    @PostMapping("/import-sql")
    public String importSql(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Please select a SQL file to import.");
            return REDIRECT_DATA_DASHBOARD;
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartFileResource(file));

            HttpEntity<MultiValueMap<String, Object>> request = sessionService.createMultipartRequest(body);
            ResponseEntity<String> apiResponse = restTemplate.exchange(apiBaseUrl + "/data/import-sql", HttpMethod.POST, request, String.class);

            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, apiResponse.getBody() != null ? apiResponse.getBody() : "SQL backup imported successfully");
        } catch (HttpClientErrorException.Forbidden e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Access denied for restore operation.");
        } catch (HttpClientErrorException.BadRequest e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Invalid SQL backup file.");
        } catch (Exception e) {
            log.atError().setCause(e).log("SQL import failed");
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Import failed: " + e.getMessage());
        }

        return REDIRECT_DATA_DASHBOARD;
    }

    @GetMapping("/deleteall")
    public String deleteAllData(RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            return redirect;
        }

        try {
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            restTemplate.exchange(apiBaseUrl + "/data/deleteall", HttpMethod.DELETE, request, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_MESSAGE, "All questionBank data deleted successfully. Users, roles, and permissions were preserved.");
        } catch (HttpClientErrorException.Forbidden e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Access denied for cleanup operation.");
        } catch (Exception e) {
            log.atError().setCause(e).log("Delete-all failed");
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Delete all failed: " + e.getMessage());
        }

        return REDIRECT_DATA_DASHBOARD;
    }
}



