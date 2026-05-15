package com.unitbv.myquiz.api.interfaces;

import com.unitbv.myquiz.api.dto.QuestionErrorDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterRequestDto;
import com.unitbv.myquiz.api.dto.QuestionErrorFilterResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface QuestionErrorApi {

    @PostMapping("/filter")
    ResponseEntity<QuestionErrorFilterResponseDto> filterErrors(@RequestBody QuestionErrorFilterRequestDto filterInput);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteError(@PathVariable Long id);

    @PutMapping("/{id}/resolve")
    ResponseEntity<QuestionErrorDto> resolveError(@PathVariable Long id);

    @GetMapping("/{id}")
    ResponseEntity<QuestionErrorDto> getErrorById(@PathVariable Long id);

    @GetMapping
    ResponseEntity<List<QuestionErrorDto>> getAllErrors();
}

