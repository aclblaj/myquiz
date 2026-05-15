package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BasePaginationDto {
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
}

