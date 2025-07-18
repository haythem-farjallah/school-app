package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.operational.dto.AdminFeedDto;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import com.example.school_management.feature.operational.service.AdminFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin-feeds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Feeds", description = "Endpoints for monitoring system activities and events")
@SecurityRequirement(name = "bearerAuth")
public class AdminFeedController {

    private final AdminFeedService service;

    @Operation(summary = "Get admin feed details by ID")
    @Parameter(name = "id", description = "ID of the feed to retrieve", required = true)
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<AdminFeedDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @Operation(summary = "Delete an admin feed")
    @Parameter(name = "id", description = "ID of the feed to delete", required = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Mark a feed as read")
    @Parameter(name = "id", description = "ID of the feed to mark as read", required = true)
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiSuccessResponse<Void>> markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Mark all feeds as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiSuccessResponse<Void>> markAllAsRead() {
        service.markAllAsRead();
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "List all admin feeds with pagination")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.list(PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by event type")
    @Parameter(name = "eventType", description = "Type of audit event", required = true)
    @GetMapping("/event-type/{eventType}")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findByEventType(
            @PathVariable AuditEventType eventType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findByEventType(eventType, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by notification type")
    @Parameter(name = "notificationType", description = "Type of notification", required = true)
    @GetMapping("/notification-type/{notificationType}")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findByNotificationType(
            @PathVariable NotificationType notificationType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findByNotificationType(notificationType, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by severity level")
    @Parameter(name = "severity", description = "Severity level (INFO, WARNING, ERROR, CRITICAL)", required = true)
    @GetMapping("/severity/{severity}")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findBySeverity(
            @PathVariable String severity,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findBySeverity(severity, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by entity type")
    @Parameter(name = "entityType", description = "Type of entity (User, Course, Class, etc.)", required = true)
    @GetMapping("/entity-type/{entityType}")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findByEntityType(
            @PathVariable String entityType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findByEntityType(entityType, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by triggered user")
    @Parameter(name = "userId", description = "ID of the user who triggered the event", required = true)
    @GetMapping("/triggered-by/{userId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findByTriggeredBy(
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findByTriggeredBy(userId, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by target user")
    @Parameter(name = "userId", description = "ID of the user affected by the event", required = true)
    @GetMapping("/target-user/{userId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findByTargetUser(
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findByTargetUser(userId, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List unread feeds")
    @GetMapping("/unread")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findUnread(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findUnread(PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List high priority feeds (ERROR and CRITICAL)")
    @GetMapping("/high-priority")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findHighPriority(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findHighPriority(PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "List feeds by date range")
    @GetMapping("/date-range")
    public ResponseEntity<ApiSuccessResponse<PageDto<AdminFeedDto>>> findByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var dto = new PageDto<>(service.findByDateRange(startDate, endDate, PageRequest.of(page, size)));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Get recent feeds")
    @Parameter(name = "limit", description = "Number of recent feeds to retrieve", required = true)
    @GetMapping("/recent")
    public ResponseEntity<ApiSuccessResponse<List<AdminFeedDto>>> findRecent(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.findRecent(limit)));
    }

    @Operation(summary = "Get feed statistics")
    @GetMapping("/stats")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = Map.of(
                "unreadCount", service.countUnread(),
                "userCreatedCount", service.countByEventType(AuditEventType.USER_CREATED),
                "courseCreatedCount", service.countByEventType(AuditEventType.COURSE_CREATED),
                "classCreatedCount", service.countByEventType(AuditEventType.CLASS_CREATED),
                "errorCount", service.countBySeverity("ERROR"),
                "criticalCount", service.countBySeverity("CRITICAL")
        );
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", stats));
    }
} 