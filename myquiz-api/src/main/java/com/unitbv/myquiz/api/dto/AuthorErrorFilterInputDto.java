package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorErrorFilterInputDto {
    private String selectedCourse;
    private String selectedAuthor;
    private Integer page;
    private Integer pageSize;
}

