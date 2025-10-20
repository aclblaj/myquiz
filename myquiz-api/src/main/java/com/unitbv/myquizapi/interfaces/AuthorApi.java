package com.unitbv.myquizapi.interfaces;

import com.unitbv.myquizapi.dto.AuthorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API interface for Author operations.
 * This interface defines the contract for author management endpoints.
 */
@Tag(name = "Authors", description = "Author management operations")
public interface AuthorApi {

    @Operation(summary = "Get all authors", description = "Retrieve all authors in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved authors"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/authors")
    ResponseEntity<List<AuthorDto>> getAllAuthors();

    @Operation(summary = "Get author by ID", description = "Retrieve a specific author by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved author"),
            @ApiResponse(responseCode = "404", description = "Author not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/authors/{id}")
    ResponseEntity<AuthorDto> getAuthorById(
            @Parameter(description = "Author ID", required = true)
            @PathVariable Long id);

    @Operation(summary = "Create new author", description = "Create a new author")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Author created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/authors")
    ResponseEntity<AuthorDto> createAuthor(
            @Parameter(description = "Author data", required = true)
            @RequestBody AuthorDto authorDto);

    @Operation(summary = "Update author", description = "Update an existing author")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Author not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/api/authors/{id}")
    ResponseEntity<AuthorDto> updateAuthor(
            @Parameter(description = "Author ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated author data", required = true)
            @RequestBody AuthorDto authorDto);

    @Operation(summary = "Delete author", description = "Delete an author by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Author deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Author not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/api/authors/{id}")
    ResponseEntity<Void> deleteAuthor(
            @Parameter(description = "Author ID", required = true)
            @PathVariable Long id);

    @Operation(summary = "Search authors by department", description = "Find authors by department")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved authors"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/authors/department/{department}")
    ResponseEntity<List<AuthorDto>> getAuthorsByDepartment(
            @Parameter(description = "Department name", required = true)
            @PathVariable String department);
}
