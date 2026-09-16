package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.types.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.unitbv.myquiz.api.validation.ValidQuestionUpsert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request-only contract for creating or updating a question. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Question data accepted when creating or updating a question")
@ValidQuestionUpsert
public class QuestionUpsertDto {

    @Size(max = 500, message = "Question title cannot exceed 500 characters")
    private String title;

    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 1000, message = "Question text cannot exceed 1000 characters")
    private String text;

    @Size(max = 2000, message = "Answer reference cannot exceed 2000 characters")
    private String answerReferenceText;

    @Size(max = 255, message = "Chapter cannot exceed 255 characters")
    private String chapter;

    @Valid
    private AuthorUpsertDto author;

    @Size(max = 255, message = "Question bank name cannot exceed 255 characters")
    private String questionBankName;

    @Positive(message = "Question bank ID must be positive")
    private Long questionBankId;

    @Size(max = 1000, message = "Response 1 cannot exceed 1000 characters")
    private String response1;
    @Size(max = 1000, message = "Response 2 cannot exceed 1000 characters")
    private String response2;
    @Size(max = 1000, message = "Response 3 cannot exceed 1000 characters")
    private String response3;
    @Size(max = 1000, message = "Response 4 cannot exceed 1000 characters")
    private String response4;

    @DecimalMin(value = "-100.0", message = "Response weight cannot be below -100")
    @DecimalMax(value = "100.0", message = "Response weight cannot exceed 100")
    private Double weightResponse1;
    @DecimalMin(value = "-100.0", message = "Response weight cannot be below -100")
    @DecimalMax(value = "100.0", message = "Response weight cannot exceed 100")
    private Double weightResponse2;
    @DecimalMin(value = "-100.0", message = "Response weight cannot be below -100")
    @DecimalMax(value = "100.0", message = "Response weight cannot exceed 100")
    private Double weightResponse3;
    @DecimalMin(value = "-100.0", message = "Response weight cannot be below -100")
    @DecimalMax(value = "100.0", message = "Response weight cannot exceed 100")
    private Double weightResponse4;
    @DecimalMin(value = "-100.0", message = "True weight cannot be below -100")
    @DecimalMax(value = "100.0", message = "True weight cannot exceed 100")
    private Double weightTrue;
    @DecimalMin(value = "-100.0", message = "False weight cannot be below -100")
    @DecimalMax(value = "100.0", message = "False weight cannot exceed 100")
    private Double weightFalse;

    @PositiveOrZero(message = "Row cannot be negative")
    private Integer row;

    @NotBlank(message = "Course cannot be blank")
    @Size(max = 255, message = "Course cannot exceed 255 characters")
    private String course;

    @NotNull(message = "Question type is required")
    private QuestionType type;

}
