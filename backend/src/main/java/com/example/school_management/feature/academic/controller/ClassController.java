package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Classes", description = "Endpoints for managing classes and their enrolments")
@SecurityRequirement(name = "bearerAuth")
public class ClassController {

    private final ClassService service;

    @Operation(summary = "Create a new class")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Class created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiSuccessResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ClassDto>> create(
            @RequestBody @Valid CreateClassRequest req) {
        log.debug("POST /classes {}", req);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(req)));
    }

    @Operation(summary = "Update an existing class by ID")
    @Parameter(name = "id", description = "ID of the class to update", required = true)
    @PutMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateClassRequest req) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, req)));
    }

    @Operation(summary = "Get class details by ID")
    @Parameter(name = "id", description = "ID of the class to retrieve", required = true)
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Delete a class by ID")
    @Parameter(name = "id", description = "ID of the class to delete", required = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "List classes with pagination and optional filters")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<ClassDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by level ID (optional)") @RequestParam(required = false) Long levelId,
            @Parameter(description = "Filter by name pattern (optional)") @RequestParam(required = false) String nameLike) {
        var dto = new PageDto<>(service.list(PageRequest.of(page, size), levelId, nameLike));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Batch update students enrolment (add/remove)")
    @Parameter(name = "classId", description = "ID of the class to update", required = true)
    @PatchMapping("/{classId}/students")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> batchStudents(
            @PathVariable Long classId,
            @RequestBody @Valid BatchIdsRequest body) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.mutateStudents(classId, body)));
    }

    @Operation(summary = "Batch update courses enrolled in a class")
    @Parameter(name = "classId", description = "ID of the class to update", required = true)
    @PatchMapping("/{classId}/courses")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> batchCourses(
            @PathVariable Long classId,
            @RequestBody @Valid BatchIdsRequest body) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.mutateCourses(classId, body)));
    }

    @Operation(summary = "Add a single student to a class")
    @Parameter(name = "classId", description = "ID of the class", required = true)
    @Parameter(name = "studentId", description = "ID of the student to add", required = true)
    @PostMapping("/{classId}/students/{studentId}")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> addStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.addStudent(classId, studentId)));
    }

    @Operation(summary = "Remove a single student from a class")
    @Parameter(name = "classId", description = "ID of the class", required = true)
    @Parameter(name = "studentId", description = "ID of the student to remove", required = true)
    @DeleteMapping("/{classId}/students/{studentId}")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> removeStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.removeStudent(classId, studentId)));
    }

    @Operation(summary = "Add a single course to a class")
    @Parameter(name = "classId", description = "ID of the class", required = true)
    @Parameter(name = "courseId", description = "ID of the course to add", required = true)
    @PostMapping("/{classId}/courses/{courseId}")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> addCourse(
            @PathVariable Long classId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.addCourse(classId, courseId)));
    }

    @Operation(summary = "Remove a single course from a class")
    @Parameter(name = "classId", description = "ID of the class", required = true)
    @Parameter(name = "courseId", description = "ID of the course to remove", required = true)
    @DeleteMapping("/{classId}/courses/{courseId}")
    public ResponseEntity<ApiSuccessResponse<ClassDto>> removeCourse(
            @PathVariable Long classId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.removeCourse(classId, courseId)));
    }
}
