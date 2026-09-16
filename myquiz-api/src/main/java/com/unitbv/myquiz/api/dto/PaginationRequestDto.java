package com.unitbv.myquiz.api.dto;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Common pagination input. Response-only metadata does not belong in requests. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequestDto {

    @Schema(description = "1-based page number", minimum = "1", defaultValue = "1")
    @NotNull(message = "Page is required")
    @Min(value = 1, message = "Page must be at least 1")
    @Builder.Default
    private Integer page = 1;

    @Schema(description = "Number of items per page", minimum = "1", maximum = "100", defaultValue = "10")
    @NotNull(message = "Page size is required")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = ControllerSettings.MAX_PAGE_SIZE, message = "Page size cannot exceed {value}")
    @Builder.Default
    private Integer pageSize = ControllerSettings.PAGE_SIZE;
}
