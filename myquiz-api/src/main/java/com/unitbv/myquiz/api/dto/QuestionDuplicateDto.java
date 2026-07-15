package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unitbv.myquiz.api.types.QuestionType;
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

    @JsonProperty("id")
    private Long id;

    @JsonProperty("duplicateLinkId")
    private Long duplicateLinkId;

    @JsonProperty("question1Id")
    private Long question1Id;

    @JsonProperty("question1Title")
    private String question1Title;

    @JsonProperty("question2Id")
    private Long question2Id;

    @JsonProperty("question2Title")
    private String question2Title;

    @JsonProperty("type1")
    private QuestionType type1;

    @JsonProperty("response1Q1")
    private String response1Q1;

    @JsonProperty("response2Q1")
    private String response2Q1;

    @JsonProperty("response3Q1")
    private String response3Q1;

    @JsonProperty("response4Q1")
    private String response4Q1;

    @JsonProperty("type2")
    private QuestionType type2;

    @JsonProperty("response1Q2")
    private String response1Q2;

    @JsonProperty("response2Q2")
    private String response2Q2;

    @JsonProperty("response3Q2")
    private String response3Q2;

    @JsonProperty("response4Q2")
    private String response4Q2;

    @JsonProperty("similarity")
    private String similarity;

    @JsonProperty("questionId")
    private Long questionId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("text")
    private String text;

    @JsonProperty("response1")
    private String response1;

    @JsonProperty("response2")
    private String response2;

    @JsonProperty("response3")
    private String response3;

    @JsonProperty("response4")
    private String response4;

    @JsonProperty("course")
    private String course;

    @JsonProperty("questionBankName")
    private String questionBankName;

    @JsonProperty("author")
    private AuthorInfo author;

    @JsonProperty("row")
    private Integer row;

    @JsonProperty("type")
    private QuestionType type;

    @JsonProperty("cause")
    private String cause;

    @JsonProperty("status")
    private String status;
}
