package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.AuthorErrorFilterDto;
import com.unitbv.myquiz.api.dto.AuthorErrorFilterInputDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API interface for Author Error operations.
 * This interface defines the contract for managing author errors in quizzes.
 */
@RequestMapping(value="/api/errors")
@Tag(name = "Author Errors", description = "Author error tracking and reporting operations")
public interface AuthorErrorApi {

    @Operation(summary = "Get author errors", description = "Retrieve author errors filtered by course and/or author with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved author errors"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value={"","/"})
    ResponseEntity<AuthorErrorFilterDto> getAuthorErrors(
            @Parameter(description = "Selected course")
            @RequestParam(value = "selectedCourse", required = false) String selectedCourse,
            @Parameter(description = "Selected author")
            @RequestParam(value = "selectedAuthor", required = false) String selectedAuthor,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);

    @Operation(summary = "Filter author errors", description = "Filter author errors by course/author with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully filtered author errors"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/filter")
    ResponseEntity<AuthorErrorFilterDto> filterAuthorErrors(@RequestBody AuthorErrorFilterInputDto filterInput);
}
