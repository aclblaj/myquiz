package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.Quiz;
import com.unitbv.myquiz.app.entities.QuizError;
import com.unitbv.myquiz.api.types.TemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling file upload operations.
 * Implements business logic for Excel and Archive uploads as per upload-sd.md specifications.
 */
@Service
public class UploadService {
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    private final QuestionService questionService;
    private final AuthorService authorService;
    private final FileService fileService;
    private final QuestionValidationService questionValidationService;
    private final CourseService courseService;
    private final QuizAuthorService quizAuthorService;
    private final QuizErrorService quizErrorService;

    public UploadService(
            QuestionService questionService,
            AuthorService authorService,
            QuestionValidationService questionValidationService,
            FileService fileService,
            CourseService courseService,
            QuizAuthorService quizAuthorService, QuizErrorService quizErrorService) {
        this.quizAuthorService = quizAuthorService;
        this.questionService = questionService;
        this.authorService = authorService;
        this.questionValidationService = questionValidationService;
        this.fileService = fileService;
        this.courseService = courseService;
        this.quizErrorService = quizErrorService;
    }

    /**
     * Process Excel file upload following upload-sd.md Section 2.2 specifications.
     *
     * Steps:
     * 1. Save file to temporary location
     * 2. Create or retrieve author
     * 3. Retrieve course information
     * 4. Create quiz with template type
     * 5. Parse Excel file and create questions
     * 6. Validate for duplicates
     * 7. Clean up temporary file
     *
     * @param file Excel file to process
     * @param username Author name
     * @param courseId Course ID
     * @param quizName Quiz short name (e.g., "Q1", "Q2")
     * @param templateType Template type for parsing
     * @return Success message
     * @throws IllegalArgumentException if courseId is invalid
     */
    @Transactional
    public String processExcelUpload(
            MultipartFile file,
            String username,
            Long courseId,
            String quizName,
            TemplateType templateType) {

        logger.atInfo()
              .addArgument(file.getOriginalFilename())
              .addArgument(username)
              .log("Processing Excel upload: file='{}', author='{}'");

        // Step 1: Save file to temporary location
        String filepath = fileService.uploadFile(file);
        logger.atInfo().addArgument(filepath).log("File uploaded to: {}");

        try {
            // Step 2: Create or retrieve author
            AuthorDto authorDto = createOrRetrieveAuthor(username);

            // Step 3: Validate and retrieve course
            if (courseId == null || courseId == 0) {
                throw new IllegalArgumentException("Course ID is required");
            }
            CourseDto courseDto = courseService.findById(courseId);
            if (courseDto == null) {
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }

            // Step 4: Create quiz with template type
            Quiz quiz = createQuiz(quizName, courseDto.getCourse(), templateType);
            logger.atInfo().addArgument(quiz.getId()).log("Quiz created with ID: {}");

            // Step 5: Parse Excel file and create questions
            Author authorEntity = authorService.findAuthorEntityById(authorDto.getId());
            if (authorEntity == null) {
                throw new IllegalStateException("Could not retrieve author entity for ID: " + authorDto.getId());
            }

            String parseResult = questionService.parseFileSheets(
                quiz, authorEntity, filepath);
            logger.atInfo().addArgument(parseResult).log("Parse result: {}");

            // Step 6: Validate for duplicates
            ArrayList<Author> authorEntities = new ArrayList<>();
            authorEntities.add(authorEntity);
            List<QuizError> duplicateErrors = questionValidationService.checkDuplicatesQuestionsForAuthors(
                authorEntities, quiz.getCourse());

            // Step 6.1: Save duplicate errors
            quizErrorService.saveAllQuizErrors(duplicateErrors);
            logger.atInfo()
                  .addArgument(duplicateErrors.size())
                  .log("Duplicate validation completed - found {} duplicate violations");

            // Step 6.2: Link errors to questions (after errors are persisted)
            questionValidationService.linkErrorsToQuestions(duplicateErrors);

            // Step 7: Clean up temporary file
            fileService.removeFile(filepath);
            logger.atInfo().addArgument(filepath).log("Cleaned up file: {}");

            return "Successfully uploaded and processed file with " + authorEntity.getName();

        } catch (Exception e) {
            // Clean up on error
            try {
                fileService.removeFile(filepath);
            } catch (Exception cleanupEx) {
                logger.atWarn().setCause(cleanupEx).log("Failed to clean up file after error");
            }
            throw e;
        }
    }

    /**
     * Process archive (ZIP) upload following upload-sd.md Section 2.4 specifications.
     * OPTIMIZED for performance with batch processing and reduced transaction scope.
     *
     * Steps:
     * 1. Create temporary directory
     * 2. Save archive file
     * 3. Extract ZIP contents (outside transaction)
     * 4. Retrieve course information (outside transaction)
     * 5. Create quiz (in transaction)
     * 6. Parse all Excel files from extracted folder (in transaction with batch saves)
     * 7. Clean up temporary directory and archive
     *
     * @param archive ZIP archive file
     * @param courseId Course ID
     * @param quizName Quiz name
     * @param year Quiz year
     * @return Result with number of files processed
     * @throws IOException if file operations fail
     * @throws IllegalArgumentException if courseId is invalid
     */
    public ArchiveUploadResult processArchiveUpload(
            MultipartFile archive,
            Long courseId,
            String quizName,
            Long year) throws IOException {

        long startTime = System.currentTimeMillis();
        logger.atInfo()
              .addArgument(archive.getOriginalFilename())
              .addArgument(quizName)
              .log("Processing archive upload: file='{}', quiz='{}'");

        Path tempDir = null;
        String archivePath = null;

        try {
            // Step 1: Create temporary directory (fast, outside transaction)
            tempDir = Files.createTempDirectory("uploaded-archive-");
            if (!Files.isWritable(tempDir)) {
                throw new IOException("Temp directory is not writable: " + tempDir);
            }
            logger.atInfo().addArgument(tempDir).log("Created temp directory: {}");

            // Step 2: Save and extract archive (I/O intensive, outside transaction)
            archivePath = fileService.uploadFile(archive);
            Path archiveFilePath = Path.of(archivePath);
            logger.atInfo().addArgument(archivePath).log("Archive uploaded to: {}");

            // Step 3: Extract ZIP contents (I/O intensive, outside transaction)
            long extractStart = System.currentTimeMillis();
            fileService.unzipAndRenameExcelFiles(archiveFilePath, tempDir);
            logger.atInfo()
                  .addArgument(System.currentTimeMillis() - extractStart)
                  .log("Archive extracted in {}ms");

            // Step 4: Validate course (outside transaction)
            CourseDto courseDto = findCourseById(courseId);
            if (courseDto == null) {
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }

            // Step 5-6: Process files in optimized transaction
            int filesProcessed = processFilesFromArchiveOptimized(
                courseDto, quizName, year, tempDir);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.atInfo()
                  .addArgument(filesProcessed)
                  .addArgument(totalTime)
                  .log("Archive upload completed: {} files processed in {}ms");

            return new ArchiveUploadResult(filesProcessed, quizName);

        } catch (Exception e) {
            logger.atError()
                  .setCause(e)
                  .log("Archive upload failed, cleaning up...");
            cleanup(tempDir, archivePath);
            throw e;
        } finally {
            // Always cleanup
            cleanup(tempDir, archivePath);
        }
    }

    /**
     * Optimized version of processFilesFromArchive with better transaction management.
     * Uses a single transaction for quiz creation, parsing, and validation.
     *
     * @param courseDto Course information (pre-fetched)
     * @param quizName Quiz name
     * @param year Quiz year
     * @param tempDir Temporary directory with extracted files
     * @return Number of files processed
     */
    @Transactional
    protected int processFilesFromArchiveOptimized(
            CourseDto courseDto,
            String quizName,
            Long year,
            Path tempDir) {

        long transactionStart = System.currentTimeMillis();

        // Create quiz
        Quiz quiz = new Quiz();
        quiz.setName(quizName);
        quiz.setCourse(courseDto.getCourse());
        quiz.setYear(year);
        quiz = questionService.saveQuiz(quiz);
        logger.atInfo().addArgument(quiz.getId()).log("Quiz created with ID: {}");

        // Parse all Excel files (uses batch saves internally)
        long parseStart = System.currentTimeMillis();
        int filesProcessed = questionService.parseExcelFilesFromFlatFolder(quiz, tempDir.toFile());
        logger.atInfo()
              .addArgument(filesProcessed)
              .addArgument(System.currentTimeMillis() - parseStart)
              .log("Parsed {} files in {}ms");

        // Validate for duplicates with optimized query
        long validationStart = System.currentTimeMillis();
        ArrayList<Author> authorEntities = quizAuthorService.getAuthorsForQuiz(quiz.getId());

        if (!authorEntities.isEmpty()) {
            List<QuizError> duplicateErrors = questionValidationService.checkDuplicatesQuestionsForAuthors(
                authorEntities, quiz.getCourse());

            logger.atInfo()
                  .addArgument(authorEntities.size())
                  .addArgument(duplicateErrors.size())
                  .addArgument(System.currentTimeMillis() - validationStart)
                  .log("Validated {} authors, found {} errors in {}ms");

            // Save errors and link to questions
            if (!duplicateErrors.isEmpty()) {
                quizErrorService.saveAllQuizErrors(duplicateErrors);
                questionValidationService.linkErrorsToQuestions(duplicateErrors);
            }
        } else {
            logger.atWarn().log("No authors found for quiz - skipping duplicate validation");
        }

        logger.atInfo()
              .addArgument(System.currentTimeMillis() - transactionStart)
              .log("Transaction completed in {}ms");

        return filesProcessed;
    }

    /**
     * Original processFilesFromArchive method - kept for backward compatibility.
     * Consider using processFilesFromArchiveOptimized for better performance.
     *
     * @deprecated Use processFilesFromArchiveOptimized instead
     */
    @Transactional
    public int processFilesFromArchive(Long courseId, String quizName, Long year, Path archiveFilePath,
                                       Path tempDir) throws IOException {
        fileService.unzipAndRenameExcelFiles(archiveFilePath, tempDir);
        logger.atInfo().log("Archive extracted successfully");

        // Step 4: Retrieve course information
        CourseDto courseDto = findCourseById(courseId);
        if (courseDto == null) {
            throw new IllegalArgumentException("Course not found for ID: " + courseId);
        }

        // Step 5: Create quiz
        Quiz quiz = new Quiz();
        quiz.setName(quizName);
        quiz.setCourse(courseDto.getCourse());
        quiz.setYear(year);
        quiz = questionService.saveQuiz(quiz);
        logger.atInfo().addArgument(quiz.getId()).log("Quiz created with ID: {}");

        // Step 6: Parse all Excel files from extracted folder
        int filesProcessed = questionService.parseExcelFilesFromFlatFolder(quiz, tempDir.toFile());
        logger.atInfo().addArgument(filesProcessed).log("Processed {} files");

        // Step 7: Validate for duplicates - get all authors who submitted questions for this quiz
        ArrayList<Author> authorEntities = quizAuthorService.getAuthorsForQuiz(quiz.getId());
        if (!authorEntities.isEmpty()) {
            List<QuizError> duplicateErrors = questionValidationService.checkDuplicatesQuestionsForAuthors(
                authorEntities, quiz.getCourse());
            logger.atInfo()
                  .addArgument(authorEntities.size())
                  .addArgument(duplicateErrors.size())
                  .log("Duplicate validation completed for {} authors, found {} duplicate violations");

            // Step 7.1: Save duplicate errors
            quizErrorService.saveAllQuizErrors(duplicateErrors);

            // Step 7.2: Link errors to questions (after errors are persisted)
            questionValidationService.linkErrorsToQuestions(duplicateErrors);
        } else {
            logger.atWarn().log("No authors found for quiz - skipping duplicate validation");
        }

        // Step 8: Clean up temporary directory and archive
        cleanup(tempDir, archiveFilePath.toAbsolutePath().toString());
        return filesProcessed;
    }

    /**
     * Create or retrieve author by name.
     *
     * @param username Author name
     * @return AuthorDto (existing or newly created)
     */
    private AuthorDto createOrRetrieveAuthor(String username) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setName(username);
        authorDto.setInitials(authorService.extractInitials(username));

        if (authorService.authorNameExists(authorDto.getName())) {
            logger.atInfo().addArgument(authorDto.getName())
                  .log("Author '{}' already exists");
            return authorService.getAuthorByName(authorDto.getName());
        } else {
            authorDto = authorService.saveAuthorDto(authorDto);
            logger.atInfo().addArgument(authorDto.getName())
                  .log("Created new author: '{}'");
            return authorDto;
        }
    }

    /**
     * Create quiz with specified parameters.
     *
     * @param name Quiz name
     * @param course Course name
     * @param templateType Template type
     * @return Created Quiz entity
     */
    private Quiz createQuiz(String name, String course, TemplateType templateType) {
        Quiz quiz = new Quiz();
        quiz.setName(name);
        quiz.setCourse(course);
        quiz.setYear(templateType.getYear());
        return questionService.saveQuiz(quiz);
    }

    /**
     * Find course by ID from all courses.
     *
     * @param courseId Course ID
     * @return CourseDto or null if not found
     */
    private CourseDto findCourseById(Long courseId) {
        return courseService.getAllCourses().stream()
            .filter(c -> c.getId().equals(courseId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Clean up temporary files and directories.
     *
     * @param tempDir Temporary directory to delete
     * @param archivePath Archive file path to delete
     */
    private void cleanup(Path tempDir, String archivePath) {
        if (tempDir != null) {
            try {
                FileSystemUtils.deleteRecursively(tempDir);
                logger.atInfo().addArgument(tempDir).log("Deleted temp directory: {}");
            } catch (IOException e) {
                logger.atWarn().setCause(e)
                      .addArgument(tempDir)
                      .log("Failed to delete temp directory: {}");
            }
        }

        if (archivePath != null) {
            try {
                fileService.removeFile(archivePath);
                logger.atInfo().addArgument(archivePath).log("Deleted archive file: {}");
            } catch (Exception e) {
                logger.atWarn().setCause(e)
                      .addArgument(archivePath)
                      .log("Failed to delete archive file: {}");
            }
        }
    }

    /**
     * Result object for archive upload operation.
     */
    public static class ArchiveUploadResult {
        private final int filesProcessed;
        private final String quizName;

        public ArchiveUploadResult(int filesProcessed, String quizName) {
            this.filesProcessed = filesProcessed;
            this.quizName = quizName;
        }

        public int getFilesProcessed() {
            return filesProcessed;
        }

        public String getQuizName() {
            return quizName;
        }

        public String toMessage() {
            return "Imported " + filesProcessed + " files successfully for quiz '" + quizName + "'";
        }
    }
}
