package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorErrorFilterDto {
    private String course;
    private String authorName;
    private List<AuthorErrorDto> authorErrors;
    private ArrayList<String> authorNames;
    private List<String> courses;
    private Map<String, List<AuthorErrorDto>> authorErrorsByAuthor;
    private Integer page;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
}
