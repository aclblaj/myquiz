package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for comprehensive data cleanup operations.
 * Handles deletion of all QuestionBank-related data while preserving
 * user management data (users, roles, permissions).
 */
@Service
public class DataCleanupService {
    private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);

    // Constants for permission and HTTP headers
    private static final String PERMISSION_VIEW_EXTENDED_STATISTICS = "VIEW_EXTENDED_STATISTICS";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String IAM_STATISTICS_ENDPOINT = "/api/auth/iam-statistics";
    private static final List<String> EXPORT_TABLES = List.of(
        "course",
        "author",
        "question_bank",
        "question_bank_author",
        "question",
        "answers_reference",
        "question_error",
        "archive_import",
        "question_duplicate",
        "duplicate_recompute_history"
    );
    private static final Set<String> AUTH_TABLE_NAMES = Set.of(
        "users",
        "roles",
        "permissions",
        "user_roles",
        "role_permissions"
    );
    private static final Pattern WRITE_STATEMENT_PATTERN = Pattern.compile(
        "(?is)^\\s*(insert\\s+into|update|delete\\s+from|truncate\\s+table|alter\\s+table|drop\\s+table|create\\s+table)\\b"
    );
    private static final Pattern SQL_LINE_COMMENT_PATTERN = Pattern.compile("(?m)--.*$");

    private final QuestionErrorRepository questionErrorRepository;
    private final QuestionRepository questionRepository;
    private final QuestionBankAuthorRepository questionBankAuthorRepository;
    private final QuestionBankRepository questionBankRepository;
    private final AuthorRepository authorRepository;
    private final CourseRepository courseRepository;
    private final RestTemplate restTemplate;
    private final QuestionDuplicateRepository questionDuplicateRepository;
    private final ArchiveImportRepository archiveImportRepository;
    private final DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${MYQUIZ_AUTH_URL:http://myquiz-auth:8090}")
    private String authUrl;

    /**
     * Custom exception for data cleanup failures.
     */
    public static class DataCleanupException extends RuntimeException {
        public DataCleanupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Autowired
    public DataCleanupService(
            QuestionErrorRepository questionErrorRepository,
            QuestionRepository questionRepository,
            QuestionBankAuthorRepository questionBankAuthorRepository,
            QuestionBankRepository questionBankRepository,
            AuthorRepository authorRepository,
            CourseRepository courseRepository,
            QuestionDuplicateRepository questionDuplicateRepository,
            RestTemplate restTemplate,
            ArchiveImportRepository archiveImportRepository,
            DuplicateRecomputeHistoryRepository duplicateRecomputeHistoryRepository,
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.questionErrorRepository = questionErrorRepository;
        this.questionRepository = questionRepository;
        this.questionBankAuthorRepository = questionBankAuthorRepository;
        this.questionBankRepository = questionBankRepository;
        this.authorRepository = authorRepository;
        this.courseRepository = courseRepository;
        this.questionDuplicateRepository = questionDuplicateRepository;
        this.restTemplate = restTemplate;
        this.archiveImportRepository = archiveImportRepository;
        this.duplicateRecomputeHistoryRepository = duplicateRecomputeHistoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Transactional(readOnly = true)
    public String exportNonAuthDataAsSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("-- MyQuiz data backup (non-auth only)\n");
        sql.append("-- Generated by DataCleanupService.exportNonAuthDataAsSql\n\n");
        sql.append("BEGIN;\n\n");
        sql.append("TRUNCATE TABLE ")
            .append(String.join(", ", EXPORT_TABLES))
            .append(" RESTART IDENTITY CASCADE;\n\n");

        for (String table : EXPORT_TABLES) {
            appendTableData(sql, table);
        }

        appendSequenceReset(sql, "course_seq", "course", "id");
        appendSequenceReset(sql, "author_seq", "author", "id");
        appendSequenceReset(sql, "question_bank_seq", "question_bank", "id");
        appendSequenceReset(sql, "question_bank_author_seq", "question_bank_author", "id");
        appendSequenceReset(sql, "question_seq", "question", "id");
        appendSequenceReset(sql, "answers_reference_seq", "answers_reference", "id");
        appendSequenceReset(sql, "question_error_seq", "question_error", "id");
        appendSequenceReset(sql, "archive_import_seq", "archive_import", "id");
        appendSequenceReset(sql, "question_duplicate_seq", "question_duplicate", "id");
        appendSequenceReset(sql, "dup_recompute_history_seq", "duplicate_recompute_history", "id");

        sql.append("\nCOMMIT;\n");
        return sql.toString();
    }

    @Transactional
    public void importNonAuthDataFromSql(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("SQL backup file is empty");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read SQL file", e);
        }

        validateBackupScript(content);

        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            EncodedResource resource = new EncodedResource(new ByteArrayResource(content), StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(connection, resource);
        } catch (Exception e) {
            throw new DataCleanupException("Failed to import SQL backup: " + e.getMessage(), e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    /**
     * Deletes all QuestionBank-related data from the database while preserving
     * user management data (users, roles, permissions).
     *
     * Deletion order (respects foreign key constraints):
     * 1. Question Errors
     * 2. Question Duplicates
     * 3. Questions
     * 4. QuestionBank Authors
     * 5. Archive Imports
     * 6. Question Banks
     * 7. Authors
     * 8. Courses
     *
     * Users, Roles, and Permissions are NOT deleted.
     */
    @Transactional
    public void deleteAllDataExceptUsersRolesPermissions() {
        long startTime = System.currentTimeMillis();
        log.atInfo().log("Starting comprehensive data cleanup (preserving users, roles, permissions)...");

        try {
            // Step 1: Delete all QuestionErrors
            deleteAllAndLog(questionErrorRepository, "question errors");

            // Step 2: Delete all QuestionDuplicates
            deleteAllAndLog(questionDuplicateRepository, "question duplicates");

            // Step 3: Delete all Questions
            deleteAllAndLog(questionRepository, "questions");

            // Step 4: Delete all QuizAuthors
            deleteAllAndLog(questionBankAuthorRepository, "QuestionBank authors");

            // Step 5: Delete all ArchiveImports
            deleteAllAndLog(archiveImportRepository, "archive imports");

            // Step 6: Delete all DuplicateRecomputeHistory entries
            deleteAllAndLog(duplicateRecomputeHistoryRepository, "duplicate recompute history entries");

            // Step 7: Delete all Question Banks
            deleteAllAndLog(questionBankRepository, "Question Banks");

            // Step 8: Delete all Authors
            deleteAllAndLog(authorRepository, "authors");

            // Step 9: Delete all Courses
            deleteAllAndLog(courseRepository, "courses");

            long totalTime = System.currentTimeMillis() - startTime;
            log.atInfo().addArgument(totalTime)
                .log("Data cleanup completed successfully in {}ms. Users, roles, and permissions preserved.");

        } catch (Exception e) {
            long failedTime = System.currentTimeMillis() - startTime;
            log.atError().addArgument(failedTime).addArgument(e.getMessage())
                .log("Error during data cleanup after {}ms: {}");
            throw new DataCleanupException("Failed to delete all data: " + e.getMessage(), e);
        }
    }

    /**
     * Gets statistics about the current database state as key-value pairs.
     * Extended statistics (users, roles, permissions) are only included
     * if the user has the canViewExtendedStatistics permission.
     *
     * @return A Map containing entity counts
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getDataStatistics() {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Basic statistics - always included
        stats.put("questionBanks", questionBankRepository.count());
        stats.put("quizAuthors", questionBankAuthorRepository.count());
        stats.put("archiveImports", archiveImportRepository.count());

        stats.put("authors", authorRepository.count());
        stats.put("courses", courseRepository.count());

        stats.put("questions", questionRepository.count());
        stats.put("questionErrors", questionErrorRepository.count());
        stats.put("questionDuplicate", questionDuplicateRepository.count());

        // Check if user has permission to view extended statistics
        if (canViewExtendedStatistics()) {
            // Add IAM statistics for administrators via single call to auth service
            try {
                Map<String, Long> iamStats = getIamStatistics();
                if (iamStats != null && !iamStats.isEmpty()) {
                    stats.putAll(iamStats);
                    log.atDebug().addArgument(iamStats)
                        .log("Extended statistics included for authorized user: {}");
                }
            } catch (Exception e) {
                log.atWarn().addArgument(e.getMessage())
                    .log("Failed to retrieve IAM statistics: {}");
            }
        } else {
            log.atDebug().log("Extended statistics not included - user lacks canViewExtendedStatistics permission");
        }

        return stats;
    }

    /**
     * Checks if the current user has the VIEW_EXTENDED_STATISTICS permission.
     * This permission is extracted from the JWT token and stored in the SecurityContext
     * by the JwtFilter, so no external call is needed.
     *
     * @return true if user has the permission, false otherwise
     */
    private boolean canViewExtendedStatistics() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.atDebug().addArgument(auth != null ? auth.getClass().getName() : "null")
                .log("[PERMISSION CHECK] Authentication object: {}");

            if (auth == null || !auth.isAuthenticated()) {
                log.atDebug().log("[PERMISSION CHECK] User not authenticated");
                return false;
            }

            String username = auth.getName();
            log.atDebug().addArgument(username).log("[PERMISSION CHECK] Username: {}");

            if (username == null || username.equals("anonymousUser")) {
                log.atDebug().log("[PERMISSION CHECK] Anonymous user detected");
                return false;
            }

            // Check if user has the VIEW_EXTENDED_STATISTICS permission
            // Permissions are stored as GrantedAuthority objects by JwtFilter
            log.atDebug().addArgument(username).addArgument(auth.getAuthorities().size())
                .log("[PERMISSION CHECK] User {} authorities count: {}");
            if (log.isTraceEnabled()) {
                auth.getAuthorities().forEach(authority ->
                    log.atTrace().addArgument(authority.getAuthority())
                        .log("[PERMISSION CHECK] Authority: {}")
                );
            }

            boolean hasPermission = auth.getAuthorities().stream()
                    .anyMatch(authority -> PERMISSION_VIEW_EXTENDED_STATISTICS.equals(authority.getAuthority()));

            log.atDebug().addArgument(username).addArgument(hasPermission)
                .log("[PERMISSION CHECK] User {} has VIEW_EXTENDED_STATISTICS permission: {}");
            return hasPermission;
        } catch (Exception e) {
            log.atWarn().addArgument(e.getMessage()).setCause(e)
                .log("Failed to check VIEW_EXTENDED_STATISTICS permission: {}");
            return false;
        }
    }

    /**
     * Gets IAM statistics (users, roles, permissions) from auth service
     * which in turn retrieves them from IAM service.
     * This makes a single call instead of three separate calls.
     * Forwards the JWT token from the current request to authenticate with auth service.
     *
     * @return A Map containing IAM entity counts (users, roles, permissions)
     */
    private Map<String, Long> getIamStatistics() {
        try {
            // Get the JWT token from the current HTTP request
            String jwtToken = getJwtTokenFromRequest();
            if (jwtToken == null) {
                log.atWarn().log("Cannot fetch IAM statistics: JWT token not found in request");
                return new LinkedHashMap<>();
            }

            // Create HTTP headers with Authorization
            HttpHeaders headers = new HttpHeaders();
            headers.set(AUTHORIZATION_HEADER, BEARER_PREFIX + jwtToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = authUrl + IAM_STATISTICS_ENDPOINT;
            log.atDebug().addArgument(url).log("Fetching IAM statistics from {} with JWT token");

            ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null) {
                log.atDebug().addArgument(response.getBody()).log("IAM statistics retrieved: {}");
                return response.getBody();
            }
            return new LinkedHashMap<>();
        } catch (Exception e) {
            log.atWarn().addArgument(e.getMessage()).log("Failed to get IAM statistics: {}");
            return new LinkedHashMap<>();
        }
    }

    /**
     * Extracts JWT token from the current HTTP request.
     *
     * @return JWT token without "Bearer " prefix, or null if not found
     */
    private String getJwtTokenFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(AUTHORIZATION_HEADER);

                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    return authHeader.substring(BEARER_PREFIX.length());
                }
            }
            return null;
        } catch (Exception e) {
            log.atWarn().addArgument(e.getMessage()).log("Failed to extract JWT token from request: {}");
            return null;
        }
    }

    private void appendTableData(StringBuilder sql, String table) {
        List<String> columns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' " +
                "AND table_name = ? ORDER BY ordinal_position",
            String.class,
            table
        );
        if (columns == null || columns.isEmpty()) {
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table + " ORDER BY id");
        if (rows.isEmpty()) {
            return;
        }

        sql.append("-- Data for table ").append(table).append("\n");
        String columnList = String.join(", ", columns);
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String column : columns) {
                values.add(formatSqlValue(row.get(column)));
            }
            sql.append("INSERT INTO ").append(table).append(" (")
                .append(columnList).append(") VALUES (")
                .append(String.join(", ", values)).append(");\n");
        }
        sql.append("\n");
    }

    private void appendSequenceReset(StringBuilder sql, String sequenceName, String table, String idColumn) {
        sql.append("SELECT setval('")
            .append(sequenceName)
            .append("', COALESCE((SELECT MAX(")
            .append(idColumn)
            .append(") FROM ")
            .append(table)
            .append("), 1), true);\n");
    }

    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Timestamp timestamp) {
            return quoteSql(timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return quoteSql(offsetDateTime.toString());
        }
        if (value instanceof TemporalAccessor) {
            return quoteSql(value.toString());
        }
        return quoteSql(value.toString());
    }

    private String quoteSql(String input) {
        return "'" + input.replace("'", "''") + "'";
    }

    private void validateBackupScript(byte[] content) {
        String sql = new String(content, StandardCharsets.UTF_8);
        String sanitizedSql = SQL_LINE_COMMENT_PATTERN.matcher(sql).replaceAll("");

        for (String rawStatement : sanitizedSql.split(";")) {
            String statement = rawStatement.trim().toLowerCase(java.util.Locale.ROOT);
            if (statement.isEmpty() || !WRITE_STATEMENT_PATTERN.matcher(statement).find()) {
                continue;
            }

            for (String authTable : AUTH_TABLE_NAMES) {
                if (containsTableReference(statement, authTable)) {
                    throw new IllegalArgumentException("Backup file must not modify auth tables");
                }
            }
        }
    }

    private boolean containsTableReference(String statement, String table) {
        return Pattern.compile("\\b(?:public\\.)?" + Pattern.quote(table) + "\\b")
            .matcher(statement)
            .find();
    }

    /**
     * Helper method to delete all entities from a repository and log the count.
     * Counts entities before deletion for logging purposes.
     *
     * @param repository the repository to delete from
     * @param entityName the name of the entity type for logging
     * @param <T> the entity type
     */
    private <T> void deleteAllAndLog(org.springframework.data.repository.CrudRepository<T, ?> repository, String entityName) {
        long count = repository.count();
        if (count > 0) {
            repository.deleteAll();
            log.atInfo().addArgument(count).addArgument(entityName).log("Deleted {} {}");
        }
    }
}

