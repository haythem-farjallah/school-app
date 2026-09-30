package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.academic.dto.AcademicYearDto;
import com.example.school_management.feature.academic.dto.CreateAcademicYearRequest;
import com.example.school_management.feature.academic.dto.UpdateAcademicYearRequest;
import com.example.school_management.feature.academic.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {
    private final AcademicYearService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<AcademicYearDto>> create(@RequestBody @Valid CreateAcademicYearRequest request) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<List<AcademicYearDto>>> list() {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSuccessResponse<AcademicYearDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<AcademicYearDto>> update(@PathVariable Long id,
                                                                   @RequestBody @Valid UpdateAcademicYearRequest request) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<AcademicYearDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.activate(id)));
    }
}
