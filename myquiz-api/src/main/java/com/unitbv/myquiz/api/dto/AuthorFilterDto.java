package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorFilterDto {
    private List<String> courses;
    private int pageNo;
    private int totalPages;
    private long totalItems;
    private List<AuthorDto> authors;
    private List<AuthorInfo> authorList;
    private String selectedCourse;
}
