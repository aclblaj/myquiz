package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.unitbv.myquiz.api.types.StudyYear;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request contract for creating or updating a question bank. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Question bank data accepted when creating or updating a question bank")
public class QuestionBankUpsertDto {

    @NotBlank(message = "Question bank name cannot be blank")
    @Size(max = 200, message = "Question bank name cannot exceed 200 characters")
    @Schema(description = "Question bank name", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String name;

    @NotBlank(message = "Course cannot be blank")
    @Size(max = 200, message = "Course cannot exceed 200 characters")
    @Schema(description = "Course name", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String course;

    @Schema(description = "Question bank study year")
    @JsonAlias("study_year")
    private StudyYear studyYear;
}
