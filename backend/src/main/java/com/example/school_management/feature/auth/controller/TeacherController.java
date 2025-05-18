package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
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
import org.springframework.data.domain.Page;


@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TeacherController {

    private final TeacherService service;
    private final TeacherMapper mapper;

    /* -------- CREATE ---------- */
    @PostMapping
    public ResponseEntity<TeacherDto> create(
            @Valid @RequestBody TeacherCreateDto body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDto(service.create(body)));
    }

    /* ---------- READ (single) ---------- */
    @GetMapping("/{id}")
    public TeacherDto get(@PathVariable long id) {
        return mapper.toDto(service.find(id));
    }

    /* ---------- READ (paged list) ---------- */
    @GetMapping
    public PageDto<TeacherDto> list(Pageable pageable) {
        return new PageDto<>(service.findAll(pageable).map(mapper::toDto));

    }

    /* ---------- UPDATE / PATCH ---------- */
    @PatchMapping("/{id}")
    public TeacherDto patch(@PathVariable long id,
                            @Valid @RequestBody TeacherUpdateDto body) {
        return mapper.toDto(service.patch(id, body));
    }

    /* ---------- SOFT-DELETE ---------- */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
