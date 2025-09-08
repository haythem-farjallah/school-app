package com.example.school_management.feature.operational.controller;

import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.AuditEvent;
import com.example.school_management.feature.operational.service.GradeService;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Validated
public class GradeController {
    private final GradeService gradeService;

    // ===== GENERAL GRADE LISTING =====
    
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<PageDto<GradeResponse>>> getAllGrades(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long courseId) {
        Page<GradeResponse> grades = gradeService.getAllGrades(pageable, search, courseId);
        var dto = new PageDto<>(grades);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grades retrieved successfully", dto));
    }

    // ===== CORE GRADE OPERATIONS =====
    
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<String>> enterBulkGrades(@Valid @RequestBody BulkGradeEntryRequest request) {
        gradeService.enterBulkGrades(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Bulk grades entered successfully", null));
    }

    @PatchMapping("/{gradeId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<String>> updateGrade(@PathVariable Long gradeId, @Valid @RequestBody UpdateGradeRequest request) {
        gradeService.updateGrade(gradeId, request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grade updated successfully", null));
    }

    @DeleteMapping("/{gradeId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<String>> deleteGrade(@PathVariable Long gradeId, @Valid @RequestBody DeleteGradeRequest request) {
        gradeService.deleteGrade(gradeId, request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grade deleted successfully", null));
    }

    // ===== GRADE RETRIEVAL =====
    
    @GetMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<GradeResponse>> getGradeById(@PathVariable Long gradeId) {
        GradeResponse grade = gradeService.getGradeById(gradeId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grade retrieved successfully", grade));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<GradeResponse>>> getGradesByStudentId(@PathVariable Long studentId) {
        List<GradeResponse> grades = gradeService.getGradesByStudentId(studentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student grades retrieved successfully", grades));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<GradeResponse>>> getGradesByClassId(@PathVariable Long classId) {
        List<GradeResponse> grades = gradeService.getGradesByClassId(classId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class grades retrieved successfully", grades));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<GradeResponse>>> getGradesByTeacherId(@PathVariable Long teacherId) {
        List<GradeResponse> grades = gradeService.getGradesByTeacherId(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher grades retrieved successfully", grades));
    }

    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<GradeResponse>>> getGradesByEnrollmentId(@PathVariable Long enrollmentId) {
        List<GradeResponse> grades = gradeService.getGradesByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollment grades retrieved successfully", grades));
    }

    // ===== PAGINATED RETRIEVAL =====
    
    @GetMapping("/student/{studentId}/paged")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageDto<GradeResponse>>> getGradesByStudentIdPaged(
            @PathVariable Long studentId, 
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GradeResponse> grades = gradeService.getGradesByStudentId(studentId, pageable);
        var dto = new PageDto<>(grades);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student grades retrieved successfully", dto));
    }

    @GetMapping("/class/{classId}/paged")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<PageDto<GradeResponse>>> getGradesByClassIdPaged(
            @PathVariable Long classId, 
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GradeResponse> grades = gradeService.getGradesByClassId(classId, pageable);
        var dto = new PageDto<>(grades);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class grades retrieved successfully", dto));
    }

    @GetMapping("/enrollment/{enrollmentId}/paged")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageDto<GradeResponse>>> getGradesByEnrollmentIdPaged(
            @PathVariable Long enrollmentId, 
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GradeResponse> grades = gradeService.getGradesByEnrollmentId(enrollmentId, pageable);
        var dto = new PageDto<>(grades);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Enrollment grades retrieved successfully", dto));
    }

    // ===== GRADE STATISTICS =====
    
    @GetMapping("/statistics/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<GradeStatistics>> getStudentGradeStatistics(@PathVariable Long studentId) {
        GradeStatistics statistics = gradeService.getStudentGradeStatistics(studentId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student grade statistics retrieved successfully", statistics));
    }

    @GetMapping("/statistics/student/{studentId}/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<GradeStatistics>> getStudentGradeStatisticsForClass(
            @PathVariable Long studentId, 
            @PathVariable Long classId) {
        GradeStatistics statistics = gradeService.getStudentGradeStatisticsForClass(studentId, classId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student class grade statistics retrieved successfully", statistics));
    }

    @GetMapping("/statistics/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<GradeStatistics>> getClassGradeStatistics(@PathVariable Long classId) {
        GradeStatistics statistics = gradeService.getClassGradeStatistics(classId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class grade statistics retrieved successfully", statistics));
    }

    @GetMapping("/statistics/student/{studentId}/date-range")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<GradeStatistics>> getGradeStatisticsForDateRange(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        GradeStatistics statistics = gradeService.getGradeStatisticsForDateRange(studentId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Date range grade statistics retrieved successfully", statistics));
    }

    // ===== AUDIT AND HISTORY =====
    
    @GetMapping("/{gradeId}/audit-history")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AuditEvent>>> getGradeAuditHistory(@PathVariable Long gradeId) {
        List<AuditEvent> auditHistory = gradeService.getGradeAuditHistory(gradeId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grade audit history retrieved successfully", auditHistory));
    }

    // ===== VALIDATION ENDPOINTS =====
    
    @GetMapping("/{gradeId}/can-edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> canEditGrade(@PathVariable Long gradeId, @RequestParam Long userId) {
        boolean canEdit = gradeService.canEditGrade(gradeId, userId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Edit permission checked successfully", canEdit));
    }

    @GetMapping("/{gradeId}/can-delete")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> canDeleteGrade(@PathVariable Long gradeId, @RequestParam Long userId) {
        boolean canDelete = gradeService.canDeleteGrade(gradeId, userId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Delete permission checked successfully", canDelete));
    }

    @GetMapping("/exists")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> gradeExistsForEnrollmentAndContent(
            @RequestParam Long enrollmentId, 
            @RequestParam String content) {
        boolean exists = gradeService.gradeExistsForEnrollmentAndContent(enrollmentId, content);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grade existence checked successfully", exists));
    }

    // ===== ADVANCED FILTERING =====
    
    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageDto<GradeResponse>>> filterGrades(
            @PageableDefault(size = 20) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest request) {
        Page<GradeResponse> grades = gradeService.findWithAdvancedFilters(pageable, request.getParameterMap());
        var dto = new PageDto<>(grades);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grades filtered successfully", dto));
    }
    
    // ===== ENHANCED GRADE MANAGEMENT ENDPOINTS =====
    
    // Teacher Grade Management
    @GetMapping("/teacher/{teacherId}/classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<TeacherGradeClassView>>> getTeacherGradeClasses(@PathVariable Long teacherId) {
        List<TeacherGradeClassView> classes = gradeService.getTeacherGradeClasses(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher grade classes retrieved successfully", classes));
    }
    
    @GetMapping("/teacher/{teacherId}/class/{classId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TeacherGradeClassView>> getTeacherGradeClass(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long courseId) {
        TeacherGradeClassView classView = gradeService.getTeacherGradeClass(teacherId, classId, courseId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher grade class retrieved successfully", classView));
    }
    
    @PostMapping("/enhanced")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<EnhancedGradeResponse>> createEnhancedGrade(@Valid @RequestBody CreateEnhancedGradeRequest request) {
        EnhancedGradeResponse response = gradeService.createEnhancedGrade(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Enhanced grade created successfully", response));
    }
    
    @PostMapping("/bulk-entry")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiSuccessResponse<List<EnhancedGradeResponse>>> createBulkEnhancedGrades(@Valid @RequestBody BulkEnhancedGradeEntryRequest request) {
        List<EnhancedGradeResponse> responses = gradeService.createBulkEnhancedGrades(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Bulk enhanced grades created successfully", responses));
    }
    
    // Staff Grade Review
    @GetMapping("/staff/reviews")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<StaffGradeReview>>> getStaffGradeReviews(
            @RequestParam Long classId, 
            @RequestParam CreateEnhancedGradeRequest.Semester semester) {
        List<StaffGradeReview> reviews = gradeService.getStaffGradeReviews(classId, semester);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Staff grade reviews retrieved successfully", reviews));
    }
    
    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<String>> approveGrades(@Valid @RequestBody ApproveGradesRequest request) {
        gradeService.approveGrades(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Grades approved successfully", null));
    }
    
    // Student Grade Sheet
    @GetMapping("/student/{studentId}/sheet")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<StudentGradeSheet>> getStudentGradeSheet(
            @PathVariable Long studentId, 
            @RequestParam CreateEnhancedGradeRequest.Semester semester) {
        StudentGradeSheet gradeSheet = gradeService.getStudentGradeSheet(studentId, semester);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student grade sheet retrieved successfully", gradeSheet));
    }
    
    @GetMapping("/student/{studentId}/export")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT', 'PARENT', 'ADMIN', 'STAFF')")
    public ResponseEntity<byte[]> exportStudentGradeSheet(
            @PathVariable Long studentId, 
            @RequestParam CreateEnhancedGradeRequest.Semester semester) {
        byte[] pdfBytes = gradeService.exportStudentGradeSheet(studentId, semester);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"grade-sheet-" + studentId + "-" + semester + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
} 