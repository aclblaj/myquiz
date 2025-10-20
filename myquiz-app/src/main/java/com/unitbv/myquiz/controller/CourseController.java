package com.unitbv.myquiz.controller;

import com.unitbv.myquiz.services.CourseService;
import com.unitbv.myquizapi.dto.CourseDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping(value="/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

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

    @GetMapping("/edit/{id}")
    public String editCourseForm(@PathVariable("id") Long id, Model model) {
        var courseDto = courseService.findById(id);
        model.addAttribute("course", courseDto);
        return "course-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateCourse(@PathVariable("id") Long id,
                               @ModelAttribute("course") com.unitbv.myquizapi.dto.CourseDto courseDto,
                               RedirectAttributes redirectAttributes) {
        courseService.updateCourse(id, courseDto);
        redirectAttributes.addFlashAttribute("message", "Course updated successfully.");
        return "redirect:/courses/";
    }

    @GetMapping("/new")
    public String newCourseForm(Model model) {
        model.addAttribute("course", new CourseDto());
        return "course-edit"; // Reuse the edit template for creation
    }

    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable("id") Long id, Model model) {
        var courseDto = courseService.findById(id);
        model.addAttribute("course", courseDto);
        return "course-details";
    }
}
