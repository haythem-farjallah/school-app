package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.dto.DashboardDto;
import com.example.school_management.feature.operational.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Management", description = "APIs for dashboard data across different user roles")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/current-user")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'TEACHER_READ_WRITE', 'PARENT_READ', 'STUDENT_READ')")
    @Operation(summary = "Get dashboard data for current authenticated user")
    public ResponseEntity<ApiSuccessResponse<Object>> getCurrentUserDashboard() {
        log.debug("Fetching dashboard for current user");
        
        Object dashboard = dashboardService.getCurrentUserDashboard();
        return ResponseEntity.ok(new ApiSuccessResponse<>("Dashboard data retrieved successfully", dashboard));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'TEACHER_READ_WRITE', 'PARENT_READ', 'STUDENT_READ')")
    @Operation(summary = "Get student-specific dashboard data")
    public ResponseEntity<ApiSuccessResponse<Object>> getStudentDashboard(
            @PathVariable Long studentId) {
        log.debug("Fetching student dashboard for student {}", studentId);
        
        Object dashboard = dashboardService.getStudentDashboard(studentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'TEACHER_READ_WRITE')")
    @Operation(summary = "Get teacher-specific dashboard data")
    public ResponseEntity<ApiSuccessResponse<Object>> getTeacherDashboard(
            @PathVariable Long teacherId) {
        log.debug("Fetching teacher dashboard for teacher {}", teacherId);
        
        Object dashboard = dashboardService.getTeacherDashboard(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/parent/{parentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'PARENT_READ')")
    @Operation(summary = "Get parent-specific dashboard data")
    public ResponseEntity<ApiSuccessResponse<Object>> getParentDashboard(
            @PathVariable Long parentId) {
        log.debug("Fetching parent dashboard for parent {}", parentId);
        
        Object dashboard = dashboardService.getParentDashboard(parentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Parent dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/admin/{adminId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE')")
    @Operation(summary = "Get admin-specific dashboard data with system statistics")
    public ResponseEntity<ApiSuccessResponse<Object>> getAdminDashboard(
            @PathVariable Long adminId) {
        log.debug("Fetching admin dashboard for admin {}", adminId);
        
        Object dashboard = dashboardService.getAdminDashboard(adminId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Admin dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/base/{userId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'TEACHER_READ_WRITE', 'PARENT_READ', 'STUDENT_READ')")
    @Operation(summary = "Get base dashboard information (common across all roles)")
    public ResponseEntity<ApiSuccessResponse<DashboardDto>> getBaseDashboardInfo(
            @PathVariable Long userId) {
        log.debug("Fetching base dashboard info for user {}", userId);
        
        DashboardDto dashboard = dashboardService.getBaseDashboardInfo(userId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Base dashboard info retrieved successfully", dashboard));
    }

    // Additional convenience endpoints
    
    @GetMapping("/student/summary/{studentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'TEACHER_READ_WRITE', 'PARENT_READ', 'STUDENT_READ')")
    @Operation(summary = "Get student summary data for quick overview")
    public ResponseEntity<ApiSuccessResponse<Object>> getStudentSummary(
            @PathVariable Long studentId) {
        log.debug("Fetching student summary for student {}", studentId);
        
        // This could be a lighter version of student dashboard
        Object summary = dashboardService.getStudentDashboard(studentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student summary retrieved successfully", summary));
    }

    @GetMapping("/teacher/classes-overview/{teacherId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'TEACHER_READ_WRITE')")
    @Operation(summary = "Get teacher's classes overview")
    public ResponseEntity<ApiSuccessResponse<Object>> getTeacherClassesOverview(
            @PathVariable Long teacherId) {
        log.debug("Fetching classes overview for teacher {}", teacherId);
        
        Object overview = dashboardService.getTeacherDashboard(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher classes overview retrieved successfully", overview));
    }

    @GetMapping("/parent/children-summary/{parentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE', 'PARENT_READ')")
    @Operation(summary = "Get parent's children summary")
    public ResponseEntity<ApiSuccessResponse<Object>> getParentChildrenSummary(
            @PathVariable Long parentId) {
        log.debug("Fetching children summary for parent {}", parentId);
        
        Object summary = dashboardService.getParentDashboard(parentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Parent children summary retrieved successfully", summary));
    }

    @GetMapping("/admin/system-health")
    @PreAuthorize("hasAnyAuthority('ADMIN_READ_WRITE')")
    @Operation(summary = "Get system health and statistics")
    public ResponseEntity<ApiSuccessResponse<Object>> getSystemHealth() {
        log.debug("Fetching system health statistics");
        
        // This could extract just the system stats portion
        Object health = dashboardService.getAdminDashboard(null);
        return ResponseEntity.ok(new ApiSuccessResponse<>("System health retrieved successfully", health));
    }
} 