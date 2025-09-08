package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface GradeService {
    
    // General grade listing
    Page<GradeResponse> getAllGrades(Pageable pageable, String search, Long courseId);
    
    // Core grade operations
    void enterBulkGrades(BulkGradeEntryRequest request);
    void updateGrade(Long gradeId, UpdateGradeRequest request);
    void deleteGrade(Long gradeId, DeleteGradeRequest request);
    
    // Grade retrieval
    GradeResponse getGradeById(Long gradeId);
    List<GradeResponse> getGradesByStudentId(Long studentId);
    List<GradeResponse> getGradesByClassId(Long classId);
    List<GradeResponse> getGradesByTeacherId(Long teacherId);
    List<GradeResponse> getGradesByEnrollmentId(Long enrollmentId);
    
    // Paginated retrieval
    Page<GradeResponse> getGradesByStudentId(Long studentId, Pageable pageable);
    Page<GradeResponse> getGradesByClassId(Long classId, Pageable pageable);
    Page<GradeResponse> getGradesByEnrollmentId(Long enrollmentId, Pageable pageable);
    
    // Grade statistics
    GradeStatistics getStudentGradeStatistics(Long studentId);
    GradeStatistics getStudentGradeStatisticsForClass(Long studentId, Long classId);
    GradeStatistics getClassGradeStatistics(Long classId);
    GradeStatistics getGradeStatisticsForDateRange(Long studentId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Audit and history
    List<AuditEvent> getGradeAuditHistory(Long gradeId);
    
    // Validation and checks
    boolean canEditGrade(Long gradeId, Long userId);
    boolean canDeleteGrade(Long gradeId, Long userId);
    boolean gradeExistsForEnrollmentAndContent(Long enrollmentId, String content);
    
    // Advanced filtering
    Page<GradeResponse> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap);
    
    // ===== ENHANCED GRADE MANAGEMENT METHODS =====
    
    // Teacher Grade Management
    List<TeacherGradeClassView> getTeacherGradeClasses(Long teacherId);
    TeacherGradeClassView getTeacherGradeClass(Long teacherId, Long classId, Long courseId);
    List<EnhancedGradeResponse> createBulkEnhancedGrades(BulkEnhancedGradeEntryRequest request);
    EnhancedGradeResponse createEnhancedGrade(CreateEnhancedGradeRequest request);
    
    // Staff Grade Review
    List<StaffGradeReview> getStaffGradeReviews(Long classId, CreateEnhancedGradeRequest.Semester semester);
    void approveGrades(ApproveGradesRequest request);
    
    // Student Grade Sheet
    StudentGradeSheet getStudentGradeSheet(Long studentId, CreateEnhancedGradeRequest.Semester semester);
    byte[] exportStudentGradeSheet(Long studentId, CreateEnhancedGradeRequest.Semester semester);
} 