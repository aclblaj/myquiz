package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.interfaces.QuestionDuplicateApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.services.QuestionDuplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone REST controller for individual duplicate-link operations, addressed by the
 * {@code QuestionDuplicate} link's own ID (not a question ID) - mirrors
 * {@link QuestionErrorController}'s structure.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/duplicates")
@CrossOrigin(origins = "${FRONTEND_URL}")
@Tag(name = "Duplicates", description = "Individual duplicate-link management")
public class QuestionDuplicateController implements QuestionDuplicateApi {

    private static final Logger log = LoggerFactory.getLogger(QuestionDuplicateController.class);

    private final QuestionDuplicationService questionDuplicationService;

    @Override
    @PutMapping(ControllerSettings.API_DUPLICATES_RESOLVE_BY_ID)
    public ResponseEntity<Void> resolveDuplicate(@PathVariable Long id) {
        log.info("Resolving duplicate link with id: {}", id);
        try {
            questionDuplicationService.markDuplicateResolved(id);
            log.info("Duplicate link {} resolved successfully", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Duplicate link not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error resolving duplicate link with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
