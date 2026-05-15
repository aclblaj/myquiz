package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.AuthorDetailsDto;
import com.unitbv.myquiz.api.dto.AuthorDto;
import com.unitbv.myquiz.api.dto.AuthorFilterRequestDto;
import com.unitbv.myquiz.api.dto.AuthorFilterResponseDto;
import com.unitbv.myquiz.api.dto.AuthorFormDataDto;
import com.unitbv.myquiz.api.dto.AuthorUpsertDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * API interface for Author operations.
 * This interface defines the contract for author management endpoints.
 */
@Tag(name = "Authors", description = "Author management operations")
public interface AuthorApi {

    @Operation(summary = "Get all authors", description = "Retrieve all authors in the system")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved authors"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping
    ResponseEntity<List<AuthorDto>> getAllAuthors();

    @Operation(summary = "Get author by ID", description = "Retrieve a specific author by their ID")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved author"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/{id}")
    ResponseEntity<AuthorDto> getAuthorById(@Parameter(description = "Author ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create new author", description = "Create a new author")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "201", description = "Author created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("")
    ResponseEntity<AuthorDto> createAuthor(@Parameter(description = "Author data", required = true) @RequestBody AuthorUpsertDto authorUpsertDto);

    @Operation(summary = "Update author", description = "Update an existing author by ID")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Author updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PutMapping("/{id}")
    ResponseEntity<AuthorDto> updateAuthor(
            @Parameter(description = "Author ID", required = true) @PathVariable Long id,
            @Parameter(description = "Author data", required = true) @RequestBody AuthorUpsertDto authorUpsertDto
    );

    @Operation(summary = "Delete author", description = "Delete an author by ID")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "204", description = "Author deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteAuthor(@Parameter(description = "Author ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get questions by author", description = "Retrieve questions created by a specific author")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved questions"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/name/{authorName}/questions")
    ResponseEntity<AuthorFormDataDto> getQuestionsForAuthorName(@PathVariable String authorName);

    @Operation(summary = "Get questions by author ID", description = "Retrieve questions created by a specific author using their ID")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved questions"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/id/{authorId}/questions")
    ResponseEntity<AuthorFormDataDto> getQuestionsForAuthorId(@PathVariable Long authorId);

    @Operation(summary = "Get author by name", description = "Retrieve a specific author by their name")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved author"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/name/{name}")
    ResponseEntity<AuthorDto> getAuthorByName(@PathVariable String name);

    @Operation(summary = "List authors filtered", description = "Filter authors by course, authorId, page, pageSize", tags = {"Authors"}, operationId = "listAuthors", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully filtered authors"),
                    @ApiResponse(responseCode = "400", description = "Invalid filter input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/filter")
    ResponseEntity<AuthorFilterResponseDto> listAuthors(@RequestBody AuthorFilterRequestDto filterInput);

    @Operation(summary = "Get author details", description = "Retrieve detailed information about a specific author by their ID")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved author details"),
                    @ApiResponse(responseCode = "404", description = "Author not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/{id}/details")
    ResponseEntity<AuthorDetailsDto> getAuthorDetails(@PathVariable("id") Long id);
}
