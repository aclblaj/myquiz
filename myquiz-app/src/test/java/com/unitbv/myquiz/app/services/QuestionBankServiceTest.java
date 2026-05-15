package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportAuthorSectionDto;
import com.unitbv.myquiz.api.dto.QuestionBankExportDto;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.api.types.StudyYear;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.entities.QuestionDuplicate;
import com.unitbv.myquiz.app.entities.QuestionError;
import com.unitbv.myquiz.app.repositories.QuestionDuplicateRepository;
import com.unitbv.myquiz.app.repositories.QuestionErrorRepository;
import com.unitbv.myquiz.app.repositories.QuestionRepository;
import com.unitbv.myquiz.app.testutil.ServiceTestData;
import com.unitbv.myquiz.app.testutil.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for QuestionBankService.getQuestionBankById() method.
 * <p>
 * Prerequisites:
 * - Docker PostgreSQL container running (from docker-compose.yml)
 * - Database exposed on localhost:5433
 * - Database name: myquiz
 * - Username: myquiz_user
 * - Password: myquiz_password
 * <p>
 * To start the database: docker-compose up postgres -d
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
class QuestionBankServiceTest {

    private static final Logger log = LoggerFactory.getLogger(QuestionBankServiceTest.class);

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionDuplicateRepository questionDuplicateRepository;

    @Autowired
    private QuestionErrorRepository questionErrorRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    private QuestionBank testQuestionBank;
    private QuestionBankAuthor testQuestionBankAuthor;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    void setUp() {
        TestEntityFactory.QuestionBankAuthorFixture fixture = testEntityFactory.createQuestionBankAuthorFixture(
                TestEntityFactory.QuestionBankAuthorSpec.builder()
                        .authorName("Test Author")
                        .initials("TA")
                        .questionBankName("Test QuestionBank")
                        .course("Test Course")
                        .studyYear(ServiceTestData.STUDY_YEAR)
                        .source("test_source.xlsx")
                        .build()
        );

        testQuestionBank = fixture.questionBank();
        Author testAuthor = fixture.author();
        testQuestionBankAuthor = fixture.questionBankAuthor();

        log.info("Test data setup complete: QuestionBank ID={}, Author ID={}", testQuestionBank.getId(), testAuthor.getId());
    }

    /**
     * Test 1: Successful retrieval of a question bank with full details
     * Validates that getQuestionBankById returns a complete QuestionBankDto with all fields populated
     */
    @Test
    void testGetQuestionBankById_SuccessfulRetrieval() {
        log.info("Starting test: getQuestionBankById with successful retrieval");

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertNotNull(result, "QuestionBankDto should not be null");
        assertEquals(testQuestionBank.getId(), result.getId(), "QuestionBank ID should match");
        assertEquals("Test QuestionBank", result.getName(), "QuestionBank name should match");
        assertEquals("Test Course", result.getCourse(), "Course should match");
        assertEquals(ServiceTestData.STUDY_YEAR, result.getStudyYear(), "Study year should match");
        assertEquals(1, result.getNoAuthors(), "Should have 1 author");
        assertNotNull(result.getAuthors(), "Authors list should not be null");
        assertEquals(1, result.getAuthors().size(), "Should have 1 author in the list");
        assertEquals("Test Author", result.getAuthors().getFirst().getName(), "Author name should match");

        log.info("Test passed: getQuestionBankById successfully retrieved question bank with ID {}", testQuestionBank.getId());
    }

    /**
     * Test 2: Retrieval of question bank with multiple choice questions
     * Validates that MC questions are properly counted and categorized
     */
    @Test
    void testGetQuestionBankById_WithMultipleChoiceQuestions() {
        log.info("Starting test: getQuestionBankById with MC questions");

        // Add MC questions
        Question mcQuestion1 = new Question();
        mcQuestion1.setQuestionBankAuthor(testQuestionBankAuthor);
        mcQuestion1.setType(QuestionType.MULTICHOICE);
        mcQuestion1.setText("MC Question 1");
        mcQuestion1.setResponse1("Option A");
        mcQuestion1.setResponse2("Option B");
        mcQuestion1.setResponse3("Option C");
        mcQuestion1.setResponse4("Option D");
        mcQuestion1.setWeightResponse1(1.0);
        questionRepository.save(mcQuestion1);

        Question mcQuestion2 = new Question();
        mcQuestion2.setQuestionBankAuthor(testQuestionBankAuthor);
        mcQuestion2.setType(QuestionType.MULTICHOICE);
        mcQuestion2.setText("MC Question 2");
        mcQuestion2.setResponse1("Option A");
        mcQuestion2.setResponse2("Option B");
        mcQuestion2.setResponse3("Option C");
        mcQuestion2.setResponse4("Option D");
        mcQuestion2.setWeightResponse2(1.0);
        questionRepository.save(mcQuestion2);

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertNotNull(result.getQuestionsMultichoice(), "MC questions list should not be null");
        assertEquals(2, result.getQuestionsMultichoice().size(), "Should have 2 MC questions");
        log.info("Test passed: getQuestionBankById correctly retrieved {} MC questions", result.getQuestionsMultichoice().size());
    }

    /**
     * Test 3: Retrieval of question bank with true/false questions
     * Validates that TF questions are properly counted and categorized
     */
    @Test
    void testgetQuestionBankById_WithTrueFalseQuestions() {
        log.info("Starting test: getQuestionBankById with TF questions");

        // Add TF questions
        Question tfQuestion1 = new Question();
        tfQuestion1.setQuestionBankAuthor(testQuestionBankAuthor);
        tfQuestion1.setType(QuestionType.TRUEFALSE);
        tfQuestion1.setText("This is true");
        tfQuestion1.setWeightTrue(1.0);
        tfQuestion1.setWeightFalse(0.0);
        questionRepository.save(tfQuestion1);

        Question tfQuestion2 = new Question();
        tfQuestion2.setQuestionBankAuthor(testQuestionBankAuthor);
        tfQuestion2.setType(QuestionType.TRUEFALSE);
        tfQuestion2.setText("This is false");
        tfQuestion2.setWeightTrue(0.0);
        tfQuestion2.setWeightFalse(1.0);
        questionRepository.save(tfQuestion2);

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertNotNull(result.getQuestionsTruefalse(), "TF questions list should not be null");
        assertEquals(2, result.getQuestionsTruefalse().size(), "Should have 2 TF questions");
        log.info("Test passed: getQuestionBankById correctly retrieved {} TF questions", result.getQuestionsTruefalse().size());
    }

    /**
     * Test 4: Retrieval of question bank with mixed question types
     * Validates that MC and TF questions are both returned and properly separated
     */
    @Test
    void testgetQuestionBankById_WithMixedQuestionTypes() {
        log.info("Starting test: getQuestionBankById with mixed question types");

        // Add MC question
        Question mcQuestion = new Question();
        mcQuestion.setQuestionBankAuthor(testQuestionBankAuthor);
        mcQuestion.setType(QuestionType.MULTICHOICE);
        mcQuestion.setText("MC Question");
        mcQuestion.setResponse1("Option A");
        mcQuestion.setResponse2("Option B");
        mcQuestion.setResponse3("Option C");
        mcQuestion.setResponse4("Option D");
        mcQuestion.setWeightResponse1(1.0);
        questionRepository.save(mcQuestion);

        // Add TF question
        Question tfQuestion = new Question();
        tfQuestion.setQuestionBankAuthor(testQuestionBankAuthor);
        tfQuestion.setType(QuestionType.TRUEFALSE);
        tfQuestion.setText("TF Question");
        tfQuestion.setWeightTrue(1.0);
        tfQuestion.setWeightFalse(0.0);
        questionRepository.save(tfQuestion);

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertEquals(1, result.getQuestionsMultichoice().size(), "Should have 1 MC question");
        assertEquals(1, result.getQuestionsTruefalse().size(), "Should have 1 TF question");
        log.info("Test passed: getQuestionBankById correctly separated {} MC and {} TF questions", result.getQuestionsMultichoice().size(), result.getQuestionsTruefalse().size());
    }

    /**
     * Test 5: Null ID should throw IllegalArgumentException
     * Validates input validation for null ID parameter
     */
    @Test
    void testgetQuestionBankById_WithNullId() {
        log.info("Starting test: getQuestionBankById with null ID");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> questionBankService.getQuestionBankById(null), "Should throw IllegalArgumentException for null ID");

        assertTrue(exception.getMessage().contains("cannot be null"), "Exception message should indicate null ID");
        log.info("Test passed: getQuestionBankById correctly rejected null ID with message: {}", exception.getMessage());
    }

    /**
     * Test 6: Non-existent question bank ID should throw IllegalArgumentException
     * Validates that a 404-like scenario is handled correctly
     */
    @Test
    void testgetQuestionBankById_WithNonExistentId() {
        log.info("Starting test: getQuestionBankById with non-existent ID");

        Long nonExistentId = 99999L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> questionBankService.getQuestionBankById(nonExistentId),
                "Should throw IllegalArgumentException for non-existent question bank"
        );

        assertTrue(exception.getMessage().contains("not found"), "Exception message should indicate question bank not found");
        assertTrue(exception.getMessage().contains(nonExistentId.toString()), "Exception message should include the question bank ID");
        log.info("Test passed: getQuestionBankById correctly threw exception for non-existent ID: {}", exception.getMessage());
    }

    /**
     * Test 7: QuestionBank with multiple authors
     * Validates that all authors linked to the question bank are returned
     */
    @Test
    void testgetQuestionBankById_WithMultipleAuthors() {
        log.info("Starting test: getQuestionBankById with multiple authors");

        // Create a second author
        Author secondAuthor = testEntityFactory.createAuthor("Second Author", "SA");

        // Link second author to question bank
        testEntityFactory.createQuestionBankAuthor(secondAuthor, testQuestionBank, "test_source_2.xlsx");

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertEquals(2, result.getNoAuthors(), "Should have 2 authors");
        assertEquals(2, result.getAuthors().size(), "Should return both authors");

        List<String> authorNames = result.getAuthors().stream().map(AuthorDto::getName).toList();
        assertTrue(authorNames.contains("Test Author"), "Should contain first author");
        assertTrue(authorNames.contains("Second Author"), "Should contain second author");

        log.info("Test passed: getQuestionBankById correctly retrieved {} authors", result.getNoAuthors());
    }

    /**
     * Test 8: QuestionBank source file is correctly retrieved
     * Validates that the sourceFile attribute is populated from QuestionBankAuthor
     */
    @Test
    void testgetQuestionBankById_SourceFilePopulation() {
        log.info("Starting test: getQuestionBankById source file population");

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertNotNull(result.getSourceFile(), "Source file should not be null");
        assertEquals("test_source.xlsx", result.getSourceFile(), "Source file should match");
        log.info("Test passed: getQuestionBankById correctly populated source file: {}", result.getSourceFile());
    }

    /**
     * Test 9: QuestionBank initial creation flow and retrieval
     * Validates the complete flow from question bank creation to retrieval
     */
    @Test
    void testgetQuestionBankById_AfterQuestionBankCreation() {
        log.info("Starting test: getQuestionBankById after question bank creation");

        // Create a new question bank using questionBankService
        QuestionBank newQuestionBank = questionBankService.createQuestionBank("Integration Test Course", "Integration Test QuestionBank", ServiceTestData.STUDY_YEAR);

        assertNotNull(newQuestionBank, "Created question bank should not be null");
        assertNotNull(newQuestionBank.getId(), "QuestionBank ID should be assigned");

        // Retrieve the question bank
        QuestionBankDto result = questionBankService.getQuestionBankById(newQuestionBank.getId());

        assertNotNull(result, "Retrieved QuestionBankDto should not be null");
        assertEquals("Integration Test QuestionBank", result.getName(), "QuestionBank name should match");
        assertEquals("Integration Test Course", result.getCourse(), "Course should match");
        assertEquals(ServiceTestData.STUDY_YEAR, result.getStudyYear(), "Study year should match");

        log.info("Test passed: getQuestionBankById successfully retrieved question bank created with ID {}", newQuestionBank.getId());
    }

    /**
     * Test 10: Empty question bank (no questions, no authors) should still be retrievable
     * Validates that the method handles minimal data correctly
     */
    @Test
    void testgetQuestionBankById_EmptyQuestionBank() {
        log.info("Starting test: getQuestionBankById with empty question bank");

        // Create an empty question bank (no questions, no authors)
        QuestionBank emptyQuestionBank = testEntityFactory.createQuestionBank("Empty QuestionBank", "Empty Course", ServiceTestData.STUDY_YEAR);

        QuestionBankDto result = questionBankService.getQuestionBankById(emptyQuestionBank.getId());

        assertNotNull(result, "Empty question bank should be retrievable");
        assertEquals("Empty QuestionBank", result.getName(), "QuestionBank name should match");
        assertEquals(0, result.getNoAuthors(), "Should have 0 authors");
        assertEquals(0, result.getQuestionsMultichoice().size(), "Should have 0 MC questions");
        assertEquals(0, result.getQuestionsTruefalse().size(), "Should have 0 TF questions");
        assertEquals(0L, result.getNumberOfDuplicates(), "Should have 0 duplicate questions");

        log.info("Test passed: getQuestionBankById correctly handled empty question bank");
    }

    @Test
    void testGetQuestionBankById_PopulatesNumberOfDuplicates() {
        Question localQuestion = new Question();
        localQuestion.setQuestionBankAuthor(testQuestionBankAuthor);
        localQuestion.setType(QuestionType.MULTICHOICE);
        localQuestion.setCrtNo(1);
        localQuestion.setTitle("Local duplicate title");
        localQuestion.setText("Local duplicate text");
        localQuestion.setResponse1("A1");
        localQuestion.setResponse2("A2");
        localQuestion.setResponse3("A3");
        localQuestion.setResponse4("A4");
        localQuestion = questionRepository.save(localQuestion);

        TestEntityFactory.QuestionBankAuthorFixture otherFixture = testEntityFactory.createQuestionBankAuthorFixture(
                "Other Author", "OA", "Other QuestionBank", "Other Course", StudyYear.Y2024_2025, "other.xlsx"
        );

        Question externalQuestion = new Question();
        externalQuestion.setQuestionBankAuthor(otherFixture.questionBankAuthor());
        externalQuestion.setType(QuestionType.MULTICHOICE);
        externalQuestion.setCrtNo(1);
        externalQuestion.setTitle("External duplicate title");
        externalQuestion.setText("External duplicate text");
        externalQuestion.setResponse1("B1");
        externalQuestion.setResponse2("B2");
        externalQuestion.setResponse3("B3");
        externalQuestion.setResponse4("B4");
        externalQuestion = questionRepository.save(externalQuestion);

        QuestionDuplicate duplicate = new QuestionDuplicate();
        duplicate.setQuestion(localQuestion);
        duplicate.setDuplicateQuestion(externalQuestion);
        questionDuplicateRepository.save(duplicate);

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertEquals(1L, result.getNumberOfDuplicates(), "Should count duplicated questions that belong to this question bank");
    }

    /**
     * Test 11: Verify question data integrity when retrieved
     * Validates that question details are correctly mapped to QuestionDto
     */
    @Test
    void testgetQuestionBankById_QuestionDataIntegrity() {
        log.info("Starting test: getQuestionBankById question data integrity");

        // Add a detailed MC question
        Question mcQuestion = new Question();
        mcQuestion.setQuestionBankAuthor(testQuestionBankAuthor);
        mcQuestion.setType(QuestionType.MULTICHOICE);
        mcQuestion.setTitle("Question Title");
        mcQuestion.setChapter("Chapter 1");
        mcQuestion.setText("What is the correct answer?");
        mcQuestion.setResponse1("Option A");
        mcQuestion.setResponse2("Option B");
        mcQuestion.setResponse3("Option C");
        mcQuestion.setResponse4("Option D");
        mcQuestion.setWeightResponse1(1.0);
        mcQuestion.setWeightResponse2(0.0);
        mcQuestion.setWeightResponse3(0.0);
        mcQuestion.setWeightResponse4(0.0);
        questionRepository.save(mcQuestion);

        QuestionBankDto result = questionBankService.getQuestionBankById(testQuestionBank.getId());

        assertEquals(1, result.getQuestionsMultichoice().size(), "Should have 1 MC question");
        var retrievedQuestion = result.getQuestionsMultichoice().getFirst();

        assertNotNull(retrievedQuestion, "Question should not be null");
        assertEquals("What is the correct answer?", retrievedQuestion.getText(), "Question text should match");
        assertEquals("Chapter 1", retrievedQuestion.getChapter(), "Chapter should match");
        assertEquals("Option A", retrievedQuestion.getResponse1(), "Response 1 should match");
        assertEquals("Option B", retrievedQuestion.getResponse2(), "Response 2 should match");

        log.info("Test passed: getQuestionBankById correctly maintained question data integrity");
    }

    @Test
    void testgetQuestionBankExtendedById_SortsAuthorsAndGroupsSections() {
        Question mcQuestion = new Question();
        mcQuestion.setQuestionBankAuthor(testQuestionBankAuthor);
        mcQuestion.setType(QuestionType.MULTICHOICE);
        mcQuestion.setCrtNo(2);
        mcQuestion.setTitle("MC Title");
        mcQuestion.setText("MC Text");
        mcQuestion.setResponse1("A1");
        mcQuestion.setResponse2("A2");
        mcQuestion.setResponse3("A3");
        mcQuestion.setResponse4("A4");
        mcQuestion = questionRepository.save(mcQuestion);

        Question tfQuestion = new Question();
        tfQuestion.setQuestionBankAuthor(testQuestionBankAuthor);
        tfQuestion.setType(QuestionType.TRUEFALSE);
        tfQuestion.setCrtNo(3);
        tfQuestion.setTitle("TF Title");
        tfQuestion.setText("TF Text");
        tfQuestion.setResponse1("TRUE");
        questionRepository.save(tfQuestion);

        QuestionError questionError = new QuestionError();
        questionError.setQuestion(mcQuestion);
        questionError.setDescription("Validation issue");
        questionError.setRowNumber(mcQuestion.getCrtNo());
        questionErrorRepository.save(questionError);

        Author alphaAuthor = testEntityFactory.createAuthor("Alpha Author", "AA");
        QuestionBankAuthor alphaQuestionBankAuthor = testEntityFactory.createQuestionBankAuthor(alphaAuthor, testQuestionBank, "alpha.xlsx");

        Question alphaTf = new Question();
        alphaTf.setQuestionBankAuthor(alphaQuestionBankAuthor);
        alphaTf.setType(QuestionType.TRUEFALSE);
        alphaTf.setCrtNo(1);
        alphaTf.setTitle("Alpha TF");
        alphaTf.setText("Alpha TF text");
        alphaTf.setResponse1("FALSE");
        alphaTf = questionRepository.save(alphaTf);

        QuestionDuplicate duplicate = new QuestionDuplicate();
        duplicate.setQuestion(mcQuestion);
        duplicate.setDuplicateQuestion(alphaTf);
        questionDuplicateRepository.save(duplicate);

        QuestionBankExportDto result = questionBankService.getQuestionBankExtendedById(testQuestionBank.getId());

        assertNotNull(result);
        assertNotNull(result.getQuestionBank());
        assertEquals(testQuestionBank.getId(), result.getQuestionBank().getId());
        assertEquals(2, result.getAuthorSections().size());
        assertEquals("Alpha Author", result.getAuthorSections().get(0).getAuthor().getName());
        assertEquals("Test Author", result.getAuthorSections().get(1).getAuthor().getName());

        QuestionBankExportAuthorSectionDto alphaSection = result.getAuthorSections().get(0);
        assertEquals(0, alphaSection.getMultipleChoiceQuestions().size());
        assertEquals(1, alphaSection.getTrueFalseQuestions().size());
        assertEquals(1, alphaSection.getDuplicateQuestions().size());
        assertEquals("Alpha TF", alphaSection.getDuplicateQuestions().getFirst().getTitle());

        QuestionBankExportAuthorSectionDto testAuthorSection = result.getAuthorSections().get(1);
        assertEquals(1, testAuthorSection.getMultipleChoiceQuestions().size());
        assertEquals(1, testAuthorSection.getTrueFalseQuestions().size());
        assertEquals(1, testAuthorSection.getErrors().size());
        assertEquals(1, testAuthorSection.getDuplicateQuestions().size());
        assertEquals("MC Title", testAuthorSection.getDuplicateQuestions().getFirst().getTitle());
    }
}










