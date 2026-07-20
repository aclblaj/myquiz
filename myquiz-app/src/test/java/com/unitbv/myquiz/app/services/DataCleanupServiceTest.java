package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.repositories.ArchiveImportRepository;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.DuplicateRecomputeHistoryRepository;
import com.unitbv.myquiz.app.repositories.CourseRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankAuthorRepository;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

    @Mock
    private QuestionErrorRepository questionErrorRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionBankAuthorRepository questionBankAuthorRepository;
    @Mock
    private QuestionBankRepository questionBankRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private QuestionDuplicateRepository questionDuplicateRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ArchiveImportRepository archiveImportRepository;
    @Mock
    private DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DataSource dataSource;

    private DataCleanupService service;

    @BeforeEach
    void setUp() {
        service = new DataCleanupService(
                questionErrorRepository,
                questionRepository,
                questionBankAuthorRepository,
                questionBankRepository,
                authorRepository,
                courseRepository,
                questionDuplicateRepository,
                restTemplate,
                archiveImportRepository,
                duplicateRecomputeHistoryRepository,
                jdbcTemplate,
                dataSource
        );
    }

    @Test
    void exportNonAuthDataAsSql_usesCurrentTableAndSequenceNames() {
        // Keep export focused on script structure; no row rendering required in this test.
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.eq(String.class), anyString())).thenReturn(List.of("id"));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());

        String sql = service.exportNonAuthDataAsSql();

        assertTrue(sql.contains("question_bank_author"));
        assertTrue(sql.contains("question"));
        assertTrue(sql.contains("answers_reference"));
        assertTrue(sql.contains("question_error"));
        assertTrue(sql.contains("archive_import"));
        assertTrue(sql.contains("question_duplicate"));
        assertTrue(sql.contains("duplicate_recompute_history"));
        assertTrue(sql.contains("question_bank_seq"));
        assertTrue(sql.contains("question_bank_author_seq"));
        assertTrue(sql.contains("question_seq"));
        assertTrue(sql.contains("answers_reference_seq"));
        assertTrue(sql.contains("question_error_seq"));
        assertTrue(sql.contains("archive_import_seq"));
        assertTrue(sql.contains("question_duplicate_seq"));
        assertTrue(sql.contains("dup_recompute_history_seq"));

        assertFalse(sql.contains("quiz_author"));
        assertFalse(sql.contains("question_bank_item"));
        assertFalse(sql.contains("quiz_seq"));
        assertFalse(sql.contains("quiz_author_seq"));
        assertFalse(sql.contains("question_bank_item_seq"));

    }

    @Test
    void importNonAuthDataFromSql_rejectsAuthTableModificationStatements() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "backup.sql",
                "application/sql",
                "UPDATE users SET username='changed';".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importNonAuthDataFromSql(file)
        );

        assertTrue(exception.getMessage().contains("must not modify auth tables"));
    }
}


