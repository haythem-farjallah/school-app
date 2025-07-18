package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.dto.AttendanceStatisticsDto;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance Management", description = "APIs for managing attendance records")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @Operation(summary = "Record attendance for a single user")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> recordAttendance(
            @Valid @RequestBody AttendanceDto attendanceDto) {
        log.debug("Recording attendance for user: {}", attendanceDto.getUserId());
        
        AttendanceDto recorded = attendanceService.recordAttendance(attendanceDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Attendance recorded successfully", recorded));
    }

    @PostMapping("/batch")
    @Operation(summary = "Record attendance for multiple users")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> recordBatchAttendance(
            @Valid @RequestBody List<AttendanceDto> attendanceDtos) {
        log.debug("Recording batch attendance for {} users", attendanceDtos.size());
        
        List<AttendanceDto> recorded = attendanceService.recordBatchAttendance(attendanceDtos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Batch attendance recorded successfully", recorded));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get attendance for a specific user in date range")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getUserAttendance(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting attendance for user {} from {} to {}", userId, startDate, endDate);
        
        List<AttendanceDto> attendances = attendanceService.getUserAttendance(userId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("User attendance retrieved successfully", attendances));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get attendance for a specific class on a date")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getClassAttendance(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Getting class attendance for class {} on date {}", classId, date);
        
        List<AttendanceDto> attendances = attendanceService.getClassAttendance(classId, date);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class attendance retrieved successfully", attendances));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get attendance for a specific course on a date")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getCourseAttendance(
            @PathVariable Long courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Getting course attendance for course {} on date {}", courseId, date);
        
        List<AttendanceDto> attendances = attendanceService.getCourseAttendance(courseId, date);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Course attendance retrieved successfully", attendances));
    }

    @GetMapping("/statistics/user/{userId}")
    @Operation(summary = "Get attendance statistics for a user")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<AttendanceStatisticsDto>> getUserAttendanceStatistics(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting attendance statistics for user {} from {} to {}", userId, startDate, endDate);
        
        AttendanceStatisticsDto statistics = attendanceService.getUserAttendanceStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("User attendance statistics retrieved successfully", statistics));
    }

    @GetMapping("/statistics/class/{classId}")
    @Operation(summary = "Get attendance statistics for a class")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceStatisticsDto>>> getClassAttendanceStatistics(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting attendance statistics for class {} from {} to {}", classId, startDate, endDate);
        
        List<AttendanceStatisticsDto> statistics = attendanceService.getClassAttendanceStatistics(classId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class attendance statistics retrieved successfully", statistics));
    }

    @PutMapping("/{attendanceId}")
    @Operation(summary = "Update attendance record")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> updateAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody AttendanceDto attendanceDto) {
        log.debug("Updating attendance {}", attendanceId);
        
        AttendanceDto updated = attendanceService.updateAttendance(attendanceId, attendanceDto);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance updated successfully", updated));
    }

    @DeleteMapping("/{attendanceId}")
    @Operation(summary = "Delete attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteAttendance(@PathVariable Long attendanceId) {
        log.debug("Deleting attendance {}", attendanceId);
        
        attendanceService.deleteAttendance(attendanceId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance deleted successfully", null));
    }

    @GetMapping("/type/{userType}")
    @Operation(summary = "Get attendance by user type with pagination")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Page<AttendanceDto>>> getAttendanceByUserType(
            @PathVariable UserType userType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        log.debug("Getting attendance by user type {} from {} to {}", userType, startDate, endDate);
        
        Page<AttendanceDto> attendances = attendanceService.getAttendanceByUserType(userType, startDate, endDate, pageable);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance by user type retrieved successfully", attendances));
    }

    @PatchMapping("/{attendanceId}/excuse")
    @Operation(summary = "Mark attendance as excused")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> markAsExcused(
            @PathVariable Long attendanceId,
            @RequestParam String excuse) {
        log.debug("Marking attendance {} as excused", attendanceId);
        
        AttendanceDto updated = attendanceService.markAsExcused(attendanceId, excuse);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance marked as excused successfully", updated));
    }

    @PatchMapping("/{attendanceId}/late")
    @Operation(summary = "Mark attendance as late")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> markAsLate(
            @PathVariable Long attendanceId,
            @RequestParam String remarks) {
        log.debug("Marking attendance {} as late", attendanceId);
        
        AttendanceDto updated = attendanceService.markAsLate(attendanceId, remarks);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance marked as late successfully", updated));
    }

    @GetMapping("/filter")
    @Operation(summary = "Advanced filtering for attendance records")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Page<AttendanceDto>>> filterAttendance(
            @PageableDefault(size = 20) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest request) {
        log.debug("Advanced filtering attendance records");
        
        Page<AttendanceDto> attendances = attendanceService.findWithAdvancedFilters(pageable, request.getParameterMap());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Attendance filtered successfully", attendances));
    }
} 