package com.unitbv.myquiz.api.validation;

import com.unitbv.myquiz.api.dto.QuestionUpsertDto;
import com.unitbv.myquiz.api.types.QuestionType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Cross-field validation for the request-only question write contract. */
public class QuestionUpsertValidator implements ConstraintValidator<ValidQuestionUpsert, QuestionUpsertDto> {

    @Override
    public boolean isValid(QuestionUpsertDto value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (value.getQuestionBankId() == null
                && (value.getQuestionBankName() == null || value.getQuestionBankName().isBlank())) {
            addViolation(context, "Either questionBankId or questionBankName must be provided", "questionBankId");
            valid = false;
        }

        if (value.getType() == QuestionType.MULTICHOICE
                && (!hasText(value.getResponse1()) || !hasText(value.getResponse2())
                || !hasText(value.getResponse3()) || !hasText(value.getResponse4()))) {
            addViolation(context, "A multiple-choice question requires responses 1-4", "type");
            valid = false;
        }

        if (value.getType() == QuestionType.TRUEFALSE && !hasText(value.getResponse1())) {
            addViolation(context, "A true/false question requires response 1", "type");
            valid = false;
        }

        return valid;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void addViolation(ConstraintValidatorContext context, String message, String property) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
