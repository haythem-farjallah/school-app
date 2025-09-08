package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import com.example.school_management.feature.operational.dto.TeacherAttendanceResponse;
import com.example.school_management.feature.operational.dto.TeacherAttendanceStatistics;
import com.example.school_management.feature.operational.service.TeacherAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/teacher-attendance")
@RequiredArgsConstructor
@Tag(name = "Teacher Attendance Management", description = "APIs for managing teacher attendance records")
public class TeacherAttendanceController {
    
    private final TeacherAttendanceService teacherAttendanceService;
    
    @PostMapping
    @Operation(summary = "Create teacher attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TeacherAttendanceResponse>> createTeacherAttendance(
            @Valid @RequestBody TeacherAttendanceRequest request) {
        log.debug("Creating teacher attendance record for teacher: {}", request.getTeacherId());
        
        TeacherAttendanceResponse response = teacherAttendanceService.createTeacherAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Teacher attendance recorded successfully", response));
    }
    
    @GetMapping
    @Operation(summary = "Get teacher attendance records with optional filters")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<List<TeacherAttendanceResponse>>> getTeacherAttendance(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting teacher attendance records with filters - teacherId: {}, startDate: {}, endDate: {}", 
                teacherId, startDate, endDate);
        
        List<TeacherAttendanceResponse> attendanceRecords = teacherAttendanceService.getTeacherAttendance(teacherId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher attendance records retrieved successfully", attendanceRecords));
    }
    
    @GetMapping("/date/{date}")
    @Operation(summary = "Get teacher attendance records for a specific date")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<TeacherAttendanceResponse>>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Getting teacher attendance records for date: {}", date);
        
        List<TeacherAttendanceResponse> attendanceRecords = teacherAttendanceService.getAttendanceByDate(date);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher attendance records for date retrieved successfully", attendanceRecords));
    }
    
    @GetMapping("/statistics/{teacherId}")
    @Operation(summary = "Get teacher attendance statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<TeacherAttendanceStatistics>> getTeacherAttendanceStatistics(
            @PathVariable Long teacherId) {
        log.debug("Getting teacher attendance statistics for teacher: {}", teacherId);
        
        TeacherAttendanceStatistics statistics = teacherAttendanceService.getTeacherAttendanceStatistics(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher attendance statistics retrieved successfully", statistics));
    }
    
    @PutMapping("/{attendanceId}")
    @Operation(summary = "Update teacher attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TeacherAttendanceResponse>> updateTeacherAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody TeacherAttendanceRequest request) {
        log.debug("Updating teacher attendance record with ID: {}", attendanceId);
        
        TeacherAttendanceResponse response = teacherAttendanceService.updateTeacherAttendance(attendanceId, request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher attendance record updated successfully", response));
    }
    
    @DeleteMapping("/{attendanceId}")
    @Operation(summary = "Delete teacher attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<String>> deleteTeacherAttendance(@PathVariable Long attendanceId) {
        log.debug("Deleting teacher attendance record with ID: {}", attendanceId);
        
        teacherAttendanceService.deleteTeacherAttendance(attendanceId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher attendance record deleted successfully", null));
    }
    
    @GetMapping("/exists")
    @Operation(summary = "Check if attendance exists for teacher and date")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> attendanceExists(
            @RequestParam Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Checking if attendance exists for teacher: {} on date: {}", teacherId, date);
        
        boolean exists = teacherAttendanceService.attendanceExistsForTeacherAndDate(teacherId, date);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance existence checked successfully", exists));
    }
}
