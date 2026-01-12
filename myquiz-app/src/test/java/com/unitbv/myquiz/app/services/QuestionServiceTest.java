package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.Quiz;
import com.unitbv.myquiz.app.entities.QuizAuthor;
import com.unitbv.myquiz.app.repositories.AuthorRepository;
import com.unitbv.myquiz.app.repositories.QuizAuthorRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for QuestionService that connects to the Docker PostgreSQL database.
 *
 * Prerequisites:
 * - Docker PostgreSQL container running (from docker-compose.yml)
 * - Database exposed on localhost:5433
 * - Database name: myquiz
 * - Username: myquiz_user
 * - Password: myquiz_password
 *
 * To start the database: docker-compose up postgres -d
 *
 * Note: This test uses @Disabled by default. Remove @Disabled annotation to run the test.
 * Make sure the test data directory exists before running parseExcelFilesFromFolder test.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
class QuestionServiceTest {

    Logger logger = LoggerFactory.getLogger(QuestionServiceTest.class);

    @Autowired
    QuestionService questionService;

   @Autowired
    QuestionValidationService questionValidationService;

    @Autowired
    EncodingSevice encodingSevice;

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    QuizAuthorRepository quizAuthorRepository;

    @Autowired
    AuthorService authorService;

    @Autowired
    QuizService quizService;

    @Autowired
    QuizAuthorService quizAuthorService;

    @Autowired
    QuizErrorService quizErrorService;

    @Autowired
    CourseService courseService;

    @Autowired
    UploadService uploadService;

    /**
     * Integration test for parsing Excel files from a folder structure.
     * This test validates the archive upload functionality described in upload-sd.md Section 2.4.
     *
     * Test Flow (as per upload-sd.md):
     * 1. Create/verify course exists (CourseService)
     * 2. Check server encoding
     * 3. Verify folder exists
     * 4. Create quiz (QuizService)
     * 5. Parse all Excel files recursively (QuestionService.parseExcelFilesFromFolder)
     *    - Template type is automatically detected during parsing
     * 6. Validate questions for duplicates (QuestionValidationService)
     *
     * Prerequisites:
     * - Database connection configured and available
     * - Test data directory exists with Excel files
     * - Valid Excel files in supported format (.xlsx)
     *
     * Note: Disabled by default. Remove @Disabled and configure the correct directory path to run.
     */
    @Test
//    @Disabled("Enable this test manually by removing @Disabled and configuring correct directory path")
    @Commit
    void parseExcelFilesFromFolder() {
        // Step 1: Create course if not exists (as per upload-sd.md Section 2.4, Step 4)
        CourseDto courseDto = new CourseDto();
        courseDto.setCourse("NetAlg");
        courseDto.setDescription("Network Algorithms");
        courseDto.setSemester("1");
        courseDto.setStudyYear("2025-2026");
        courseDto.setUniversityYear("2");
        courseDto = courseService.createCourseIfNotExists(courseDto);
        assertNotNull(courseDto, "Course should be created successfully");
        logger.atInfo().addArgument(courseDto.getCourse()).log("Course created/verified: {}");

        // Configure input parameters - Update this path to match your local test data
        InputParameters input = new InputParameters(
            "C:\\work\\_mi\\2025-NA\\inpQ1\\",
            courseDto.getCourse(),
            "NetAlg",
            2025L
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
        assertTrue(folder.exists(), "Test data folder should exist: " + input.getDirPath());
        assertTrue(folder.isDirectory(), "Test data path should be a directory: " + input.getDirPath());
        logger.atInfo().addArgument(input.getDirPath()).log("Processing folder: {}");

        // Step 4: Create quiz (as per upload-sd.md Section 2.4, Step 4)
        Quiz quiz = quizService.createQuizz(input.getCourse(), input.getQuiz(), input.getYear());
        assertNotNull(quiz, "Quiz should be created successfully");
        assertNotNull(quiz.getId(), "Quiz should have an ID after creation");
        logger.atInfo().addArgument(quiz.getName()).addArgument(quiz.getId())
              .log("Quiz '{}' created with ID: {}");

        // Step 5: Parse Excel files from folder (as per upload-sd.md Section 2.4, Step 4)
        // This corresponds to QuestionService.parseExcelFilesFromFolder in upload-sd.md
        // Note: Template type is now automatically detected during parsing
        int result = questionService.parseExcelFilesFromFolder(quiz, folder, 0);

        long endTime = System.currentTimeMillis();
        logger.atInfo().addArgument(result).log("Number of parsed excel files: {}");
        logger.atInfo().addArgument((endTime - startTime)).log("Execution time: {} ms");

        // Step 6: Validate questions for duplicates (as per upload-sd.md Section 2.2, Step 3)
        // Note: authorEntities list should be populated from the parsing results
        // Currently empty as the author tracking was commented out in the service
        ArrayList<Author> authorEntities = new ArrayList<>();
        questionValidationService.checkDuplicatesQuestionsForAuthors(authorEntities, quiz.getCourse());
        logger.atInfo().log("Duplicate validation completed");

        // Assertions
        assertNotEquals(-1, result, "Parsing should not return error code -1");
        assertTrue(result >= 0, "Number of parsed files should be non-negative");
        logger.atInfo().log("Test completed successfully");
    }

    @Test
    void parseExcelFilesFromArchive() throws IOException {
        // This test can be implemented similarly to parseExcelFilesFromFolder
        // by creating a ZIP archive input and calling the appropriate service method.
        String archivePath = "C:\\work\\_mi\\2026-BD\\2026-BD-InpQ1 - Intrebari propuse din C1-C5-29980.zip";
        File archiveFile = new File(archivePath);
        assertTrue(archiveFile.exists(), "Archive file should exist: " + archivePath);
        assertTrue(archiveFile.isFile(), "Archive path should be a file: " + archivePath);
        logger.atInfo().addArgument(archivePath).log("Processing archive file: {}");

        CourseDto courseDto = new CourseDto();
        courseDto.setCourse("26-BD");
        courseDto.setDescription("Baze de date");
        courseDto.setSemester("1");
        courseDto.setStudyYear("2025-2026");
        courseDto.setUniversityYear("2");
        courseDto = courseService.createCourseIfNotExists(courseDto);
        assertNotNull(courseDto, "Course should be created successfully");
        logger.atInfo().addArgument(courseDto.getCourse()).log("Course created/verified: {}");

        int nofiles = uploadService.processFilesFromArchive(
                courseDto.getId(),
                "Q22",
                2026L,
                archiveFile.toPath(),
                Path.of("C:\\temp\\myquiz")
        );

    }

    /**
     * Test to verify database connectivity.
     * This test validates that the test configuration properly connects to the local database.
     *
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
        CourseDto courseDto = new CourseDto();
        courseDto.setCourse("TEST_DB_CONN");
        courseDto.setDescription("Test Database Connectivity");
        courseDto.setSemester("1");
        courseDto.setStudyYear("2025-2026");
        courseDto.setUniversityYear("1");

        // Save to database (within transaction)
        CourseDto savedCourse = courseService.createCourseIfNotExists(courseDto);

        // Verify course was saved (within transaction)
        assertNotNull(savedCourse, "Course should be saved");
        assertNotNull(savedCourse.getId(), "Course should have an ID");
        logger.atInfo().addArgument(savedCourse.getId()).addArgument(savedCourse.getCourse())
              .log("Database connectivity verified - Course ID: {}, Course: {}");

        // Verify we can retrieve the course (within transaction)
        assertEquals("TEST_DB_CONN", savedCourse.getCourse(),
                   "Retrieved course should match saved course");

        logger.atInfo().log("Note: Course will be rolled back after test due to @Transactional");
    }

    /**
     * Test to verify database connectivity with PERSISTENT data.
     * This test is NOT transactional, so the course WILL persist in the database.
     *
     * Use this test when you want to verify data actually persists to the database
     * and remains after the test completes.
     *
     * WARNING: This test does NOT clean up after itself. The course will remain in the database.
     * Run the 'delete' test or manually delete if needed.
     *
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
        CourseDto courseDto = new CourseDto();
        courseDto.setCourse("TEST_DB_CONN_PERSIST");
        courseDto.setDescription("Test Database Connectivity - Persistent");
        courseDto.setSemester("1");
        courseDto.setStudyYear("2025-2026");
        courseDto.setUniversityYear("1");

        // Save to database (will actually persist)
        CourseDto savedCourse = courseService.createCourseIfNotExists(courseDto);

        // Verify course was saved
        assertNotNull(savedCourse, "Course should be saved");
        assertNotNull(savedCourse.getId(), "Course should have an ID");
        logger.atInfo().addArgument(savedCourse.getId()).addArgument(savedCourse.getCourse())
              .log("Database connectivity verified (PERSISTENT) - Course ID: {}, Course: {}");

        // Verify we can retrieve the course
        assertEquals("TEST_DB_CONN_PERSIST", savedCourse.getCourse(),
                   "Retrieved course should match saved course");

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
        QuizAuthor quizAuthor = createQuizWithQuestions();
        List<Question> result = questionService.getQuestionsForAuthorId(quizAuthor.getAuthor().getId(), quizAuthor.getQuiz().getCourse());
        logger.atInfo().addArgument(result.size()).log("Number of questions: {}");
        result.forEach(question -> logger.atInfo().addArgument(question).log("Question: {}"));
        assertNotNull(result);
    }

    @Test
    void getQuestionsByAuthorName() {
        QuizAuthor quizAuthor = createQuizWithQuestions();
        List<Question> result = questionService.getQuestionsForAuthorName("Diana");
        logger.atInfo().addArgument(result.size()).log("Number of questions: {}");
        result.forEach(question -> logger.atInfo().addArgument(question).log("Question: {}"));
        assertNotNull(result);
    }

    private QuizAuthor createQuizWithQuestions() {
        Author author = new Author("Erika Diana Mustermann", "EDM");

        Quiz quiz = new Quiz();
        quiz.setName("Q1");
        quiz.setCourse("RC");
        quiz.setYear(2024L);

        QuizAuthor quizAuthor = new QuizAuthor();
        quizAuthor.setAuthor(author);
        quizAuthor.setQuiz(quiz);
        quizAuthor.setSource("File-RC.xlsx");

        Set<Question> questions = new HashSet<>();

        Question question1 = new Question();
        question1.setCrtNo(1);
        question1.setTitle("Title Q1");
        question1.setText("Text Q1");
        question1.setQuizAuthor(quizAuthor);
        questions.add(question1);

        Question question2 = new Question();
        question2.setCrtNo(2);
        question2.setTitle("Title Q22");
        question2.setText("Text Q2");
        question2.setQuizAuthor(quizAuthor);
        questions.add(question2);

        quizAuthor.setQuestions(questions);

        quizAuthor = quizAuthorRepository.save(quizAuthor);

        logger.atInfo().addArgument(author).log("Author: {}");
        return quizAuthor;
    }

}