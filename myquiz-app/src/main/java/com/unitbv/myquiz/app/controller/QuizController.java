package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuizDto;
import com.unitbv.myquiz.api.dto.QuizFilterInputDto;
import com.unitbv.myquiz.api.dto.QuizFilterDto;
import com.unitbv.myquiz.api.dto.QuizStatisticsDto;
import com.unitbv.myquiz.api.interfaces.QuizApi;
import com.unitbv.myquiz.app.entities.Question;
import com.unitbv.myquiz.api.types.QuestionType;
import com.unitbv.myquiz.app.services.QuizErrorService;
import com.unitbv.myquiz.app.services.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes", description = "Quiz management operations - Organize questions into cohesive quizzes")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class QuizController implements QuizApi {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
    private static final String REGEX_SAFE = "[^a-zA-Z0-9]";
    private final QuizService quizService;
    private final QuizErrorService quizErrorService;

    // Remove @Autowired for constructor injection (Spring 4.3+ does this automatically)
    public QuizController(
            QuizService quizService,
            QuizErrorService quizErrorService) {
        this.quizService = quizService;
        this.quizErrorService = quizErrorService;
    }

    @GetMapping("")
    @Operation(
        summary = "Get All Quizzes",
        description = "Retrieve a list of all available quizzes with their basic information and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<List<QuizDto>> getAllQuizzes() {
        logger.atInfo().log("QuizController.getAllQuizzes called");
        try {
            List<QuizDto> quizzes = quizService.getAllQuizzes();
            logger.info("Retrieved {} quizzes", quizzes.size());
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            logger.error("Error retrieving all quizzes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get quiz by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Quiz by ID",
        description = "Retrieve detailed information about a specific quiz including questions and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quiz found",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "404", description = "Quiz not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<QuizDto> getQuizById(
            @Parameter(description = "Unique identifier of the quiz", required = true, example = "1")
            @PathVariable Long id) {
        try {
            QuizDto quiz = quizService.getQuizById(id);
            if (quiz != null) {
                logger.info("Retrieved quiz: {}", quiz.getName());
                return ResponseEntity.ok(quiz);
            } else {
                logger.warn("Quiz not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving quiz with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new quiz
     */
    @PostMapping
    @Operation(
        summary = "Create New Quiz",
        description = "Create a new quiz with the specified course, name, and year"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Quiz created successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quiz data",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<QuizDto> createQuiz(@RequestBody QuizDto quizDto) {
        try {
            var quiz = quizService.createQuizz(quizDto.getCourse(), quizDto.getName(), quizDto.getYear());
            QuizDto createdDto = new QuizDto();
            createdDto.setId(quiz.getId());
            createdDto.setName(quiz.getName());
            createdDto.setCourse(quiz.getCourse());
            createdDto.setYear(quiz.getYear());
            return ResponseEntity.status(201).body(createdDto);
        } catch (Exception e) {
            logger.error("Error creating quiz", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update quiz by ID
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update Quiz",
        description = "Update an existing quiz's details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quiz updated successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quiz data",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Quiz not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<QuizDto> updateQuiz(@PathVariable("id") Long id, @RequestBody QuizDto quizDto) {
        try {
            var updatedQuiz = quizService.updateQuiz(id, quizDto.getCourse(), quizDto.getName(), quizDto.getYear());
            if (updatedQuiz == null) return ResponseEntity.notFound().build();
            QuizDto updatedDto = new QuizDto();
            updatedDto.setId(updatedQuiz.getId());
            updatedDto.setName(updatedQuiz.getName());
            updatedDto.setCourse(updatedQuiz.getCourse());
            updatedDto.setYear(updatedQuiz.getYear());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            logger.error("Error updating quiz", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete quiz by ID
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Quiz",
        description = "Permanently delete a quiz and all its associated data including questions, errors, and QuizAuthor entries. Orphaned authors (with no other quiz contributions) will be automatically removed. This operation cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Quiz deleted successfully (No Content)"),
        @ApiResponse(responseCode = "404", description = "Quiz not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    @Override
    public ResponseEntity<Void> deleteQuiz(@PathVariable("id") Long id) {
        logger.atInfo().addArgument(id).log("QuizController.deleteQuiz called for quiz ID: {}");
        try {
            quizService.deleteQuizById(id);
            logger.atInfo().addArgument(id).log("Successfully deleted quiz with ID: {}");
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // Quiz not found
            logger.atWarn().addArgument(id).log("Quiz not found with ID: {} - {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Other errors (database errors, transaction errors, etc.)
            logger.error("Error deleting quiz with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get quizzes by Course ID
     */
    @GetMapping("/course/{courseId}")
    @Operation(
        summary = "Get Quizzes by Course ID",
        description = "Retrieve a list of quizzes associated with a specific course"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = QuizDto.class))),
        @ApiResponse(responseCode = "404", description = "Course not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })

    @Override
    public ResponseEntity<List<QuizDto>> getQuizzesByCourseId(@PathVariable("courseId") Long courseId) {
        try {
            List<QuizDto> quizzes = quizService.getQuizzesByCourseId(courseId);
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            logger.error("Error retrieving quizzes for course ID: {}", courseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<QuizDto>> getQuizzesByCourseName(String courseName) {
        try {
            List<QuizDto> quizzes = quizService.getQuizzesByCourse(courseName);
            return ResponseEntity.ok(quizzes);
        } catch (Exception e) {
            logger.error("Error retrieving quizzes for course name: {}", courseName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildCsvLineMC(int number, QuestionDto dto, String chapter, String title, String questionText) {
        String[] weights = new String[4];
        String[] responses = new String[4];
        weights[0] = dto.getWeightResponse1() != null ? dto.getWeightResponse1().toString() : "";
        weights[1] = dto.getWeightResponse2() != null ? dto.getWeightResponse2().toString() : "";
        weights[2] = dto.getWeightResponse3() != null ? dto.getWeightResponse3().toString() : "";
        weights[3] = dto.getWeightResponse4() != null ? dto.getWeightResponse4().toString() : "";
        responses[0] = dto.getResponse1() != null ? dto.getResponse1() : "";
        responses[1] = dto.getResponse2() != null ? dto.getResponse2() : "";
        responses[2] = dto.getResponse3() != null ? dto.getResponse3() : "";
        responses[3] = dto.getResponse4() != null ? dto.getResponse4() : "";
        StringBuilder line = new StringBuilder();
        line.append(number).append(",");
        line.append(chapter).append(",");
        line.append(title).append(",");
        line.append("\"").append(questionText.replace("\"", "\"\"")).append("\"").append(",");
        for (int i = 0; i < 4; i++) {
            line.append(weights[i]).append(",");
            line.append("\"").append(responses[i].replace("\"", "\"\"")).append("\"");
            if (i < 3) line.append(",");
        }
        line.append("\n");
        return line.toString();
    }

    private String buildCsvLineTF(int number, QuestionDto dto, String chapter, String title, String questionText) {
        String weightTrue = dto.getWeightTrue() != null ? dto.getWeightTrue().toString() : "";
        String weightFalse = dto.getWeightFalse() != null ? dto.getWeightFalse().toString() : "";
        return number + "," +
                chapter + "," +
                title + "," +
                "\"" + questionText.replace("\"", "\"\"") + "\"" + "," +
                weightTrue + "," +
                weightFalse +
                "\n";
    }

    @GetMapping(value = "/{id}/export-mc")
    public void exportQuizToCsv(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
            return;
        }
        List<?> questionsRaw = quizService.getQuestionsByQuizId(id);
        response.setContentType("text/csv; charset=UTF-8");
        String safeCourse = quiz.getCourse() != null ? quiz.getCourse().replaceAll(REGEX_SAFE, "_") : "course";
        String safeQuizName = quiz.getName() != null ? quiz.getName().replaceAll(REGEX_SAFE, "_") : "quiz";
        String safeYear = quiz.getYear() != null ? quiz.getYear().toString() : "year";
        String filename = String.format("%s_%s_%s_MC.csv", safeCourse, safeQuizName, safeYear);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        String header = "Nr.,Curs,Titlu intrebare,Text intrebare,PR1,Raspuns 1,PR2,Raspuns 2,PR3,Raspuns 3,PR4,Raspuns 4, Feedback\n";
        ServletOutputStream out = response.getOutputStream();
        out.write("\uFEFF".getBytes(StandardCharsets.UTF_8)); // Write BOM for UTF-8
        out.write(header.getBytes(StandardCharsets.UTF_8));
        int number = 1;
        for (Object q : questionsRaw) {
            Question question = (Question) q;
            if (question.getType() == null || !question.getType().equals(QuestionType.MULTICHOICE)) continue; // Only MC questions
            QuestionDto dto = toDto(question);
            String chapter = dto.getChapter() != null ? dto.getChapter() : "";
            String title = quiz.getName();
            String questionText = dto.getText() != null ? dto.getText() : "";
            String line = buildCsvLineMC(number++, dto, chapter, title, questionText);
            out.write(line.getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
        out.close();
    }

    @GetMapping(value = "/{id}/export-tf")
    public void exportQuizToCsvTF(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        QuizDto quiz = quizService.getQuizById(id);
        if (quiz == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
            return;
        }
        List<?> questionsRaw = quizService.getQuestionsByQuizId(id);
        response.setContentType("text/csv; charset=UTF-8");
        String safeCourse = quiz.getCourse() != null ? quiz.getCourse().replaceAll(REGEX_SAFE, "_") : "course";
        String safeQuizName = quiz.getName() != null ? quiz.getName().replaceAll(REGEX_SAFE, "_") : "quiz";
        String safeYear = quiz.getYear() != null ? quiz.getYear().toString() : "year";
        String filename = String.format("%s_%s_%s_TF.csv", safeCourse, safeQuizName, safeYear);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        String header = "Nr.,Curs,Titlu,Text intrebare (afirmatie),TRUE,FALSE,Feedback\n";
        ServletOutputStream out = response.getOutputStream();
        out.write("\uFEFF".getBytes(StandardCharsets.UTF_8)); // Write BOM for UTF-8
        out.write(header.getBytes(StandardCharsets.UTF_8));
        int number = 1;
        for (Object q : questionsRaw) {
            Question question = (Question) q;
            if (question.getType() == null || !question.getType().equals(QuestionType.TRUEFALSE)) continue; // Only TF questions
            QuestionDto dto = toDto(question);
            String chapter = dto.getChapter() != null ? dto.getChapter() : "";
            String title = dto.getTitle();
            String questionText = dto.getText() != null ? dto.getText() : "";
            String line = buildCsvLineTF(number++, dto, chapter, title, questionText);
            out.write(line.getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
        out.close();
    }

    /**
     * Get quiz statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<QuizStatisticsDto> getQuizStatistics(@PathVariable Long id) {
        try {
            QuizDto quiz = quizService.getQuizById(id);
            if (quiz == null) return ResponseEntity.notFound().build();
            List<Question> questions = quizService.getQuestionsByQuizId(id);
            Map<Long, QuizStatisticsDto.AuthorStatsDto> statsMap = new java.util.HashMap<>();
            for (Question q : questions) {
                if (q.getQuizAuthor() == null) continue;
                Long authorId = q.getQuizAuthor().getId();
                String authorName = q.getQuizAuthor().getName();
                QuizStatisticsDto.AuthorStatsDto stat = statsMap.getOrDefault(authorId, new QuizStatisticsDto.AuthorStatsDto());
                stat.setAuthorId(authorId);
                stat.setAuthorName(authorName);
                if (q.getType() == QuestionType.MULTICHOICE) stat.setMcCount(stat.getMcCount() + 1);
                if (q.getType() == QuestionType.TRUEFALSE) stat.setTfCount(stat.getTfCount() + 1);
                statsMap.put(authorId, stat);
            }
            // Count errors per author
            for (QuizStatisticsDto.AuthorStatsDto stat : statsMap.values()) {
                int errorCount = quizErrorService.countErrorsByAuthorAndQuiz(stat.getAuthorId(), quiz.getId());
                stat.setErrorCount(errorCount);
            }
            QuizStatisticsDto dto = new QuizStatisticsDto();
            dto.setQuiz(quiz);
            dto.setAuthorStats(new java.util.ArrayList<>(statsMap.values()));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching statistics for quiz {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Filter quizzes
     */
    @PostMapping("/filter")
    public ResponseEntity<QuizFilterDto> filterQuizzes(@RequestBody QuizFilterInputDto filterInput) {
        logger.atInfo().log("QuizController.filterQuizzes called with input: {}", filterInput);
        try {
            QuizFilterDto result = quizService.filterQuizzes(filterInput);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error filtering quizzes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private QuestionDto toDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setTitle(question.getTitle());
        dto.setText(question.getText());
        dto.setResponse1(question.getResponse1());
        dto.setResponse2(question.getResponse2());
        dto.setResponse3(question.getResponse3());
        dto.setResponse4(question.getResponse4());
        dto.setWeightResponse1(question.getWeightResponse1());
        dto.setWeightResponse2(question.getWeightResponse2());
        dto.setWeightResponse3(question.getWeightResponse3());
        dto.setWeightResponse4(question.getWeightResponse4());
        dto.setWeightTrue(question.getWeightTrue());
        dto.setWeightFalse(question.getWeightFalse());
        dto.setChapter(question.getChapter());
        return dto;
    }
}
