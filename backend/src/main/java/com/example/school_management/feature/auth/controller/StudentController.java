package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.mapper.StudentMapper;
import com.example.school_management.feature.auth.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/student-management")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StudentController {

    private final StudentService service;
    private final StudentMapper mapper;

    /* -------- CREATE ---------- */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<StudentDto>> create(
            @Valid @RequestBody StudentCreateDto body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(service.create(body))));
    }

    /* ---------- READ (single) ---------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<StudentDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.find(id))));
    }

    /* ---------- READ (paged list) ---------- */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<StudentDto>>> list(Pageable pageable) {
        var dto = new PageDto<>(service.findAll(pageable).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- UPDATE / PATCH ---------- */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<StudentDto>> patch(@PathVariable long id,
                           @Valid @RequestBody StudentUpdateDto body) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.patch(id, body))));
    }

    /* ---------- SOFT-DELETE ---------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }
} 