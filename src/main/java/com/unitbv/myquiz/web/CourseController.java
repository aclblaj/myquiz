package com.unitbv.myquiz.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value="/courses")
public class CourseController {

    @GetMapping(value="/")
    public String home(Model model) {
        return "course-list";
    }
}
