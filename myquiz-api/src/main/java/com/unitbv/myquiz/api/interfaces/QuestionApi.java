package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.DuplicateUnlinkRequestDto;
import com.unitbv.myquiz.api.dto.QuestionDto;
import com.unitbv.myquiz.api.dto.QuestionFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionFilterResponseDto;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
    ResponseEntity<QuestionDto> createQuestion(@Parameter(description = "Question data", required = true) @RequestBody QuestionDto questionDto);

    @Operation(summary = "Update question", description = "Update an existing question")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question updated successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PutMapping("/{id}")
    ResponseEntity<QuestionDto> updateQuestion(@Parameter(description = "Question ID", required = true) @PathVariable Long id,
                                               @Parameter(description = "Updated question data", required = true) @RequestBody QuestionDto questionDto);

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
    ResponseEntity<QuestionFilterResponseDto> listQuestionsFiltered(@Parameter(description = "Filter criteria", required = true) @RequestBody QuestionFilterRequestDto filterInput);

    @Operation(summary = "Get duplicates for question", description = "Retrieve a question DTO enriched with all linked duplicate questions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully retrieved duplicates"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}/duplicates")
    ResponseEntity<QuestionDto> getQuestionDuplicates(@Parameter(description = "Question ID", required = true) @PathVariable Long id);

    @Operation(summary = "Remove duplicate links", description = "Remove one or more duplicate links from a question")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Duplicate links removed successfully"), @ApiResponse(responseCode = "400", description = "Invalid request"), @ApiResponse(responseCode = "404", description = "Question not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/{id}/duplicates/remove")
    ResponseEntity<Void> removeQuestionDuplicates(@Parameter(description = "Question ID", required = true) @PathVariable Long id,
                                                   @Parameter(description = "Duplicate question IDs to unlink", required = true) @RequestBody DuplicateUnlinkRequestDto selectionDto);

    @Operation(summary = "Get sample question", description = "Returns a pre-filled sample question for a given question type")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Sample question generated"), @ApiResponse(responseCode = "400", description = "Invalid question type")})
    @GetMapping("/sample")
    ResponseEntity<QuestionDto> getSampleQuestion(@Parameter(description = "Question type", required = false) @RequestParam(value = "type", defaultValue = "MULTICHOICE") String type);

}
