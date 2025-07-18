package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.academic.dto.CreateLearningResourceRequest;
import com.example.school_management.feature.academic.dto.LearningResourceDto;
import com.example.school_management.feature.academic.dto.UpdateLearningResourceRequest;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.service.LearningResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/learning-resources")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
@Tag(name = "Learning Resources", description = "Endpoints for managing learning resources")
@SecurityRequirement(name = "bearerAuth")
public class LearningResourceController {

    private final LearningResourceService service;

    @Operation(summary = "Create a new learning resource")
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> create(
            @Valid @RequestBody CreateLearningResourceRequest request) {
        log.debug("POST /learning-resources {}", request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(request)));
    }

    @Operation(summary = "Upload a learning resource file")
    @PostMapping("/upload")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> uploadResource(
            @RequestParam("file") MultipartFile file,
            @Valid @RequestBody CreateLearningResourceRequest request) {
        log.debug("POST /learning-resources/upload with file: {}", file.getOriginalFilename());
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.uploadResource(file, request)));
    }

    @Operation(summary = "Update an existing learning resource")
    @Parameter(name = "id", description = "ID of the resource to update", required = true)
    @PutMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningResourceRequest request) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, request)));
    }

    @Operation(summary = "Get learning resource details by ID")
    @Parameter(name = "id", description = "ID of the resource to retrieve", required = true)
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<LearningResourceDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Delete a learning resource")
    @Parameter(name = "id", description = "ID of the resource to delete", required = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "List learning resources with pagination and filters")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<LearningResourceDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by resource type") @RequestParam(required = false) ResourceType type,
            @Parameter(description = "Filter by teacher ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "Filter by class ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "Filter by course ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "Search by title or description") @RequestParam(required = false) String search) {

        if (type != null) {
            var dto = new PageDto<>(service.findByType(type, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (teacherId != null) {
            var dto = new PageDto<>(service.findByTeacherId(teacherId, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (classId != null) {
            var dto = new PageDto<>(service.findByClassId(classId, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (courseId != null) {
            var dto = new PageDto<>(service.findByCourseId(courseId, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else if (search != null && !search.trim().isEmpty()) {
            var dto = new PageDto<>(service.searchByTitleOrDescription(search, search, PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        } else {
            var dto = new PageDto<>(service.list(PageRequest.of(page, size)));
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
        }
    }

    @Operation(summary = "Add target classes to a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @PostMapping("/{id}/classes")
    public ResponseEntity<ApiSuccessResponse<Void>> addTargetClasses(
            @PathVariable Long id,
            @RequestBody Set<Long> classIds) {
        service.addTargetClasses(id, classIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Remove target classes from a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @DeleteMapping("/{id}/classes")
    public ResponseEntity<ApiSuccessResponse<Void>> removeTargetClasses(
            @PathVariable Long id,
            @RequestBody Set<Long> classIds) {
        service.removeTargetClasses(id, classIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Add target courses to a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @PostMapping("/{id}/courses")
    public ResponseEntity<ApiSuccessResponse<Void>> addTargetCourses(
            @PathVariable Long id,
            @RequestBody Set<Long> courseIds) {
        service.addTargetCourses(id, courseIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Remove target courses from a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @DeleteMapping("/{id}/courses")
    public ResponseEntity<ApiSuccessResponse<Void>> removeTargetCourses(
            @PathVariable Long id,
            @RequestBody Set<Long> courseIds) {
        service.removeTargetCourses(id, courseIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Add teachers to a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @PostMapping("/{id}/teachers")
    public ResponseEntity<ApiSuccessResponse<Void>> addTeachers(
            @PathVariable Long id,
            @RequestBody Set<Long> teacherIds) {
        service.addTeachers(id, teacherIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Remove teachers from a learning resource")
    @Parameter(name = "id", description = "ID of the resource", required = true)
    @DeleteMapping("/{id}/teachers")
    public ResponseEntity<ApiSuccessResponse<Void>> removeTeachers(
            @PathVariable Long id,
            @RequestBody Set<Long> teacherIds) {
        service.removeTeachers(id, teacherIds);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Download a learning resource file")
    @Parameter(name = "filename", description = "Name of the file to download", required = true)
    @GetMapping("/files/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable String filename) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads/learning-resources").resolve(filename);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error downloading file: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }
} 