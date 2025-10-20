package com.unitbv.myquiz.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ThymeleafTemplateRenderer {

    @Autowired
    private TemplateEngine templateEngine;

    public String renderTemplate(String templateName, Model model) {
        Context context = new Context();
        model.asMap().forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }
}
