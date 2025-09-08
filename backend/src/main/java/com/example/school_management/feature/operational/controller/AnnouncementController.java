package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Endpoints for managing announcements")
@SecurityRequirement(name = "bearerAuth")
public class AnnouncementController {

    private final AnnouncementService service;

    @Operation(summary = "Create a new announcement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Announcement created successfully")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<AnnouncementDto>> create(
            @RequestBody @Valid CreateAnnouncementRequest req) {
        log.debug("POST /announcements {}", req);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(req)));
    }

    @Operation(summary = "Update an existing announcement")
    @Parameter(name = "id", description = "ID of the announcement to update", required = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<AnnouncementDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateAnnouncementRequest req) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, req)));
    }

    @Operation(summary = "Get announcement details by ID")
    @Parameter(name = "id", description = "ID of the announcement to retrieve", required = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<AnnouncementDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Delete an announcement")
    @Parameter(name = "id", description = "ID of the announcement to delete", required = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "List announcements with pagination and filters")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<AnnouncementDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by importance") @RequestParam(required = false) String importance,
            @Parameter(description = "Filter by public status") @RequestParam(required = false) Boolean isPublic) {
        var dto = new PageDto<>(service.list(PageRequest.of(page, size), importance, isPublic));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Publish announcement to specific users")
    @Parameter(name = "id", description = "ID of the announcement", required = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiSuccessResponse<AnnouncementDto>> publish(
            @PathVariable Long id,
            @RequestBody @Valid PublishAnnouncementRequest req) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.publish(id, req)));
    }

    @Operation(summary = "Get public announcements for students/parents")
    @GetMapping("/public")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT', 'TEACHER', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageDto<AnnouncementDto>>> getPublicAnnouncements(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        var dto = new PageDto<>(service.getPublicAnnouncements(PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Get classes that current teacher can send announcements to")
    @GetMapping("/teacher-classes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<Object>> getTeacherClasses() {
        // This will be handled by the service layer
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.getTeacherClasses()));
    }

    @Operation(summary = "Create test teaching assignments for current teacher (DEV ONLY)")
    @PostMapping("/create-test-assignments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<String>> createTestAssignments() {
        service.createTestTeachingAssignments();
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Test teaching assignments created"));
    }
} 