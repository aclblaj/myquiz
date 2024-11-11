package com.unitbv.myquiz.web;

import com.unitbv.myquiz.dto.CourseDto;
import com.unitbv.myquiz.services.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping(value="/courses")
public class CourseController {

    @Autowired
    CourseService courseService;

    @GetMapping(value="/")
    public String listAll(Model model) {
        List<CourseDto> coursesDto = courseService.getAllCourses();
        model.addAttribute("courses", coursesDto);
        return "course-list";
    }

    @GetMapping(value = "/{id}/delete")
    public String deleteCourse(
            @PathVariable(value = "id") Long id,
            RedirectAttributes redirectAttributes
    ) {
        courseService.deleteCourseById(id);
        redirectAttributes.addFlashAttribute("message", "Course successfully deleted: " + id);
        return "redirect:/success";
    }
}
