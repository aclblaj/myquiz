package com.unitbv.myquiz.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
@RestController
@RequestMapping("/api/session")
public class SessionController {
    private static final String SELECTED_AUTHOR_KEY = "selectedAuthor";
    private static final String SELECTED_COURSE_KEY = "selectedCourse";

    /**
     * Creates a new SessionController to manage the selectedAuthor global variable using HttpSession.
     */
    @PostMapping("/author")
    public void setSelectedAuthor(@RequestParam("authorId") Long authorId, HttpSession session) {
        session.setAttribute(SELECTED_AUTHOR_KEY, authorId);
    }

    @GetMapping("/author")
    public Long getSelectedAuthor(HttpSession session) {
        Object authorId = session.getAttribute(SELECTED_AUTHOR_KEY);
        return authorId instanceof Long ? (Long) authorId : null;
    }

    @PostMapping("/course")
    public void setSelectedCourse(@RequestParam("course") String course, HttpSession session) {
        session.setAttribute(SELECTED_COURSE_KEY, course);
    }

    @GetMapping("/course")
    public String getSelectedCourse(HttpSession session) {
        Object course = session.getAttribute(SELECTED_COURSE_KEY);
        return course instanceof String ? (String) course : null;
    }
}