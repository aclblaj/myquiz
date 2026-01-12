package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.thy.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thymeleaf controller for home and general application pages.
 * Handles homepage navigation, help pages, and utility pages.
 * Provides server-side rendering for main application navigation.
 */
@Controller
@SessionAttributes(ControllerSettings.ATTR_LOGGED_IN_USER)
public class ThyHomeController {
    private static final Logger logger = LoggerFactory.getLogger(ThyHomeController.class);

    private final SessionService sessionService;

    @Autowired
    public ThyHomeController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping({"/", ""})
    public String home() {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) {
            logger.warn("Invalid session on home page access");
            return redirect;
        }

        Object loggedInUser = sessionService.getLoggedInUser();
        logger.info("Home page requested by user: {}", loggedInUser);
        logger.info("Redirecting to quiz list for home page");
        return ControllerSettings.VIEW_REDIRECT_QUIZ;
    }

    @GetMapping("/help")
    public String about() {
        return "help";
    }

    @GetMapping("/questions/deleteall")
    public String deleteAllQuestions(Model model) {
        model.addAttribute("message", "All questions deleted (stub)");
        return "success";
    }


}
