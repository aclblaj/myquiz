package com.unitbv.myquiz.app.upload.application.handler;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.types.TemplateType;
import com.unitbv.myquiz.app.entities.Author;
import com.unitbv.myquiz.app.entities.QuestionBank;
import com.unitbv.myquiz.app.repositories.QuestionBankRepository;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.FileService;
import com.unitbv.myquiz.app.services.QuestionService;
import com.unitbv.myquiz.app.specifications.QuestionBankSpecification;
import com.unitbv.myquiz.app.upload.domain.validation.UploadInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Component
public class ExcelUploadHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadHandler.class);

    private final QuestionService questionService;
    private final AuthorService authorService;
    private final FileService fileService;
    private final CourseService courseService;
    private final UploadInputValidator uploadInputValidator;
    private final QuestionBankRepository questionBankRepository;

    public ExcelUploadHandler(QuestionService questionService,
                              AuthorService authorService,
                              FileService fileService,
                              CourseService courseService,
                              UploadInputValidator uploadInputValidator,
                              QuestionBankRepository questionBankRepository) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.fileService = fileService;
        this.courseService = courseService;
        this.uploadInputValidator = uploadInputValidator;
        this.questionBankRepository = questionBankRepository;
    }

    @Transactional
    public String processExcelUpload(MultipartFile file, String username, Long courseId, String questionBankName, TemplateType templateType) {

        uploadInputValidator.validateExcelUploadInputs(file, username, courseId, questionBankName, templateType);

        logger.atInfo().addArgument(file.getOriginalFilename()).addArgument(username).log("Processing Excel upload: file='{}', author='{}'");

        final String filepath;
        try {
            filepath = fileService.uploadFile(file);
        } catch (IOException e) {
            logger.atError().setCause(e).addArgument(file.getOriginalFilename()).log("Failed to save Excel file '{}' before processing");
            throw new IllegalStateException("Could not save uploaded Excel file: " + e.getMessage(), e);
        }
        logger.atInfo().addArgument(filepath).log("File uploaded to: {}");

        try {
            AuthorDto authorDto = createOrRetrieveAuthor(username);

            if (courseId == null || courseId == 0) {
                throw new IllegalArgumentException("Course ID is required");
            }
            CourseDto courseDto = courseService.findById(courseId);
            if (courseDto == null) {
                throw new IllegalArgumentException("Course not found for ID: " + courseId);
            }

            QuestionBank questionBank = getOrCreateQuestionBank(questionBankName, courseId, courseDto.getCourse(), templateType);
            if (questionBank.getId() != null) {
                logger.atInfo().addArgument(questionBank.getId()).log("QuestionBank retrieved or created with ID: {}");
            } else {
                logger.atInfo().addArgument(questionBank.getName()).log("QuestionBank created with name: {}");
            }

            Author authorEntity = authorService.findAuthorEntityById(authorDto.getId());
            if (authorEntity == null) {
                throw new IllegalStateException("Could not retrieve author entity for ID: " + authorDto.getId());
            }

            String parseResult = questionService.parseFileSheets(questionBank, authorEntity, filepath);
            logger.atInfo().addArgument(parseResult).log("Parse result: {}");

            if (parseResult == null || parseResult.isBlank()) {
                throw new IllegalStateException("Excel parsing produced an empty result");
            }
            if (isParseError(parseResult)) {
                throw new IllegalArgumentException("Failed to parse Excel file: " + parseResult);
            }

            fileService.removeFile(filepath);
            logger.atInfo().addArgument(filepath).log("Cleaned up file: {}");

            return "Successfully uploaded and processed file with " + authorEntity.getName();

        } catch (Exception e) {
            try {
                fileService.removeFile(filepath);
            } catch (Exception cleanupEx) {
                logger.atWarn().setCause(cleanupEx).log("Failed to clean up file after error");
            }
            throw e;
        }
    }

    private boolean isParseError(String parseResult) {
        return parseResult.toLowerCase().contains("error");
    }

    private AuthorDto createOrRetrieveAuthor(String username) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setName(username);
        authorDto.setInitials(authorService.extractInitials(username));

        if (authorService.authorNameExists(authorDto.getName())) {
            logger.atInfo().addArgument(authorDto.getName()).log("Author '{}' already exists");
            return authorService.getAuthorByName(authorDto.getName());
        } else {
            authorDto = authorService.saveAuthorDto(authorDto);
            logger.atInfo().addArgument(authorDto.getName()).log("Created new author: '{}'");
            return authorDto;
        }
    }

    /**
     * Retrieves an existing QuestionBank if one exists with the same name and courseId,
     * otherwise creates a new one.
     *
     * @param name the question bank name
     * @param courseId the course ID
     * @param courseName the course name
     * @param templateType the template type (unused in retrieval, kept for future use)
     * @return the existing or newly created QuestionBank
     */
    private QuestionBank getOrCreateQuestionBank(String name, Long courseId, String courseName, TemplateType templateType) {
        // Search for existing QuestionBank with exact name and courseId match
        Specification<QuestionBank> spec = Specification
                .where(QuestionBankSpecification.hasNameExact(name))
                .and(QuestionBankSpecification.byCourseId(courseId));
        Optional<QuestionBank> existingQuestionBank = questionBankRepository.findOne(spec);

        if (existingQuestionBank.isPresent()) {
            QuestionBank qb = existingQuestionBank.get();
            logger.atInfo().addArgument(qb.getId()).addArgument(name).addArgument(courseId)
                    .log("Found existing QuestionBank with ID '{}', name '{}', courseId '{}'");
            return qb;
        }

        // Create new QuestionBank if it doesn't exist
        logger.atInfo().addArgument(name).addArgument(courseId)
                .log("No existing QuestionBank found for name '{}', courseId '{}'. Creating new one.");
        return createQuestionBank(name, courseName, templateType);
    }

    private QuestionBank createQuestionBank(String name, String course, TemplateType templateType) {
        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(name);
        questionBank.setCourse(courseService.getOrCreateCourseEntity(course));
        questionBank.setStudyYear(null);
        return questionService.saveQuestionBank(questionBank);
    }
}

