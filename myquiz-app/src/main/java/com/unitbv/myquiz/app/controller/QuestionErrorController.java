package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterResponseDto;
import com.unitbv.myquiz.api.interfaces.QuestionErrorApi;
import com.unitbv.myquiz.app.services.QuestionErrorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for question error management.
 * Handles filtering, resolving, and deleting question validation errors.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/errors")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class QuestionErrorController implements QuestionErrorApi {

    private static final Logger log = LoggerFactory.getLogger(QuestionErrorController.class);

    private final QuestionErrorService questionErrorService;

    @Override
    public ResponseEntity<QuestionErrorFilterResponseDto> filterErrors(@Valid @RequestBody QuestionErrorFilterRequestDto filterInput) {
        log.info("Filtering errors with input: {}", filterInput);
        try {
            QuestionErrorFilterResponseDto result = questionErrorService.filter(filterInput);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter input: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error filtering errors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteError(@PathVariable Long id) {
        log.info("Deleting error with id: {}", id);
        try {
            boolean deleted = questionErrorService.deleteErrorById(id);
            if (!deleted) {
                log.atDebug().addArgument(id).log("Error with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error deleting error with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<QuestionErrorDto> resolveError(@PathVariable Long id) {
        log.info("Resolving error with id: {}", id);
        try {
            QuestionErrorDto result = questionErrorService.resolveErrorById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error resolving error with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<QuestionErrorDto> getErrorById(@PathVariable Long id) {
        log.info("Getting error with id: {}", id);
        try {
            QuestionErrorDto result = questionErrorService.getErrorById(id);
            if (result == null) {
                log.atDebug().addArgument(id).log("Error with id {} not found", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting error with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<List<QuestionErrorDto>> getAllErrors() {
        log.info("Getting all errors");
        try {
            List<QuestionErrorDto> result = questionErrorService.getAllErrors();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting all errors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

