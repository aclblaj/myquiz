package com.unitbv.myquiz.app.controller;

import com.unitbv.myquiz.api.dto.AuthorErrorFilterDto;
import com.unitbv.myquiz.api.dto.AuthorErrorFilterInputDto;
import com.unitbv.myquiz.api.interfaces.AuthorErrorApi;
import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.app.services.QuizErrorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/errors")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class AuthorErrorController implements AuthorErrorApi {

    private static final Logger log = LoggerFactory.getLogger(AuthorErrorController.class);

    private final QuizErrorService quizErrorService;

    @GetMapping({"", "/"})
    @Override
    public ResponseEntity<AuthorErrorFilterDto> getAuthorErrors(
            @RequestParam(value = ControllerSettings.ATTR_SELECTED_COURSE, required = false) String selectedCourse,
            @RequestParam(value = ControllerSettings.ATTR_SELECTED_AUTHOR, required = false) String selectedAuthor,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE) Integer page,
            @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false, defaultValue = ControllerSettings.DEFAULT_PAGE_SIZE) Integer pageSize
    ) {
        log.info("Fetching author errors for course: {} and author: {}, page: {}, pageSize: {}", selectedCourse, selectedAuthor, page, pageSize);
        AuthorErrorFilterDto getAuthorErrorsModel = quizErrorService.getAuthorErrors(selectedCourse, selectedAuthor, page, pageSize);
        return ResponseEntity.ok(getAuthorErrorsModel);
    }

    @PostMapping("/filter")
    @Override
    public ResponseEntity<AuthorErrorFilterDto> filterAuthorErrors(@RequestBody AuthorErrorFilterInputDto filterInput) {
        log.info("Filtering author errors with input: {}", filterInput);
        AuthorErrorFilterDto dto = quizErrorService.filter(filterInput);
        return ResponseEntity.ok(dto);
    }
}
