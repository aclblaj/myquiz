package com.unitbv.myquiz.api.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

public interface QuestionDuplicateApi {

    @Operation(summary = "Resolve a duplicate link", description = "Mark a duplicate link as resolved without deleting it")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "204", description = "Duplicate link marked as resolved successfully"),
                    @ApiResponse(responseCode = "404", description = "Duplicate link not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PutMapping("/{id}/resolve")
    ResponseEntity<Void> resolveDuplicate(@PathVariable Long id);
}
