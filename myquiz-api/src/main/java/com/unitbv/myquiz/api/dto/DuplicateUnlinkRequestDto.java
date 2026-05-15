package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical request DTO containing duplicate question IDs to unlink.
 */
@Schema(description = "DTO containing selected duplicate question IDs to remove")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DuplicateUnlinkRequestDto {

    @Schema(description = "IDs of duplicate questions to unlink from the source question")
    @JsonProperty("duplicateQuestionIds")
    private List<Long> duplicateQuestionIds = new ArrayList<>();
}
