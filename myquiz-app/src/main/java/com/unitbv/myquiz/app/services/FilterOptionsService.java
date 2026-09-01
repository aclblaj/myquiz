package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared service for resolving filter dropdown options (currently authors)
 * used by both question-list and author-list filtering endpoints.
 * <p>
 * Resolution rules:
 * 1. If a question bank is selected, only authors that contributed to that
 *    question bank are returned.
 * 2. Otherwise, if a course is selected, only authors that contributed to
 *    that course are returned.
 * 3. Otherwise, all authors are returned.
 */
@Service
@RequiredArgsConstructor
public class FilterOptionsService {
    private static final Logger log = LoggerFactory.getLogger(FilterOptionsService.class);

    private final AuthorService authorService;
    private final QuestionBankService questionBankService;
    private final QuestionBankAuthorService questionBankAuthorService;

    /**
     * Resolves the list of authors that should be displayed in the Author
     * filter dropdown, scoped by the given question bank and/or course selection.
     *
     * @param questionBankId the selected question bank id, or null if none selected
     * @param selectedCourse the selected course name, or null if none selected
     * @return list of author options matching the current filter scope
     */
    public List<AuthorInfo> resolveAuthorOptions(Long questionBankId, String selectedCourse) {
        if (questionBankId != null) {
            List<AuthorInfo> byQuestionBank = resolveAuthorsByQuestionBank(questionBankId);
            if (byQuestionBank != null) {
                return byQuestionBank;
            }
        }

        if (selectedCourse != null && !selectedCourse.isBlank()) {
            List<AuthorInfo> byCourse = questionBankAuthorService.getAuthorDtosByCourse(selectedCourse);
            return byCourse != null ? byCourse : new ArrayList<>();
        }

        List<AuthorInfo> all = authorService.getAllAuthorsBasic();
        return all != null ? all : new ArrayList<>();
    }

    private List<AuthorInfo> resolveAuthorsByQuestionBank(Long questionBankId) {
        try {
            QuestionBankDto questionBankDto = questionBankService.getQuestionBankById(questionBankId);
            if (questionBankDto == null || questionBankDto.getAuthors() == null || questionBankDto.getAuthors().isEmpty()) {
                return new ArrayList<>();
            }
            return questionBankDto.getAuthors().stream()
                    .map(author -> new AuthorInfo(author.getId(), author.getName(), author.getInitials()))
                    .toList();
        } catch (Exception e) {
            log.warn("Unable to resolve authors for questionBankId {}", questionBankId, e);
            return new ArrayList<>();
        }
    }
}
