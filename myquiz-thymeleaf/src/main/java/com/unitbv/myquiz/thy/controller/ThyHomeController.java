package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Thymeleaf controller for home and general application pages.
 * Handles homepage navigation, help pages, and utility pages.
 * Provides server-side rendering for main application navigation.
 */
@Controller
@SessionAttributes(ControllerSettings.ATTR_LOGGED_IN_USER)
public class ThyHomeController {
    private static final Logger log = LoggerFactory.getLogger(ThyHomeController.class);

    private final SessionService sessionService;
    private final RestTemplate restTemplate;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiUrl;

    @Autowired
    public ThyHomeController(SessionService sessionService, RestTemplate restTemplate) {
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
    }

    @GetMapping({"/", ""})
    public String home() {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            log.atWarn().log("Invalid session on home page access");
            return redirect;
        }

        Object loggedInUser = sessionService.getLoggedInUser();
        log.atInfo().addArgument(loggedInUser).log("Home page requested by user: {}");
        log.atInfo().log("Redirecting to question-bank list for home page");
        return ControllerSettings.VIEW_REDIRECT_QUESTION_BANK;
    }

    @GetMapping("/help")
    public String about() {
        return ControllerSettings.VIEW_HELP;
    }

    @GetMapping("/home/deleteall")
    public String deleteAllQuestions() {
        return "redirect:/admin/data";
    }

    @GetMapping("/statistics")
    public String getStatistics(Model model) {
        try {
            log.atInfo().log("Fetching database statistics");

            // Create authorized request with JWT token
            HttpEntity<Void> request = sessionService.createAuthorizedRequest();
            if (request == null) {
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Session expired. Please log in again.");
                return "database-statistics";
            }

            // Call the API to get statistics
            String url = apiUrl + "/data/statistics";
            ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Long> statistics = response.getBody();
                model.addAttribute(ControllerSettings.ATTR_STATISTICS, statistics);
                log.atInfo().addArgument(statistics != null ? statistics.size() : 0).log("Statistics retrieved successfully: {} tables");
            } else {
                model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Unexpected response from server: " + response.getStatusCode());
                log.atWarn().addArgument(response.getStatusCode()).log("Unexpected response: {}");
            }

        } catch (Exception e) {
            log.atError().setCause(e).addArgument(e.getMessage()).log("Error fetching database statistics: {}");
            model.addAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, "Error fetching statistics: " + e.getMessage());
        }

        return "database-statistics";
    }


}
