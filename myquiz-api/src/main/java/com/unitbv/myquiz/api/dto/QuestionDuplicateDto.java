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

    @JsonProperty("duplicateLinkId")
    private Long duplicateLinkId;

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

    @JsonProperty("authorName")
    private String authorName;

    @JsonProperty("row")
    private Integer row;

    @JsonProperty("type")
    private QuestionType type;
}
