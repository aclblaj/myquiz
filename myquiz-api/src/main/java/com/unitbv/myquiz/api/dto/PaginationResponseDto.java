package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Common calculated pagination metadata returned by list endpoints. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResponseDto {
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
}
