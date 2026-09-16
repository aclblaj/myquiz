package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unitbv.myquiz.api.types.StudyYear;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "name", "course", "studyYear"})
@EqualsAndHashCode(of = "id")
public class QuestionBankDto {

    @Schema(description = "Unique identifier of the question bank")
    private Long id;

    @Schema(description = "Question bank name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Course name")
    private String course;

    @Schema(description = "Course ID")
    private Long courseId;

    @Schema(description = "Question bank study year")
    @JsonProperty("study_year")
    @JsonAlias("studyYear")
    private StudyYear studyYear;

    @Schema(description = "Source file")
    private String sourceFile;

    @Schema(description = "Question bank author ID")
    private Long questionBankAuthorId;

    @Schema(description = "Number of authors")
    private Integer noAuthors;

    @Schema(description = "Multiple choice questions")
    @Builder.Default
    private List<QuestionDto> questionsMultichoice = new ArrayList<>();

    @Schema(description = "True/false questions")
    @Builder.Default
    private List<QuestionDto> questionsTruefalse = new ArrayList<>();

    @Schema(description = "Question error DTOs")
    @Builder.Default
    private List<QuestionErrorDto> questionErrorDtos = new ArrayList<>();

    @Schema(description = "Authors")
    @Builder.Default
    private List<AuthorDto> authors = new ArrayList<>();

    @Schema(description = "Number of MC questions")
    private int mcQuestionsCount;

    @Schema(description = "Number of TF questions")
    private int tfQuestionsCount;

    @Schema(description = "Number of duplicated questions in this question bank")
    @Builder.Default
    private Long numberOfDuplicates = 0L;

    /** Convert list of raw Object[] rows from a projection query to lightweight DTOs. */
    public static List<QuestionBankDto> toDtoList(List<Object[]> questionBankData) {
        List<QuestionBankDto> dtos = new ArrayList<>();
        if (questionBankData == null) {
            return dtos;
        }
        for (Object[] data : questionBankData) {
            if (data != null && data.length >= 4) {
                QuestionBankDto dto = new QuestionBankDto();
                dto.setId((Long) data[0]);
                dto.setName((String) data[1]);
                dto.setCourse((String) data[2]);
                dto.setStudyYear((StudyYear) data[3]);
                dtos.add(dto);
            }
        }
        return dtos;
    }

    /** Derives lightweight {@link AuthorInfo} items from the embedded {@code authors} list. */
    public List<AuthorInfo> getAuthorInfos() {
        List<AuthorInfo> authorInfos = new ArrayList<>();
        if (authors != null) {
            for (AuthorDto authorDto : authors) {
                if (authorDto != null) {
                    authorInfos.add(new AuthorInfo(authorDto.getId(), authorDto.getName(), authorDto.getInitials()));
                }
            }
        }
        return authorInfos;
    }
}
