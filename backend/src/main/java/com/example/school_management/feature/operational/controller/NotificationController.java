package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.operational.dto.NotificationDto;
import com.example.school_management.feature.operational.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'PARENT', 'TEACHER', 'STAFF', 'ADMIN')")
@Tag(name = "Notifications", description = "Endpoints for managing user notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService service;

    @Operation(summary = "Get current user's notifications")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<NotificationDto>>> getMyNotifications(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by read status") @RequestParam(required = false) Boolean readStatus) {
        var dto = new PageDto<>(service.getMyNotifications(PageRequest.of(page, size), readStatus));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Get notification details by ID")
    @Parameter(name = "id", description = "ID of the notification to retrieve", required = true)
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<NotificationDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Mark notification as read")
    @Parameter(name = "id", description = "ID of the notification to mark as read", required = true)
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiSuccessResponse<NotificationDto>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.markAsRead(id)));
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiSuccessResponse<Void>> markAllAsRead() {
        service.markAllAsRead();
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Delete a notification")
    @Parameter(name = "id", description = "ID of the notification to delete", required = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiSuccessResponse<Integer>> getUnreadCount() {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.getUnreadCount()));
    }
} 