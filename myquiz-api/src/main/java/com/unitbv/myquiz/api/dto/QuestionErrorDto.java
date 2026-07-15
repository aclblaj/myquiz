package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO for question validation and listing errors.
 *
 * <p>Primary accessors: {@link #getId()} / {@link #setId(Long)} for the error ID,
 * {@link #getRow()} / {@link #setRow(Integer)} for the row number.
 */
@Schema(description = "Question error DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "errorCode", "row", "authorName"})
public class QuestionErrorDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("description")
    private String description;

    @JsonProperty("row")
    private Integer row;

    @JsonProperty("authorId")
    private Long authorId;

    @JsonProperty("authorName")
    private String authorName;

    @JsonProperty("questionBankName")
    private String questionBankName;

    @JsonProperty("questionBankId")
    private Long questionBankId;

    @JsonProperty("dateCreated")
    private java.util.Date dateCreated;

    @JsonProperty("status")
    private String status;

    @JsonProperty("timestamp")
    private java.time.OffsetDateTime timestamp;

    @JsonProperty("questionId")
    private Long questionId;

    @JsonProperty("questionType")
    private String questionType;

    /**
     * Convenience constructor used when mapping from a {@code QuestionError} entity.
     * Does <em>not</em> set {@code timestamp}, {@code dateCreated}, or {@code status} —
     * callers are expected to populate those fields explicitly when required.
     */
    public QuestionErrorDto(Long id, String description, Integer row) {
        this.id = id;
        this.description = description;
        this.row = row;
    }

    public String getAuthorInitials() {
        if (authorName == null || authorName.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : authorName.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }
}
