package com.unitbv.myquiz.app.upload.api.support;

import com.unitbv.myquiz.api.types.TemplateType;
import org.springframework.stereotype.Component;

@Component
public class TemplateTypeResolver {
    public static final String VALID_TEMPLATE_VALUES = "Template2023, Template2024, template2023, template2024";

    public TemplateType resolve(String template) {
        try {
            TemplateType templateType = TemplateType.fromType(template);
            if (templateType == null) {
                templateType = TemplateType.valueOf(template);
            }
            return templateType;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid template type: " + template + ". Valid values: " + VALID_TEMPLATE_VALUES, e);
        }
    }
}

