package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.ResolutionStatus;
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

    private Long id;

    private String errorCode;

    private String message;

    private String description;

    private Integer row;

    private Long authorId;

    private String authorName;

    private String questionBankName;

    private Long questionBankId;

    private java.time.OffsetDateTime dateCreated;

    private ResolutionStatus status;

    private java.time.OffsetDateTime timestamp;

    private Long questionId;

    private QuestionType questionType;

    /** Keeps source compatibility for legacy internal mappers. */
    public void setStatus(String status) {
        this.status = ResolutionStatus.fromValue(status);
    }

    /** Keeps source compatibility for legacy internal mappers. */
    public void setQuestionType(String questionType) {
        this.questionType = QuestionType.fromValue(questionType);
    }

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
