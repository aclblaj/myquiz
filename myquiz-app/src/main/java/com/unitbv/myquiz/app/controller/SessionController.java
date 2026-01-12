package com.unitbv.myquiz.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.unitbv.myquiz.api.settings.ControllerSettings;

/**
 * Session management controller.
 * Note: This controller manages HTTP session state and cannot directly implement SessionApi
 * because HttpSession is a servlet-specific implementation detail not suitable for API contracts.
 * However, it exposes the same endpoints as defined in SessionApi.
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {
    private static final Logger log = LoggerFactory.getLogger(SessionController.class);
    // Use constants from ControllerSettings for session keys
    private static final String SELECTED_AUTHOR_KEY = ControllerSettings.ATTR_SELECTED_AUTHOR;
    private static final String SELECTED_COURSE_KEY = ControllerSettings.ATTR_SELECTED_COURSE;

    /**
     * Set selected author ID in session.
     * Endpoint: POST /api/session/author
     */
    @PostMapping("/author")
    public ResponseEntity<Void> setSelectedAuthor(@RequestParam("authorId") Long authorId, HttpSession session) {
        log.info("Setting selected author in session: {}", authorId);
        session.setAttribute(SELECTED_AUTHOR_KEY, authorId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get selected author ID from session.
     * Endpoint: GET /api/session/author
     */
    @GetMapping("/author")
    public ResponseEntity<Long> getSelectedAuthor(HttpSession session) {
        Object authorId = session.getAttribute(SELECTED_AUTHOR_KEY);
        Long result = authorId instanceof Long longValue ? longValue : null;
        log.info("Getting selected author from session: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Set selected course in session.
     * Endpoint: POST /api/session/course
     */
    @PostMapping("/course")
    public ResponseEntity<Void> setSelectedCourse(@RequestParam("course") String course, HttpSession session) {
        log.info("Setting selected course in session: {}", course);
        session.setAttribute(SELECTED_COURSE_KEY, course);
        return ResponseEntity.ok().build();
    }

    /**
     * Get selected course from session.
     * Endpoint: GET /api/session/course
     */
    @GetMapping("/course")
    public ResponseEntity<String> getSelectedCourse(HttpSession session) {
        Object course = session.getAttribute(SELECTED_COURSE_KEY);
        String result = course instanceof String stringValue ? stringValue : null;
        log.info("Getting selected course from session: {}", result);
        return ResponseEntity.ok(result);
    }
}