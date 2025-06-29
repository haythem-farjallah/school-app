package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.TeacherCreateDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.auth.dto.TeacherUpdateDto;
import com.example.school_management.feature.auth.mapper.TeacherMapper;
import com.example.school_management.feature.auth.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TeacherController {

    private final TeacherService service;
    private final TeacherMapper mapper;

    /* -------- CREATE ---------- */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<TeacherDto>> create(
            @Valid @RequestBody TeacherCreateDto body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(service.create(body))));
    }

    /* ---------- READ (single) ---------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TeacherDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.find(id))));
    }

    /* ---------- READ (paged list) ---------- */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<TeacherDto>>> list(Pageable pageable) {
        var dto = new PageDto<>(service.findAll(pageable).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- UPDATE / PATCH ---------- */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TeacherDto>> patch(@PathVariable long id,
                            @Valid @RequestBody TeacherUpdateDto body) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.patch(id, body))));
    }

    /* ---------- SOFT-DELETE ---------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }
}
