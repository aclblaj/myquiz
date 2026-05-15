package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.ArchiveUploadResult;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.specifications.QuestionSpecification;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for QuestionService that connects to the Docker PostgreSQL database.
 * <p>
 * Prerequisites:
 * - Docker PostgreSQL container running (from docker-compose.yml)
 * - Database exposed on localhost:5433
 * - Database name: myquiz
 * - Username: myquiz_user
 * - Password: myquiz_password
 * <p>
 * To start the database: docker-compose up postgres -d
 * <p>
 * Note: This test uses @Disabled by default. Remove @Disabled annotation to run the test.
 * Make sure the test data directory exists before running parseExcelFilesFromFolder test.
 */
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application.properties", properties = {
        "myquiz.tasks.duplicate-check.core-pool-size=80",
        "myquiz.tasks.duplicate-check.max-pool-size=80",
        "myquiz.tasks.duplicate-check.queue-capacity=20000",
        "logging.level.com.unitbv.myquiz.app.services.QuestionDuplicationService=DEBUG"
}
)
@Transactional
class QuestionServiceTest {

    Logger logger = LoggerFactory.getLogger(QuestionServiceTest.class);

    @Autowired
    QuestionService questionService;

    @Autowired
    QuestionDuplicationService questionDuplicationService;

    @Autowired
    EncodingSevice encodingSevice;

    @Autowired
    AuthorService authorService;

    @Autowired
    QuestionBankService questionBankService;

    @Autowired
    QuestionBankAuthorService questionBankAuthorService;

    @Autowired
    QuestionErrorService questionErrorService;

    @Autowired
    CourseService courseService;

    @Autowired
    UploadService uploadService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    QuestionDuplicateRepository questionDuplicateRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Test
    void compareDuplicateRecomputeAlgorithmsForExistingCourseBd() {
        final String courseName = System.getProperty(
                "myquiz.test.duplicate.course",
                ServiceTestData.DEFAULT_DUPLICATE_COURSE
        );
        final int maxQuestions = Integer.getInteger(
                "myquiz.test.duplicate.maxQuestions",
                ServiceTestData.DEFAULT_MAX_QUESTIONS
        );

//        final int maxQuestions = Integer.getInteger("myquiz.test.duplicate.maxQuestions", Integer.MAX_VALUE);

        List<Question> selectedQuestions = questionRepository.findAll(QuestionSpecification.byFilters(
                courseName,
                null,
                null,
                null
        )).stream().filter(question -> question.getId() != null).sorted(java.util.Comparator.comparing(Question::getId)).limit(maxQuestions).toList();
        assumeTrue(
                !selectedQuestions.isEmpty(),
                "Skipping test: no questions found for course: " + courseName
        );

        List<Long> selectedQuestionIds = selectedQuestions.stream().map(Question::getId).toList();
        logger.atInfo().addArgument(courseName).addArgument(selectedQuestionIds.size()).addArgument(maxQuestions)
              .log("Starting first-N duplicate recompute comparison for course '{}': selected {} questions (requested max {})");

        logger.atInfo().addArgument("levenshtein").addArgument(Thread.currentThread().getName()).log("Invoking recompute subset with algorithm '{}' from test thread '{}'");
        QuestionDuplicationService.DuplicateRecomputeSummary levenshteinSummary = questionDuplicationService.recomputeDuplicatesForCourseSubset(
                courseName,
                "levenshtein",
                maxQuestions
        );
        Set<String> levenshteinPairs = loadDuplicatePairsForQuestionIds(selectedQuestionIds);

        logger.atInfo().addArgument("jaro-winkler").addArgument(Thread.currentThread().getName()).log("Invoking recompute subset with algorithm '{}' from test thread '{}'");
        QuestionDuplicationService.DuplicateRecomputeSummary jaroWinklerSummary = questionDuplicationService.recomputeDuplicatesForCourseSubset(
                courseName,
                "jaro-winkler",
                maxQuestions
        );
        Set<String> jaroWinklerPairs = loadDuplicatePairsForQuestionIds(selectedQuestionIds);

        assertEquals(
                levenshteinSummary.totalQuestions(),
                jaroWinklerSummary.totalQuestions()
        );
        assertTrue(levenshteinSummary.totalQuestions() >= selectedQuestionIds.size());
        assertTrue(levenshteinSummary.duplicateErrorsCreated() >= 0);
        assertTrue(jaroWinklerSummary.duplicateErrorsCreated() >= 0);

        logger.atInfo().addArgument(selectedQuestionIds.size()).addArgument(courseName).addArgument(levenshteinPairs.size()).addArgument(jaroWinklerPairs.size())
              .addArgument(levenshteinSummary.duplicateErrorsCreated()).addArgument(jaroWinklerSummary.duplicateErrorsCreated())
              .log("First-{} duplicate recompute comparison for course '{}': pairs lev={}, pairs jaro={}, errors lev={}, errors jaro={}");
    }

    private Set<String> loadDuplicatePairsForCourse(String courseName) {
        List<Question> questions = questionRepository.findAll(QuestionSpecification.byFilters(
                courseName,
                null,
                null,
                null
        ));
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        if (questionIds.isEmpty()) {
            return Set.of();
        }

        return questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(
                questionIds,
                questionIds
        ).stream().map(link -> normalizeDuplicatePair(
                link.getQuestion().getId(),
                link.getDuplicateQuestion().getId()
        )).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadDuplicatePairsForQuestionIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> idSet = new LinkedHashSet<>(questionIds);
        return questionDuplicateRepository.findByQuestionIdInOrDuplicateQuestionIdIn(
                questionIds,
                questionIds
        ).stream().filter(link -> idSet.contains(link.getQuestion().getId()) && idSet.contains(link.getDuplicateQuestion().getId())).map(link -> normalizeDuplicatePair(
                link.getQuestion().getId(),
                link.getDuplicateQuestion().getId()
        )).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeDuplicatePair(Long left, Long right) {
        long lower = Math.min(
                left,
                right
        );
        long higher = Math.max(
                left,
                right
        );
        return lower + "-" + higher;
    }

    /**
     * Integration test for parsing Excel files from a folder structure.
     * This test validates the archive upload functionality described in upload-sd.md Section 2.4.
     * <p>
     * Test Flow (as per upload-sd.md):
     * 1. Create/verify course exists (CourseService)
     * 2. Check server encoding
     * 3. Verify folder exists
     * 4. Create questionBank (QuestionBankService)
     * 5. Parse all Excel files recursively (QuestionService.parseExcelFilesFromFolder)
     * - Template type is automatically detected during parsing
     * 6. Validate questions for duplicates (QuestionDuplicationService)
     * <p>
     * Prerequisites:
     * - Database connection configured and available
     * - Test data directory exists with Excel files
     * - Valid Excel files in supported format (.xlsx)
     * <p>
     * Note: Disabled by default. Remove @Disabled and configure the correct directory path to run.
     */
    @Test
//    @Disabled("Enable this test manually by removing @Disabled and configuring correct directory path")
    void parseExcelFilesFromFolder() {
        String configuredFolder = System.getProperty(
                "myquiz.test.folder",
                ServiceTestData.DEFAULT_FOLDER_PATH
        );
        syncCoreSequences();
        long runId = System.currentTimeMillis();

        // Step 1: Create course if not exists (as per upload-sd.md Section 2.4, Step 4)
        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.PARSE_FOLDER_COURSE_PREFIX + runId)
                                             .description(ServiceTestData.PARSE_FOLDER_DESCRIPTION).semester("2").universityYear("2").build();
        courseDto = courseService.createCourseIfNotExists(courseDto);
        assertNotNull(
                courseDto,
                "Course should be created successfully"
        );
        logger.atInfo().addArgument(courseDto.getCourse()).log("Course created/verified: {}");

        // Configure input parameters - Update this path to match your local test data
        InputParameters input = new InputParameters(
                configuredFolder,
                courseDto.getCourse(),
                ServiceTestData.PARSE_FOLDER_QUESTION_BANK_PREFIX + runId,
                StudyYear.Y2026_2027
        );

        // Alternative test data configurations (uncomment as needed):
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-VDB\\inpQ1\\", "25-VDB-Cloud", "Cloud", 2025L);
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-BD\\inpQ1\\", "25-BD-Q1-v2", "Q1", 2025L);
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-BD\\inpQ2\\", "25-DB-Q2", "Q2", 2025L);
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-MIDB\\inpQ1\\", "25-MIDB-Q1", "Q1", 2025L);
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-MIDB\\inpQ2\\", "25-MIDB-Q2", "Q2", 2025L);
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-ITSec\\inpQ1\\", "25-ITSec-Q1", "Q1", 2025L);
        // InputParameters input = new InputParameters("C:\\work\\_mi\\2025-ITSec\\inpQ2\\", "25-ITSec-Q2", "Q2", 2025L);

        long startTime = System.currentTimeMillis();

        // Step 2: Check server encoding (as per upload-sd.md)
        if (encodingSevice.checkServerEncoding()) {
            logger.atWarn().log("Server encoding check failed - test aborted");
            return;
        }

        // Step 3: Verify folder exists
        File folder = new File(input.getDirPath());
        assumeTrue(
                folder.exists() && folder.isDirectory(),
                "Skipping test: test data folder missing or invalid: " + input.getDirPath()
        );
        logger.atInfo().addArgument(input.getDirPath()).log("Processing folder: {}");

        // Step 4: Create questionBank (as per upload-sd.md Section 2.4, Step 4)
        QuestionBank questionBank = questionBankService.createQuestionBank(
                input.getCourse(),
                input.getQuestionBank(),
                input.getStudyYear()
        );
        assertNotNull(
                questionBank,
                "QuestionBank should be created successfully"
        );
        assertNotNull(
                questionBank.getId(),
                "QuestionBank should have an ID after creation"
        );
        logger.atInfo().addArgument(questionBank.getName()).addArgument(questionBank.getId()).log("QuestionBank '{}' created with ID: {}");

        // Step 5: Parse Excel files from folder (as per upload-sd.md Section 2.4, Step 4)
        // This corresponds to QuestionService.parseExcelFilesFromFolder in upload-sd.md
        // Note: Template type is now automatically detected during parsing
        int result = questionService.parseExcelFilesFromFolder(
                questionBank,
                folder,
                0
        );

        long endTime = System.currentTimeMillis();
        logger.atInfo().addArgument(result).log("Number of parsed excel files: {}");
        logger.atInfo().addArgument((endTime - startTime)).log("Execution time: {} ms");

        // Step 6: Validate questions for duplicates (as per upload-sd.md Section 2.2, Step 3)
        // Note: authorEntities list should be populated from the parsing results
        // Currently empty as the author tracking was commented out in the service
        ArrayList<Author> authorEntities = questionBankAuthorService.getAuthorsForQuestionBank(questionBank.getId());

        questionDuplicationService.checkDuplicateQuestionsForAuthors(
                authorEntities,
                questionBank.getCourseName()
        );
        logger.atInfo().log("Duplicate validation completed");

        // Assertions
        assertNotEquals(
                -1,
                result,
                "Parsing should not return error code -1"
        );
        assertTrue(
                result >= 0,
                "Number of parsed files should be non-negative"
        );
        logger.atInfo().log("Test completed successfully");
    }

    private void syncCoreSequences() {
        syncSequence(
                "author_seq",
                "author"
        );
        syncSequence(
                "course_seq",
                "course"
        );
        syncSequence(
                "question_bank_seq",
                "question_bank"
        );
        syncSequence(
                "question_bank_author_seq",
                "question_bank_author"
        );
        syncSequence(
                "question_seq",
                "question"
        );
        syncSequence(
                "question_error_seq",
                "question_error"
        );
        syncSequence(
                "question_duplicate_seq",
                "question_duplicate"
        );
    }

    private void syncSequence(String sequenceName, String tableName) {
        String sql = "SELECT setval('" + sequenceName + "', GREATEST(COALESCE((SELECT MAX(id) FROM " + tableName + "), 0) + 1, 1), false)";
        jdbcTemplate.queryForObject(
                sql,
                Long.class
        );
    }

    @Test
    @Commit
    void parseSingleExcelFile() {
        String configuredExcel = System.getProperty(
                "myquiz.test.excel",
                ServiceTestData.DEFAULT_EXCEL_PATH
        );

        // Target Excel file – update this constant if the file is moved
        final String EXCEL_FILE_PATH = configuredExcel;

        Author author = authorService.saveAuthorFromFile(new File(EXCEL_FILE_PATH));

        // Step 1: Create course if not exists
        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.SINGLE_FILE_COURSE).description(ServiceTestData.SINGLE_FILE_COURSE_DESCRIPTION)
                                                     .semester("1").universityYear("2").build();
        courseDto = courseService.createCourseIfNotExists(courseDto);
        assertNotNull(
                courseDto,
                "Course should be created successfully"
        );
        logger.atInfo().addArgument(courseDto.getCourse()).log("Course created/verified: {}");

        // Configure input parameters
        InputParameters input = new InputParameters(
                EXCEL_FILE_PATH,
                courseDto.getCourse(),
                ServiceTestData.SINGLE_FILE_QUESTION_BANK,
                StudyYear.Y2024_2025
        );

        long startTime = System.currentTimeMillis();

        // Step 2: Check server encoding
        if (encodingSevice.checkServerEncoding()) {
            logger.atWarn().log("Server encoding check failed - test aborted");
            return;
        }

        // Step 3: Verify the file exists and is a valid Excel file
        File excelFile = new File(EXCEL_FILE_PATH);
        assumeTrue(
                excelFile.exists() && excelFile.isFile() && excelFile.getName().toLowerCase().endsWith(".xlsx"),
                "Skipping test: Excel file missing or invalid: " + EXCEL_FILE_PATH
        );
        logger.atInfo().addArgument(EXCEL_FILE_PATH).log("Processing single Excel file: {}");

        // Step 4: Create questionBank
        QuestionBank questionBank = questionBankService.createQuestionBank(
                input.getCourse(),
                input.getQuestionBank(),
                input.getStudyYear()
        );
        assertNotNull(
                questionBank,
                "QuestionBank should be created successfully"
        );
        assertNotNull(
                questionBank.getId(),
                "QuestionBank should have an ID after creation"
        );
        logger.atInfo().addArgument(questionBank.getName()).addArgument(questionBank.getId()).log("QuestionBank '{}' created with ID: {}");

        // Step 5: Parse the single Excel file
        // parseExcelFilesFromFolder also handles a Path argument pointing to a single file
        int result = questionService.parseExcelFilesFromFolder(
                questionBank,
                excelFile,
                0
        );

        long endTime = System.currentTimeMillis();
        logger.atInfo().addArgument(result).log("Number of parsed excel files: {}");
        logger.atInfo().addArgument((endTime - startTime)).log("Execution time: {} ms");

        // Step 6: Validate questions for duplicates
        ArrayList<Author> authorEntities = new ArrayList<>();
        authorEntities.add(author);

        questionDuplicationService.checkDuplicateQuestionsForAuthors(
                authorEntities,
                questionBank.getCourseName()
        );

        // Assertions
        assertNotEquals(
                -1,
                result,
                "Parsing should not return error code -1"
        );
        assertEquals(
                1,
                result,
                "Exactly 1 file should have been parsed"
        );
        logger.atInfo().log("parseSingleExcelFile test completed successfully");
    }

    /**
     * Integration input requirements for this test:
     * - Place the ZIP archive in src/test/resources/test-input/archives/
     * - Expected file name: 2026-SSI-inpQ1 25 intrebari - Kap. 1-4 - Erste Teil - Fragen Vorbereitung-39230.zip
     * - Expected classpath location: test-input/archives/<expected-file-name>
     * <p>
     * If the archive is missing, the test is skipped with a clear message.
     */
    @Test
    void parseExcelFilesFromArchive() throws Exception {
        final String archiveFileName = ServiceTestData.ARCHIVE_FILE_NAME;
        final String archiveResourcePath = "test-input/archives/" + archiveFileName;

        URL archiveResource = getClass().getClassLoader().getResource(archiveResourcePath);
        assumeTrue(
                archiveResource != null,
                "Test archive missing. Place '" + archiveFileName + "' under myquiz-app/src/test/resources/test-input/archives/"
        );

        File archiveFile = new File(archiveResource.toURI());
        assertTrue(
                archiveFile.exists(),
                "Archive file should exist: " + archiveFile.getAbsolutePath()
        );
        assertTrue(
                archiveFile.isFile(),
                "Archive path should be a file: " + archiveFile.getAbsolutePath()
        );
        logger.atInfo().addArgument(archiveFile.getAbsolutePath()).log("Processing archive file: {}");

        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.ARCHIVE_COURSE).description(ServiceTestData.ARCHIVE_COURSE_DESCRIPTION).semester("2")
                                                     .universityYear("2").build();
        courseDto = courseService.createCourseIfNotExists(courseDto);
        assertNotNull(
                courseDto,
                "Course should be created successfully"
        );
        logger.atInfo().addArgument(courseDto.getCourse()).log("Course created/verified: {}");

        MockMultipartFile archiveMultipartFile = new MockMultipartFile(
                "archive",
                archiveFileName,
                "application/zip",
                Files.readAllBytes(archiveFile.toPath())
        );

        ArchiveUploadResult result = uploadService.processArchiveUpload(
                archiveMultipartFile,
                courseDto.getId(),
                ServiceTestData.ARCHIVE_QUESTION_BANK,
                StudyYear.Y2026_2027
        );

        assertNotNull(
                result,
                "Archive processing should return a result"
        );
        assertEquals(
                ServiceTestData.ARCHIVE_QUESTION_BANK,
                result.questionBankName(),
                "QuestionBank name should match the requested questionBank"
        );
        assertTrue(
                result.filesProcessed() >= 0,
                "Processed files count should be non-negative"
        );
        logger.atInfo().addArgument(result.toMessage()).log("Archive processing completed: {}");

    }

    /**
     * Test to verify database connectivity.
     * This test validates that the test configuration properly connects to the local database.
     * <p>
     * NOTE: This test uses @Transactional (from class level), so data is ROLLED BACK after test.
     * The course will NOT persist in the database after the test completes.
     * This is intentional to keep tests isolated and repeatable.
     * Use @Commit if you want to persist data for debugging.
     * Test Flow:
     * 1. Create a test course
     * 2. Verify course is persisted to database (within transaction)
     * 3. Retrieve course from database (within transaction)
     * 4. Verify course data matches
     * 5. Transaction rolls back automatically
     */

    @Test
    void testDatabaseConnectivity() {
        // Create test course
        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.DB_TEST_COURSE).description(ServiceTestData.DB_TEST_COURSE_DESCRIPTION).semester("1")
                                                     .universityYear("1").build();

        // Save to database (within transaction)
        CourseDto savedCourse = courseService.createCourseIfNotExists(courseDto);

        // Verify course was saved (within transaction)
        assertNotNull(
                savedCourse,
                "Course should be saved"
        );
        assertNotNull(
                savedCourse.getId(),
                "Course should have an ID"
        );
        logger.atInfo().addArgument(savedCourse.getId()).addArgument(savedCourse.getCourse()).log("Database connectivity verified - Course ID: {}, Course: {}");

        // Verify we can retrieve the course (within transaction)
        assertEquals(
                ServiceTestData.DB_TEST_COURSE,
                savedCourse.getCourse(),
                "Retrieved course should match saved course"
        );

        logger.atInfo().log("Note: Course will be rolled back after test due to @Transactional");
    }

    /**
     * Test to verify database connectivity with PERSISTENT data.
     * This test is NOT transactional, so the course WILL persist in the database.
     * <p>
     * Use this test when you want to verify data actually persists to the database
     * and remains after the test completes.
     * <p>
     * WARNING: This test does NOT clean up after itself. The course will remain in the database.
     * Run the 'delete' test or manually delete if needed.
     * <p>
     * Test Flow:
     * 1. Create a test course
     * 2. Verify course is persisted to database
     * 3. Retrieve course from database
     * 4. Verify course data matches
     * 5. Course remains in database after test
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testDatabaseConnectivityWithPersistence() {
        // Create test course
        CourseDto courseDto = ServiceTestData.courseDtoBuilder().course(ServiceTestData.DB_PERSIST_TEST_COURSE).description(ServiceTestData.DB_PERSIST_TEST_COURSE_DESCRIPTION)
                                                     .semester("1").universityYear("1").build();

        // Save to database (will actually persist)
        CourseDto savedCourse = courseService.createCourseIfNotExists(courseDto);

        // Verify course was saved
        assertNotNull(
                savedCourse,
                "Course should be saved"
        );
        assertNotNull(
                savedCourse.getId(),
                "Course should have an ID"
        );
        logger.atInfo().addArgument(savedCourse.getId()).addArgument(savedCourse.getCourse()).log("Database connectivity verified (PERSISTENT) - Course ID: {}, Course: {}");

        // Verify we can retrieve the course
        assertEquals(
                ServiceTestData.DB_PERSIST_TEST_COURSE,
                savedCourse.getCourse(),
                "Retrieved course should match saved course"
        );

        logger.atWarn().log("WARNING: Course 'TEST_DB_CONN_PERSIST' will remain in database after test!");
    }

    @Test
    void getServerEncoding() {
        String result = encodingSevice.getServerEncoding();
        logger.atInfo().addArgument(result).log("Server encoding: {}");
        assertNotNull(result);
    }

    @Test
    void getQuestionsByAuthorId() {
        QuestionBankAuthor questionBankAuthor = createQuestionBankWithQuestions();
        List<Question> result = questionService.getQuestionsForAuthorId(
                questionBankAuthor.getAuthor().getId(),
                questionBankAuthor.getQuestionBank().getCourseName()
        );
        logger.atInfo().addArgument(result.size()).log("Number of questions: {}");
        result.forEach(question -> logger.atInfo().addArgument(question).log("Question: {}"));
        assertNotNull(result);
    }

    @Test
    void getQuestionsByAuthorName() {
        QuestionBankAuthor questionBankAuthor = createQuestionBankWithQuestions();
        String authorName = questionBankAuthor.getAuthor().getName();
        List<Question> result = questionService.getQuestionsForAuthorName(authorName);
        logger.atInfo().addArgument(result.size()).log("Number of questions: {}");
        result.forEach(question -> logger.atInfo().addArgument(question).log("Question: {}"));
        assertNotNull(result);
        assertTrue(result.stream().allMatch(q -> q.getQuestionBankAuthor().getAuthor().getName().contains(authorName)));
    }

    private QuestionBankAuthor createQuestionBankWithQuestions() {
        TestEntityFactory.QuestionBankAuthorFixture fixture = testEntityFactory.createQuestionBankAuthorFixture(ServiceTestData.questionBankAuthorSpecBuilder().build());
        QuestionBankAuthor questionBankAuthor = fixture.questionBankAuthor();

        Question question1 = ServiceTestData.questionBuilder().crtNo(1).title(ServiceTestData.AUTHOR_QUESTION_1_TITLE).text(ServiceTestData.AUTHOR_QUESTION_1_TEXT)
                                                    .type(QuestionType.MULTICHOICE).questionBankAuthor(questionBankAuthor).build();

        Question question2 = ServiceTestData.questionBuilder().crtNo(2).title(ServiceTestData.AUTHOR_QUESTION_2_TITLE).text(ServiceTestData.AUTHOR_QUESTION_2_TEXT)
                                                    .type(QuestionType.MULTICHOICE).questionBankAuthor(questionBankAuthor).build();

        List<Question> questions = List.of(
                questionRepository.save(question1),
                questionRepository.save(question2)
        );
        questionBankAuthor.setQuestions(questions);

        logger.atInfo().addArgument(fixture.author()).log("Author: {}");
        return questionBankAuthor;
    }

}
