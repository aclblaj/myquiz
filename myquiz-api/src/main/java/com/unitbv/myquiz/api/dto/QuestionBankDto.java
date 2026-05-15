package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unitbv.myquiz.api.types.StudyYear;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Question Bank DTO for question bank management")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"id", "name", "course", "studyYear"})
@EqualsAndHashCode(of = "id")
public class QuestionBankDto {

    @Schema(description = "Unique identifier of the question bank")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Question bank name", required = true)
    @NotBlank(message = "Question bank name cannot be blank")
    @Size(max = 255, message = "Question bank name cannot exceed 255 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Course name")
    @JsonProperty("course")
    private String course;

    @Schema(description = "Course ID")
    @JsonProperty("courseId")
    private Long courseId;

    @Schema(description = "Question bank study year")
    @JsonProperty("study_year")
    private StudyYear studyYear;

    @Schema(description = "Source file")
    @JsonProperty("sourceFile")
    private String sourceFile;

    @Schema(description = "Question bank author ID")
    @JsonProperty("questionBankAuthorId")
    private Long questionBankAuthorId;

    @Schema(description = "Number of authors")
    @JsonProperty("noAuthors")
    private Integer noAuthors;

    @Schema(description = "Multiple choice questions")
    @JsonProperty("questionsMultichoice")
    private List<QuestionDto> questionsMultichoice = new ArrayList<>();

    @Schema(description = "True/false questions")
    @JsonProperty("questionsTruefalse")
    private List<QuestionDto> questionsTruefalse = new ArrayList<>();

    @Schema(description = "Question error DTOs")
    @JsonProperty("questionErrorDtos")
    private List<QuestionErrorDto> questionErrorDtos;

    @Schema(description = "Authors")
    @JsonProperty("authors")
    private List<AuthorDto> authors;

    @Schema(description = "Number of MC questions")
    @JsonProperty("mcQuestionsCount")
    private int mcQuestionsCount;

    @Schema(description = "Number of TF questions")
    @JsonProperty("tfQuestionsCount")
    private int tfQuestionsCount;

    @Schema(description = "Number of duplicated questions in this question bank")
    @JsonProperty("numberOfDuplicates")
    private Long numberOfDuplicates = 0L;

    /** Convert list of raw Object[] rows from a projection query to lightweight DTOs. */
    public static List<QuestionBankDto> toDtoList(List<Object[]> questionBankData) {
        List<QuestionBankDto> dtos = new ArrayList<>();
        for (Object[] data : questionBankData) {
            QuestionBankDto dto = new QuestionBankDto();
            dto.setId((Long) data[0]);
            dto.setName((String) data[1]);
            dto.setCourse((String) data[2]);
            dto.setStudyYear((StudyYear) data[3]);
            dtos.add(dto);
        }
        return dtos;
    }

    /** Derives lightweight {@link AuthorInfo} items from the embedded {@code authors} list. */
    public List<AuthorInfo> getAuthorInfos() {
        List<AuthorInfo> authorInfos = new ArrayList<>();
        if (authors != null) {
            for (AuthorDto authorDto : authors) {
                authorInfos.add(new AuthorInfo(authorDto.getId(), authorDto.getName(), authorDto.getInitials()));
            }
        }
        return authorInfos;
    }
}
