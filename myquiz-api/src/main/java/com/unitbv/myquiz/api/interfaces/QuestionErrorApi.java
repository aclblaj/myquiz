package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterResponseDto;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@Tag(name = "Errors", description = "Question error management")
public interface QuestionErrorApi {

    @Operation(summary = "Filter question errors", description = "Filter and paginate question errors by course, author, and questionBank")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question errors filtered successfully"), @ApiResponse(responseCode = "400", description = "Invalid filter input"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PostMapping("/filter")
    ResponseEntity<QuestionErrorFilterResponseDto> filterErrors(@Valid @RequestBody QuestionErrorFilterRequestDto filterInput);

    @Operation(summary = "Delete a question error", description = "Remove a specific question error by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Question error deleted successfully"), @ApiResponse(responseCode = "404", description = "Question error not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteError(@PathVariable Long id);

    @Operation(summary = "Resolve a question error", description = "Mark a question error as resolved")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question error resolved successfully"), @ApiResponse(responseCode = "404", description = "Question error not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PutMapping("/{id}/resolve")
    ResponseEntity<QuestionErrorDto> resolveError(@PathVariable Long id);

    @Operation(summary = "Get a question error by ID", description = "Retrieve details of a specific question error")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question error retrieved successfully"), @ApiResponse(responseCode = "404", description = "Question error not found"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/{id}")
    ResponseEntity<QuestionErrorDto> getErrorById(@PathVariable Long id);

    @Operation(summary = "Get all question errors", description = "Retrieve all question errors without filtering")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Question errors retrieved successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping
    ResponseEntity<List<QuestionErrorDto>> getAllErrors();
}

