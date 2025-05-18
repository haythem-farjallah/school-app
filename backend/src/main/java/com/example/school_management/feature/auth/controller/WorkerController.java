package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import com.example.school_management.feature.auth.service.WorkerService;
import com.example.school_management.feature.auth.mapper.WorkerMapper;
import com.example.school_management.feature.auth.dto.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/workers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WorkerController {

    private final WorkerService service;
    private final WorkerMapper mapper;

    @PostMapping
    public ResponseEntity<WorkerDto> create(@Valid @RequestBody WorkerCreateDto body) {
        WorkerDto dto = mapper.toDto(service.create(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public WorkerDto get(@PathVariable long id) {
        return mapper.toDto(service.find(id));
    }

    @GetMapping
    public PageDto<WorkerDto> list(Pageable pageable) {
        return new PageDto<>(service.findAll(pageable).map(mapper::toDto));
    }

    @PatchMapping("/{id}")
    public WorkerDto patch(@PathVariable long id,
                           @Valid @RequestBody WorkerUpdateDto body) {
        return mapper.toDto(service.patch(id, body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
