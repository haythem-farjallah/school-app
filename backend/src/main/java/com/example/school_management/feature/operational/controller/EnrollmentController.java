package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.operational.dto.EnrollmentDto;
import com.example.school_management.feature.operational.dto.EnrollmentStatsDto;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Management", description = "APIs for managing student enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get all enrollments with optional filters")
    public ResponseEntity<ApiSuccessResponse<PageDto<EnrollmentDto>>> getAllEnrollments(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EnrollmentStatus status) {
        log.debug("Fetching enrollments with search: {}, status: {}", search, status);
        
        Page<EnrollmentDto> enrollments = enrollmentService.getAllEnrollments(pageable, search, status);
        var dto = new PageDto<>(enrollments);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollments retrieved successfully", dto));
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Enroll a student in a class")
    public ResponseEntity<ApiSuccessResponse<EnrollmentDto>> enrollStudent(
            @Valid @RequestBody EnrollStudentRequest request) {
        log.debug("Enrolling student {} in class {}", request.studentId(), request.classId());
        
        EnrollmentDto enrollment = enrollmentService.enrollStudent(request.studentId(), request.classId());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student enrolled successfully", enrollment));
    }

    @PutMapping("/{enrollmentId}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Transfer a student to another class")
    public ResponseEntity<ApiSuccessResponse<EnrollmentDto>> transferStudent(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody TransferStudentRequest request) {
        log.debug("Transferring enrollment {} to class {}", enrollmentId, request.newClassId());
        
        EnrollmentDto enrollment = enrollmentService.transferStudent(enrollmentId, request.newClassId());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student transferred successfully", enrollment));
    }

    @PutMapping("/{enrollmentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Update enrollment status")
    public ResponseEntity<ApiSuccessResponse<EnrollmentDto>> updateEnrollmentStatus(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request) {
        log.debug("Updating enrollment {} status to {}", enrollmentId, request.status());
        
        EnrollmentDto enrollment = enrollmentService.updateEnrollmentStatus(enrollmentId, request.status());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollment status updated successfully", enrollment));
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Drop/withdraw a student from enrollment")
    public ResponseEntity<ApiSuccessResponse<String>> dropEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody DropEnrollmentRequest request) {
        log.debug("Dropping enrollment {} with reason: {}", enrollmentId, request.reason());
        
        enrollmentService.dropEnrollment(enrollmentId, request.reason());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student dropped from enrollment successfully", ""));
    }

    @GetMapping("/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @Operation(summary = "Get enrollment details by ID")
    public ResponseEntity<ApiSuccessResponse<EnrollmentDto>> getEnrollment(
            @PathVariable Long enrollmentId) {
        log.debug("Fetching enrollment {}", enrollmentId);
        
        EnrollmentDto enrollment = enrollmentService.getEnrollment(enrollmentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollment retrieved successfully", enrollment));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @Operation(summary = "Get all enrollments for a student")
    public ResponseEntity<ApiSuccessResponse<PageDto<EnrollmentDto>>> getStudentEnrollments(
            @PathVariable Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching enrollments for student {}", studentId);
        
        Page<EnrollmentDto> enrollments = enrollmentService.getStudentEnrollments(studentId, pageable);
        var dto = new PageDto<>(enrollments);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student enrollments retrieved successfully", dto));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get all enrollments for a class")
    public ResponseEntity<ApiSuccessResponse<PageDto<EnrollmentDto>>> getClassEnrollments(
            @PathVariable Long classId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching enrollments for class {}", classId);
        
        Page<EnrollmentDto> enrollments = enrollmentService.getClassEnrollments(classId, pageable);
        var dto = new PageDto<>(enrollments);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class enrollments retrieved successfully", dto));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get enrollments by status")
    public ResponseEntity<ApiSuccessResponse<PageDto<EnrollmentDto>>> getEnrollmentsByStatus(
            @PathVariable EnrollmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching enrollments with status {}", status);
        
        Page<EnrollmentDto> enrollments = enrollmentService.getEnrollmentsByStatus(status, pageable);
        var dto = new PageDto<>(enrollments);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollments by status retrieved successfully", dto));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get enrollments within a date range")
    public ResponseEntity<ApiSuccessResponse<PageDto<EnrollmentDto>>> getEnrollmentsByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching enrollments between {} and {}", startDate, endDate);
        
        Page<EnrollmentDto> enrollments = enrollmentService.getEnrollmentsByDateRange(startDate, endDate, pageable);
        var dto = new PageDto<>(enrollments);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollments by date range retrieved successfully", dto));
    }

    @GetMapping("/stats/class/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get enrollment statistics for a class")
    public ResponseEntity<ApiSuccessResponse<EnrollmentStatsDto>> getClassEnrollmentStats(
            @PathVariable Long classId) {
        log.debug("Fetching enrollment stats for class {}", classId);
        
        EnrollmentStatsDto stats = enrollmentService.getClassEnrollmentStats(classId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class enrollment statistics retrieved successfully", stats));
    }

    @GetMapping("/stats/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @Operation(summary = "Get enrollment statistics for a student")
    public ResponseEntity<ApiSuccessResponse<EnrollmentStatsDto>> getStudentEnrollmentStats(
            @PathVariable Long studentId) {
        log.debug("Fetching enrollment stats for student {}", studentId);
        
        EnrollmentStatsDto stats = enrollmentService.getStudentEnrollmentStats(studentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student enrollment statistics retrieved successfully", stats));
    }

    @PostMapping("/bulk-enroll")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk enroll students in a class")
    public ResponseEntity<ApiSuccessResponse<String>> bulkEnrollStudents(
            @Valid @RequestBody BulkEnrollStudentsRequest request) {
        log.debug("Bulk enrolling {} students in class {}", request.studentIds().size(), request.classId());
        
        enrollmentService.bulkEnrollStudents(request.classId(), request.studentIds());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Students bulk enrolled successfully", ""));
    }

    @GetMapping("/can-enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Check if a student can be enrolled in a class")
    public ResponseEntity<ApiSuccessResponse<Boolean>> canEnrollStudent(
            @RequestParam Long studentId,
            @RequestParam Long classId) {
        log.debug("Checking if student {} can be enrolled in class {}", studentId, classId);
        
        boolean canEnroll = enrollmentService.canEnrollStudent(studentId, classId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollment eligibility checked", canEnroll));
    }

    // Request DTOs
    public record EnrollStudentRequest(
            @NotNull Long studentId,
            @NotNull Long classId
    ) {}

    public record TransferStudentRequest(
            @NotNull Long newClassId
    ) {}

    public record UpdateEnrollmentStatusRequest(
            @NotNull EnrollmentStatus status
    ) {}

    public record DropEnrollmentRequest(
            @NotNull String reason
    ) {}

    public record BulkEnrollStudentsRequest(
            @NotNull Long classId,
            @NotEmpty List<Long> studentIds
    ) {}
} 