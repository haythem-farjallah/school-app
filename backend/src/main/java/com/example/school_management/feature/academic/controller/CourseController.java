package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;

    /* CRUD -------------------------------------------------- */

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<CourseDto>> create(
            @RequestBody @Valid CreateCourseRequest req) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(req)));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<CourseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCourseRequest req) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, req)));
    }

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<CourseDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<PageDto<CourseDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String nameLike) {

        var dto = new PageDto<>(service.list(PageRequest.of(page, size),
                teacherId, nameLike));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<PageDto<CourseDto>>> filter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        var dto = new PageDto<>(service.findWithAdvancedFilters(PageRequest.of(page, size), request.getParameterMap()));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
}
