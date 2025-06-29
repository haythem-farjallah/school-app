package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.mapper.ParentMapper;
import com.example.school_management.feature.auth.service.ParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/parent-management")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ParentController {

    private final ParentService service;
    private final ParentMapper mapper;

    /* -------- CREATE ---------- */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ParentDto>> create(
            @Valid @RequestBody ParentCreateDto body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(service.create(body))));
    }

    /* ---------- READ (single) ---------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ParentDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.find(id))));
    }

    /* ---------- READ (paged list) ---------- */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<ParentDto>>> list(Pageable pageable) {
        var dto = new PageDto<>(service.findAll(pageable).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- UPDATE / PATCH ---------- */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ParentDto>> patch(@PathVariable long id,
                          @Valid @RequestBody ParentUpdateDto body) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.patch(id, body))));
    }

    /* ---------- SOFT-DELETE ---------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }
} 