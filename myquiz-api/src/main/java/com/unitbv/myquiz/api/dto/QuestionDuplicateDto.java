package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.ResolutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO representing a duplicate question relationship")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDuplicateDto {

    private Long id;

    private Long duplicateLinkId;

    private Long question1Id;

    private String question1Title;

    private Long question2Id;

    private String question2Title;

    private QuestionType type1;

    private String response1Q1;

    private String response2Q1;

    private String response3Q1;

    private String response4Q1;

    private QuestionType type2;

    private String response1Q2;

    private String response2Q2;

    private String response3Q2;

    private String response4Q2;

    private String similarity;

    private Long questionId;

    private String title;

    private String text;

    private String response1;

    private String response2;

    private String response3;

    private String response4;

    private String course;

    private String questionBankName;

    private AuthorInfo author;

    private Integer row;

    private QuestionType type;

    private String cause;

    private ResolutionStatus status;

    /** Keeps source compatibility for legacy internal mappers. */
    public void setStatus(String status) {
        this.status = ResolutionStatus.fromValue(status);
    }
}
