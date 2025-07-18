package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.operational.dto.CreateResourceCommentRequest;
import com.example.school_management.feature.operational.dto.ResourceCommentDto;
import com.example.school_management.feature.operational.service.ResourceCommentService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/resource-comments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Resource Comments", description = "Endpoints for managing learning resource comments")
@SecurityRequirement(name = "bearerAuth")
public class ResourceCommentController {

    private final ResourceCommentService service;

    @Operation(summary = "Create a new comment on a learning resource")
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ResourceCommentDto>> create(
            @Valid @RequestBody CreateResourceCommentRequest request) {
        log.debug("POST /resource-comments {}", request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(request)));
    }

    @Operation(summary = "Get comment details by ID")
    @Parameter(name = "id", description = "ID of the comment to retrieve", required = true)
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ResourceCommentDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Delete a comment")
    @Parameter(name = "id", description = "ID of the comment to delete", required = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "List comments for a specific learning resource")
    @Parameter(name = "resourceId", description = "ID of the learning resource", required = true)
    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<ResourceCommentDto>>> getByResource(
            @PathVariable Long resourceId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        var dto = new PageDto<>(service.findByResourceId(resourceId, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List comments by a specific user")
    @Parameter(name = "userId", description = "ID of the user", required = true)
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<ResourceCommentDto>>> getByUser(
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        var dto = new PageDto<>(service.findByUserId(userId, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List all comments with pagination")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<ResourceCommentDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        var dto = new PageDto<>(service.list(PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
} 