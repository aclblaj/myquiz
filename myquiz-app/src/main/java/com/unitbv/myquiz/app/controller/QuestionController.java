package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorInfo;
import com.unitbv.myquiz.api.dto.CourseDto;
import com.unitbv.myquiz.api.dto.CourseInfo;
import com.unitbv.myquiz.api.dto.DuplicateUnlinkRequestDto;
import com.unitbv.myquiz.api.dto.QuestionBankDto;
import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.interfaces.QuestionApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.entities.QuestionBankAuthor;
import com.unitbv.myquiz.app.services.AuthorService;
import com.unitbv.myquiz.app.services.CourseService;
import com.unitbv.myquiz.app.services.QuestionBankAuthorService;
import com.unitbv.myquiz.app.services.QuestionBankService;
import com.unitbv.myquiz.app.services.QuestionCorrectionService;
import com.unitbv.myquiz.app.services.QuestionService;
import com.unitbv.myquiz.app.web.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/questions")
public class QuestionController implements QuestionApi {

    // Use SLF4J's log.info for consistency and compatibility
    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);
    private static final String LOG_QUESTION_NOT_FOUND = "Question not found with id: {}";
    // Use constructor injection for all dependencies
    private final QuestionService questionService;
    private final AuthorService authorService;
    private final CourseService courseService;
    private final QuestionBankService questionBankService;
    private final QuestionBankAuthorService questionBankAuthorService;
    private final QuestionCorrectionService questionCorrectionService;

    public QuestionController(QuestionService questionService, AuthorService authorService, CourseService courseService, QuestionBankService questionBankService, QuestionBankAuthorService questionBankAuthorService,
                              QuestionCorrectionService questionCorrectionService) {
        this.questionService = questionService;
        this.authorService = authorService;
        this.courseService = courseService;
        this.questionBankService = questionBankService;
        this.questionBankAuthorService = questionBankAuthorService;
        this.questionCorrectionService = questionCorrectionService;
    }

    @Override
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        log.atInfo().log("Getting all questions");
        try {
            List<QuestionDto> questions = questionService.getAllQuestions();
            return ResponseEntity.ok(questions != null ? questions : new ArrayList<>());
        } catch (Exception e) {
            log.atError().setCause(e).log("Error getting all questions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<QuestionDto> getQuestionById(@PathVariable Long id) {
        log.atInfo().addArgument(id).log("Getting question by id: {}");
        try {
            QuestionDto question = questionService.getQuestionById(id);
            return question != null ? ResponseEntity.ok(question) : ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            log.atWarn().addArgument(id).log(LOG_QUESTION_NOT_FOUND);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error getting question by id: {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping({"", "/"})
    @Override
    public ResponseEntity<QuestionDto> createQuestion(@RequestBody QuestionDto questionDto) {
        log.atInfo().log("Creating new question");
        try {
            if (questionDto == null) {
                log.atWarn().log("Received null question data");
                return ResponseEntity.badRequest().build();
            }
            QuestionDto createdQuestion = questionService.createQuestion(questionDto);
            if (createdQuestion == null) {
                log.atError().log("Service returned null after creating question");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error creating question");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<QuestionDto> updateQuestion(@PathVariable Long id, @RequestBody QuestionDto questionDto) {
        log.atInfo().addArgument(id).log("Updating question with id: {}");
        try {
            questionDto.setId(id);
            QuestionDto updatedQuestion = questionService.updateQuestion(questionDto);
            return updatedQuestion != null ? ResponseEntity.ok(updatedQuestion) : ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            log.atWarn().addArgument(id).log(LOG_QUESTION_NOT_FOUND);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error updating question with id: {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        log.atInfo().addArgument(id).log("Deleting question with id: {}");
        try {
            boolean deleted = questionService.deleteQuestion(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (ResourceNotFoundException e) {
            log.atWarn().addArgument(id).log(LOG_QUESTION_NOT_FOUND);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error deleting question with id: {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(ControllerSettings.API_QUESTION_BANKS_GET_BY_ID)
    @Override
    public ResponseEntity<QuestionFilterResponseDto> getQuestionsByQuestionBankId(@PathVariable Long questionBankId) {
        log.atInfo().addArgument(questionBankId).log("Getting questions by questionBank id: {}");
        try {
            QuestionBankDto questionBankById = questionBankService.getQuestionBankById(questionBankId);
            if (questionBankById == null) {
                log.atWarn().addArgument(questionBankId).log("QuestionBank not found with id: {}");
                return ResponseEntity.notFound().build();
            }

            List<QuestionDto> questions = questionService.getQuestionsByQuestionBankId(questionBankId);
            if (questions == null) {
                questions = new ArrayList<>();
            }

            QuestionFilterResponseDto filteredQuestionsDto = QuestionFilterResponseDto.builder()
                    .questions(questions)
                    .selectedQuestionBank(questionBankById)
                    .selectedCourse(questionBankById.getCourse())
                    .selectedCourseId(questionBankById.getCourseId())
                    .selectedQuestionBankId(questionBankId)
                    .build();

            // Populate authors and all courses for consistency with main filter endpoint
            if (questionBankById.getCourse() != null) {
                List<AuthorInfo> authors = authorService.getAuthorsByCourse(questionBankById.getCourse());
                filteredQuestionsDto.setAuthors(authors != null ? authors : new ArrayList<>());
            } else {
                List<AuthorInfo> allAuthors = authorService.getAllAuthorsBasic();
                filteredQuestionsDto.setAuthors(allAuthors != null ? allAuthors : new ArrayList<>());
            }

            // Populate course list for dropdown
            List<CourseInfo> allCourses = courseService.getAllCourses().stream()
                    .map(CourseInfo::from)
                    .toList();
            filteredQuestionsDto.setAllCourses(allCourses);

            return ResponseEntity.ok(filteredQuestionsDto);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(questionBankId).log("Error getting questions by questionBank id: {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/filter")
    @Override
    public ResponseEntity<QuestionFilterResponseDto> listQuestionsFiltered(@RequestBody QuestionFilterRequestDto filterInput) {
        log.atInfo().addArgument(filterInput).log("Filtering questions with input: {}");

        // Validate and normalize input
        if (filterInput == null) {
            return ResponseEntity.badRequest().build();
        }

        AuthorInfo authorInfo = resolveAuthorInfo(filterInput.getAuthorId());
        String selectedCourse = null;
        Long selectedCourseId = filterInput.getCourseId();
        if (selectedCourseId != null) {
            selectedCourse = courseService.getCourseName(selectedCourseId);
        } else if (filterInput.getCourse() != null) {
            selectedCourse = filterInput.getCourse().trim();
        }
        if (selectedCourse != null && selectedCourse.isEmpty()) {
            selectedCourse = null;
        }

        // Guard against invalid page/pageSize
        int validPage = (filterInput.getPage() != null && filterInput.getPage() > 0) ? filterInput.getPage() : 1;
        int validPageSize = (filterInput.getPageSize() != null && filterInput.getPageSize() > 0) ? filterInput.getPageSize() : 10;

        QuestionFilterResponseDto filteredQuestions = questionService.getQuestionsFiltered(
                selectedCourse, authorInfo.getId(), validPage, validPageSize, filterInput.getQuestionBank(),
                filterInput.getQuestionType()
        );

        // Enrich response with resolved author information (name resolved from ID)
        filteredQuestions.setAuthorName(authorInfo.getName());
        filteredQuestions.setSelectedCourseId(selectedCourseId);

        return ResponseEntity.ok(filteredQuestions);
    }

    /**
     * Get duplicates for a specific question.
     * Returns the question and all its related duplicates.
     *
     * @param id The question ID
     * @return Question DTO enriched with duplicates
     */
    @GetMapping("/{id}/duplicates")
    @Operation(summary = "Get question duplicates", description = "Retrieve a question and all its duplicate links")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicates retrieved successfully"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<QuestionDto> getQuestionDuplicates(@Parameter(description = "Question ID") @PathVariable Long id) {
        log.atInfo().addArgument(id).log("Getting duplicates for question {}");
        try {
            QuestionDto questionWithDuplicates = questionService.getQuestionWithDuplicates(id);
            if (questionWithDuplicates == null) {
                log.atWarn().addArgument(id).log("Question not found: {}");
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(questionWithDuplicates);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error getting duplicates for question {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove specific duplication links for a question.
     * This keeps both questions intact but removes the duplication association.
     *
     * @param id           The primary question ID
     * @param selectionDto Contains list of duplicate IDs to unlink
     * @return Success response
     */
    @PostMapping(ControllerSettings.API_QUESTION_BANKS_DUPLICATES_REMOVE_BY_ID)
    @Override
    @Operation(summary = "Remove duplication links", description = "Remove specific duplication links without deleting questions")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Duplication links removed successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<Void> removeQuestionDuplicates(@PathVariable Long id, @RequestBody DuplicateUnlinkRequestDto selectionDto) {
        log.atInfo().addArgument(id).log("Removing duplication links for question {}");
        try {
            if (selectionDto == null || selectionDto.getDuplicateQuestionIds() == null || selectionDto.getDuplicateQuestionIds().isEmpty()) {
                log.atWarn().log("Invalid duplicate selection provided");
                return ResponseEntity.badRequest().build();
            }

            boolean removed = questionService.removeDuplicationLinks(id, selectionDto.getDuplicateQuestionIds());

            if (removed) {
                log.atInfo().addArgument(id).addArgument(selectionDto.getDuplicateQuestionIds().size()).log("Removed {} duplication links for question {}");
                return ResponseEntity.noContent().build();
            } else {
                log.atWarn().addArgument(id).log("Question not found: {}");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error removing duplication links for question {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a question that is marked as a duplicate.
     * This completely removes the question and all its associations.
     *
     * @param id The question ID to delete
     * @return Success response
     */
    @DeleteMapping("/{id}/as-duplicate")
    @Operation(summary = "Delete duplicate question", description = "Delete a question marked as duplicate including all associations")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicate question deleted successfully"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<Map<String, Object>> deleteDuplicateQuestion(@PathVariable Long id) {
        log.atInfo().addArgument(id).log("Deleting duplicate question {}");
        try {
            boolean deleted = questionService.removeDuplicateQuestion(id);

            Map<String, Object> response = new HashMap<>();
            response.put("status", deleted ? "success" : "not_found");
            response.put("message", deleted ? "Duplicate question deleted successfully" : "Question not found");
            response.put("questionId", id);

            if (deleted) {
                log.atInfo().addArgument(id).log("Successfully deleted duplicate question {}");
            } else {
                log.atWarn().addArgument(id).log("Question not found for deletion: {}");
            }

            return deleted ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error deleting duplicate question {}");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error deleting question: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all questions with duplicates in a specified course.
     * Returns a filtered list showing only questions that have duplication errors.
     *
     * @param course The course name
     * @return List of questions with duplicates
     */
    @GetMapping("/course/{course}/with-duplicates")
    @Operation(summary = "Get duplicates in course", description = "Retrieve all questions with duplicates in a specific course")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicates list retrieved successfully"), @ApiResponse(responseCode = "400", description = "Invalid course name"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<Map<String, Object>> getQuestionsWithDuplicatesInCourse(@PathVariable String course) {
        log.atInfo().addArgument(course).log("Getting questions with duplicates in course {}");
        try {
            if (course == null || course.trim().isEmpty()) {
                log.atWarn().log("Invalid course name provided");
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Course name cannot be empty"));
            }

            List<QuestionDto> duplicates = questionService.getQuestionsWithDuplicates(course);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("course", course);
            response.put("duplicateCount", duplicates != null ? duplicates.size() : 0);
            response.put("questions", duplicates != null ? duplicates : new ArrayList<>());

            log.atInfo().addArgument(course).addArgument(duplicates != null ? duplicates.size() : 0).log("Found {} questions with duplicates in course {}");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(course).log("Error getting duplicates for course {}");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error retrieving duplicates: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/correction/grammar")
    @Operation(summary = "Correct grammar in question", description = "Use AI to correct grammar and spelling errors")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Grammar correction completed successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<QuestionCorrectionDto> correctGrammar(@PathVariable("id") Long id, @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.atInfo().addArgument(id).log("[APP] Processing grammar correction for question {}");
            QuestionCorrectionDto result = questionCorrectionService.correctGrammar(correctionDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error correcting grammar");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/correction/improve")
    @Operation(summary = "Improve question", description = "Use AI to improve question clarity and precision")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question improvement completed successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<QuestionCorrectionDto> improveQuestion(@PathVariable("id") Long id, @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.atInfo().addArgument(id).log("[APP] Processing question improvement for question {}");
            QuestionCorrectionDto result = questionCorrectionService.improveQuestion(correctionDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error improving question");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/correction/alternatives")
    @Operation(summary = "Generate alternative answers", description = "Use AI to generate plausible but incorrect alternatives")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Alternatives generated successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<Map<String, String>> generateAlternatives(@PathVariable("id") Long id, @RequestBody QuestionCorrectionDto correctionDto) {
        try {
            log.atInfo().addArgument(id).log("[APP] Processing generate alternatives for question {}");
            String alternatives = questionCorrectionService.generateAlternatives(correctionDto);
            Map<String, String> response = new HashMap<>();
            response.put("alternatives", alternatives);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error generating alternatives");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/correction/explanation")
    @Operation(summary = "Explain correct answer", description = "Use AI to explain why an answer is correct and others are wrong")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Answer explanation generated successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<Map<String, String>> explainAnswer(@PathVariable("id") Long id, @RequestBody QuestionCorrectionDto correctionDto) {
        log.atInfo().addArgument(id).log("[APP] Processing explain answer for question {}");
        try {
            String explanation = questionCorrectionService.explainAnswer(correctionDto);
            Map<String, String> response = new HashMap<>();
            response.put("explanation", explanation);
            response.put("status", "success");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.atError().setCause(e).addArgument(id).log("Explain answer interrupted for question {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(id).log("Error explaining answer for question {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resolves author information from a numeric author ID.
     */
    private AuthorInfo resolveAuthorInfo(Long authorId) {
        if (authorId == null) {
            return new AuthorInfo(null, "", null);
        }

        AuthorDto author = authorService.getAuthorById(authorId);
        String authorName = author != null ? author.getName() : "";
        String authorInitials = author != null ? author.getInitials() : null;
        return new AuthorInfo(authorId, authorName, authorInitials);
    }


    @GetMapping("/author/{authorId}/question-bank/{questionBankId}")
    @Operation(summary = "Get questions by author and QuestionBank", description = "Retrieve all questions for a specific QuestionBank created by a specific author")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved questions"), @ApiResponse(responseCode = "404", description = "QuestionBank author combination not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    public ResponseEntity<QuestionFilterResponseDto> getQuestionsByAuthorAndQuestionBank(@Parameter(description = "Author ID", required = true) @PathVariable Long authorId,
                                                                                 @Parameter(description = "QuestionBank ID", required = true) @PathVariable Long questionBankId) {
        log.atInfo().addArgument(authorId).addArgument(questionBankId).log("Getting questions by author {} and questionBank {}");
        try {
            var qaOpt = questionBankAuthorService.getQuestionBankAuthorByQuestionBankIdAndAuthorId(questionBankId, authorId);
            if (qaOpt.isEmpty()) {
                log.atWarn().addArgument(authorId).addArgument(questionBankId).log("No QuestionBankAuthor found for author {} and questionBank {}");
                return ResponseEntity.notFound().build();
            }
            QuestionBankAuthor questionBankAuthor = qaOpt.get();
            QuestionBankDto questionBankDto = questionBankService.getQuestionBankById(questionBankId);
            if (questionBankDto == null) {
                return ResponseEntity.notFound().build();
            }
            List<QuestionDto> questions = questionService.getQuestionBankQuestionsForAuthor(questionBankAuthor.getId()).stream()
                    .map(q -> questionService.getQuestionById(q.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            QuestionFilterResponseDto dto = QuestionFilterResponseDto.builder()
                    .questions(questions)
                    .selectedQuestionBank(questionBankDto)
                    .selectedCourse(questionBankDto.getCourse())
                    .selectedCourseId(questionBankDto.getCourseId())
                    .selectedAuthorId(authorId)
                    .selectedQuestionBankId(questionBankId)
                    .build();

            // Populate authors for the selected course
            if (questionBankDto.getCourse() != null) {
                List<AuthorInfo> authors = authorService.getAuthorsByCourse(questionBankDto.getCourse());
                dto.setAuthors(authors != null ? authors : new ArrayList<>());
            }

            // Populate course list for dropdown
            List<CourseInfo> allCourses = courseService.getAllCourses().stream()
                    .map(CourseInfo::from)
                    .toList();
            dto.setAllCourses(allCourses);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.atError().setCause(e).addArgument(authorId).addArgument(questionBankId).log("Error getting questions by author {} and questionBank {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Returns a sample question populated with demo data for the given type.
     * Used by the question editors to fill in example values.
     *
     * @param type question type: MULTICHOICE or TRUEFALSE
     * @return QuestionDto pre-filled with sample data
     */
    @GetMapping("/sample")
    @Operation(summary = "Get sample question", description = "Returns a pre-filled sample question for a given type")
    public ResponseEntity<QuestionDto> getSampleQuestion(@org.springframework.web.bind.annotation.RequestParam(value = "type", defaultValue = "MULTICHOICE") String type) {
        try {
            QuestionType qType = QuestionType.valueOf(type.toUpperCase());
            QuestionDto sample = new QuestionDto();
            sample.setType(qType);
            sample.setAuthorName(ControllerSettings.DEFAULT_AUTHOR);
            sample.setCourse(ControllerSettings.DEFAULT_COURSE);
            sample.setQuestionBankName(ControllerSettings.DEFAULT_QUESTION_BANK);
            if (qType == QuestionType.MULTICHOICE) {
                sample.setTitle("SQL Query Basics");
                sample.setText("Which SQL statement is used to retrieve data from a database table?");
                sample.setChapter("Databases");
                sample.setResponse1("SELECT");
                sample.setResponse2("INSERT");
                sample.setResponse3("UPDATE");
                sample.setResponse4("DELETE");
                sample.setWeightResponse1(100.0);
                sample.setWeightResponse2(-100.0);
                sample.setWeightResponse3(-100.0);
                sample.setWeightResponse4(-100.0);
            } else {
                sample.setTitle("SQL Statement Usage");
                sample.setText("The SQL statement SELECT is used to retrieve data from a table.");
                sample.setChapter("Databases");
                sample.setResponse1("True");
                sample.setWeightTrue(100.0);
                sample.setWeightFalse(-100.0);
            }
            return ResponseEntity.ok(sample);
        } catch (IllegalArgumentException e) {
            log.atWarn().addArgument(type).log("Invalid question type for sample: {}");
            return ResponseEntity.badRequest().build();
        }
    }
}

