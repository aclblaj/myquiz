package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.DuplicateUnlinkRequestDto;
import com.unitbv.myquiz.api.dto.QuestionCorrectionDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Tag(name = "Questions", description = "Question management operations")
public interface QuestionApi {

    @Operation(summary = "Get all questions", description = "Retrieve all questions in the system")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved questions"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping
    ResponseEntity<List<QuestionDto>> getAllQuestions();

    @Operation(summary = "Get question by ID", description = "Retrieve a specific question by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved question"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}")
    ResponseEntity<QuestionDto> getQuestionById(@Parameter(description = "Question ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create new question", description = "Create a new question")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Question created successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping
    ResponseEntity<QuestionDto> createQuestion(@Parameter(description = "Question data", required = true) @Valid @RequestBody QuestionDto questionDto);

    @Operation(summary = "Update question", description = "Update an existing question")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question updated successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PutMapping("/{id}")
    ResponseEntity<QuestionDto> updateQuestion(@Parameter(description = "Question ID", required = true) @PathVariable Long id,
                                               @Parameter(description = "Updated question data", required = true) @Valid @RequestBody QuestionDto questionDto);

    @Operation(summary = "Delete question", description = "Delete a question by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Question deleted successfully"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteQuestion(@Parameter(description = "Question ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get questions by questionBank", description = "Retrieve all questions for a specific questionBank")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved questions"), @ApiResponse(responseCode = "404", description = "QuestionBank not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping(ControllerSettings.API_QUESTION_BANKS_GET_BY_ID)
    ResponseEntity<QuestionFilterResponseDto> getQuestionsByQuestionBankId(@Parameter(description = "questionBank ID", required = true) @PathVariable Long questionBankId);


    @Operation(summary = "List questions with filters", description = "Retrieve questions based on filtering criteria")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved filtered questions"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/filter")
    ResponseEntity<QuestionFilterResponseDto> listQuestionsFiltered(@Parameter(description = "Filter criteria", required = true) @Valid @RequestBody QuestionFilterRequestDto filterInput);

    @Operation(summary = "Get duplicates for question", description = "Retrieve a question DTO enriched with all linked duplicate questions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved duplicates"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}/duplicates")
    ResponseEntity<QuestionDto> getQuestionDuplicates(@Parameter(description = "Question ID", required = true) @PathVariable Long id);

    @Operation(summary = "Remove duplicate links", description = "Remove one or more duplicate links from a question")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Duplicate links removed successfully"), @ApiResponse(responseCode = "400", description = "Invalid request"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/duplicates/remove")
    ResponseEntity<Void> removeQuestionDuplicates(@Parameter(description = "Question ID", required = true) @PathVariable Long id,
                                                   @Parameter(description = "Duplicate question IDs to unlink", required = true) @Valid @RequestBody DuplicateUnlinkRequestDto selectionDto);

    @Operation(summary = "Remove all duplicate links", description = "Remove every duplicate link for a question, regardless of pagination")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "All duplicate links removed successfully"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id:\\d+}/duplicates/remove-all")
    ResponseEntity<Void> removeAllQuestionDuplicates(@Parameter(description = "Question ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get sample question", description = "Returns a pre-filled sample question for a given question type")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Sample question generated"), @ApiResponse(responseCode = "400", description = "Invalid question type")})
    @GetMapping("/sample")
    ResponseEntity<QuestionDto> getSampleQuestion(@Parameter(description = "Question type", required = false) @RequestParam(value = "type", defaultValue = "MULTICHOICE") String type);

    @Operation(summary = "Delete duplicate question", description = "Delete a question marked as duplicate including all associations")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Duplicate question deleted successfully"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @DeleteMapping("/{id}/as-duplicate")
    ResponseEntity<Map<String, Object>> deleteDuplicateQuestion(@Parameter(description = "Question ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get duplicates in course", description = "Retrieve all questions with duplicates in a specific course")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Duplicates list retrieved successfully"), @ApiResponse(responseCode = "400", description = "Invalid course name"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/course/{course}/with-duplicates")
    ResponseEntity<Map<String, Object>> getQuestionsWithDuplicatesInCourse(@Parameter(description = "Course name", required = true) @PathVariable String course);

    @Operation(summary = "Correct grammar in question", description = "Use AI to correct grammar and spelling errors")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Grammar correction completed successfully"), @ApiResponse(responseCode = "400", description = "Invalid correction payload"), @ApiResponse(responseCode = "503", description = "Correction request interrupted"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/correction/grammar")
    ResponseEntity<QuestionCorrectionDto> correctGrammar(@Parameter(description = "Question ID", required = true) @PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto);

    @Operation(summary = "Improve question", description = "Use AI to improve question clarity and precision")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question improvement completed successfully"), @ApiResponse(responseCode = "400", description = "Invalid correction payload"), @ApiResponse(responseCode = "503", description = "Improvement request interrupted"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/correction/improve")
    ResponseEntity<QuestionCorrectionDto> improveQuestion(@Parameter(description = "Question ID", required = true) @PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto);

    @Operation(summary = "Generate alternative answers", description = "Use AI to generate plausible but incorrect alternatives")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Alternatives generated successfully"), @ApiResponse(responseCode = "400", description = "Invalid correction payload"), @ApiResponse(responseCode = "503", description = "Alternatives request interrupted"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/correction/alternatives")
    ResponseEntity<Map<String, String>> generateAlternatives(@Parameter(description = "Question ID", required = true) @PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto);

    @Operation(summary = "Explain correct answer", description = "Use AI to explain why an answer is correct and others are wrong")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Answer explanation generated successfully"), @ApiResponse(responseCode = "400", description = "Invalid correction payload"), @ApiResponse(responseCode = "503", description = "Explanation request interrupted"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/correction/explanation")
    ResponseEntity<Map<String, String>> explainAnswer(@Parameter(description = "Question ID", required = true) @PathVariable("id") Long id, @Valid @RequestBody QuestionCorrectionDto correctionDto);

    @Operation(summary = "Get questions by author and question bank", description = "Retrieve all questions for a specific question bank created by a specific author")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved questions"), @ApiResponse(responseCode = "404", description = "Question bank author combination not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/author/{authorId}/question-bank/{questionBankId}")
    ResponseEntity<QuestionFilterResponseDto> getQuestionsByAuthorAndQuestionBank(@Parameter(description = "Author ID", required = true) @PathVariable Long authorId,
                                                                                   @Parameter(description = "Question Bank ID", required = true) @PathVariable Long questionBankId);

}
