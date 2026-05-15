package com.unitbv.myquiz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Canonical DTO used to populate author-focused views and forms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorFormDataDto {
	private List<QuestionBankDto> questionBankDtos;
	private List<AuthorInfo> authorsList;
	private AuthorDto author;
}


