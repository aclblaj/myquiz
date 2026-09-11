package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.interfaces.SessionApi;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session management controller.
 * HttpSession remains an implementation detail while the endpoint contract is defined by SessionApi.
 */
@RestController
@RequestMapping("/api/session")
public class SessionController implements SessionApi {
    private static final Logger log = LoggerFactory.getLogger(SessionController.class);
    // Use constants from ControllerSettings for session keys
    private static final String SELECTED_AUTHOR_KEY = ControllerSettings.ATTR_SELECTED_AUTHOR;
    private static final String SELECTED_COURSE_KEY = ControllerSettings.ATTR_SELECTED_COURSE;
    private final ObjectProvider<HttpSession> sessionProvider;

    public SessionController(ObjectProvider<HttpSession> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /**
     * Set selected author ID in session.
     * Endpoint: POST /api/session/author
     */
    @Override
    public ResponseEntity<Void> setSelectedAuthor(@RequestParam("authorId") Long authorId) {
        HttpSession session = sessionProvider.getObject();
        log.info("Setting selected author in session: {}", authorId);
        session.setAttribute(SELECTED_AUTHOR_KEY, authorId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get selected author ID from session.
     * Endpoint: GET /api/session/author
     */
    @Override
    public ResponseEntity<Long> getSelectedAuthor() {
        HttpSession session = sessionProvider.getObject();
        Object authorId = session.getAttribute(SELECTED_AUTHOR_KEY);
        Long result = authorId instanceof Long longValue ? longValue : null;
        log.info("Getting selected author from session: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Set selected course in session.
     * Endpoint: POST /api/session/course
     */
    @Override
    public ResponseEntity<Void> setSelectedCourse(@RequestParam("course") String course) {
        HttpSession session = sessionProvider.getObject();
        log.info("Setting selected course in session: {}", course);
        session.setAttribute(SELECTED_COURSE_KEY, course);
        return ResponseEntity.ok().build();
    }

    /**
     * Get selected course from session.
     * Endpoint: GET /api/session/course
     */
    @Override
    public ResponseEntity<String> getSelectedCourse() {
        HttpSession session = sessionProvider.getObject();
        Object course = session.getAttribute(SELECTED_COURSE_KEY);
        String result = course instanceof String stringValue ? stringValue : null;
        log.info("Getting selected course from session: {}", result);
        return ResponseEntity.ok(result);
    }
}
