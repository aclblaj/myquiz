package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotEmpty(message = "At least one duplicate question ID is required")
    private List<@NotNull(message = "Duplicate question ID cannot be null") @Positive(message = "Duplicate question ID must be positive") Long> duplicateQuestionIds = new ArrayList<>();
}
