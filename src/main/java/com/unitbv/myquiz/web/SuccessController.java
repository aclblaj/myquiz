package com.unitbv.myquiz.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SuccessController {
    @GetMapping("/success")
    public String showSuccess(Model model) {
        if (!model.containsAttribute("message")) {
            model.addAttribute("message", "No news is good news!");
        }
        return "success";
    }
}