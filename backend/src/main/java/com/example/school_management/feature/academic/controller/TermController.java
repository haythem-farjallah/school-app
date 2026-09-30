package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.academic.dto.TermDto;
import com.example.school_management.feature.academic.dto.TermRequest;
import com.example.school_management.feature.academic.service.TermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years/{academicYearId}/terms")
@RequiredArgsConstructor
public class TermController {
    private final TermService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<TermDto>> create(@PathVariable Long academicYearId,
                                                            @RequestBody @Valid TermRequest request) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(academicYearId, request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<List<TermDto>>> list(@PathVariable Long academicYearId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.list(academicYearId)));
    }

    @GetMapping("/{termId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<TermDto>> get(@PathVariable Long academicYearId, @PathVariable Long termId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(academicYearId, termId)));
    }

    @PutMapping("/{termId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<TermDto>> update(@PathVariable Long academicYearId, @PathVariable Long termId,
                                                            @RequestBody @Valid TermRequest request) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(academicYearId, termId, request)));
    }
}
