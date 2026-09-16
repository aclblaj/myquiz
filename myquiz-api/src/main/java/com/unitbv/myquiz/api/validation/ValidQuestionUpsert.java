package com.unitbv.myquiz.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Validates relationships between question type, bank reference and answers. */
@Documented
@Constraint(validatedBy = QuestionUpsertValidator.class)
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValidQuestionUpsert {
    String message() default "Question data is inconsistent";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
