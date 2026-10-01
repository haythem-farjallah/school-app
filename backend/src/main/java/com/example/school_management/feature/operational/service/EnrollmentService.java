package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.EnrollmentDto;
import com.example.school_management.feature.operational.dto.EnrollmentStatsDto;
import com.example.school_management.feature.operational.dto.AutoEnrollmentResultDto;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface EnrollmentService {
    
    /**
     * Get all enrollments with optional filters
     */
    Page<EnrollmentDto> getAllEnrollments(Pageable pageable, String search, EnrollmentStatus status);
    
    /**
     * Enroll a student in a class
     */
    EnrollmentDto enrollStudent(Long studentId, Long classId);
    
    /**
     * Transfer a student to another class of the same academic year. The ACTIVE enrollment becomes
     * TRANSFERRED and a new ACTIVE enrollment is created and returned.
     */
    EnrollmentDto transferStudent(Long enrollmentId, Long newClassId);
    
    /**
     * Complete or withdraw an ACTIVE enrollment. TRANSFERRED is reserved for the transfer workflow.
     */
    EnrollmentDto updateEnrollmentStatus(Long enrollmentId, EnrollmentStatus status);
    
    /**
     * Withdraw a student from an ACTIVE enrollment
     */
    void withdrawEnrollment(Long enrollmentId, String reason);
    
    /**
     * Get enrollment by ID
     */
    EnrollmentDto getEnrollment(Long enrollmentId);
    
    /**
     * Get all enrollments for a student
     */
    Page<EnrollmentDto> getStudentEnrollments(Long studentId, Pageable pageable);
    
    /**
     * Get all enrollments for a class
     */
    Page<EnrollmentDto> getClassEnrollments(Long classId, Pageable pageable);
    
    /**
     * Get enrollments by status
     */
    Page<EnrollmentDto> getEnrollmentsByStatus(EnrollmentStatus status, Pageable pageable);
    
    /**
     * Get enrollments within a date range
     */
    Page<EnrollmentDto> getEnrollmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Get enrollment statistics for a class
     */
    EnrollmentStatsDto getClassEnrollmentStats(Long classId);
    
    /**
     * Get enrollment statistics for a student
     */
    EnrollmentStatsDto getStudentEnrollmentStats(Long studentId);
    
    /**
     * Bulk enroll students in a class
     */
    void bulkEnrollStudents(Long classId, java.util.List<Long> studentIds);
    
    /**
     * Check if student can be enrolled in class (capacity, prerequisites, etc.)
     */
    boolean canEnrollStudent(Long studentId, Long classId);
    
    /**
     * Automatically enroll all unenrolled students into appropriate classes
     */
    AutoEnrollmentResultDto autoEnrollAllStudents();
    
    /**
     * Automatically enroll students of a specific grade level
     */
    AutoEnrollmentResultDto autoEnrollByGradeLevel(String gradeLevel);
    
    /**
     * Preview what auto-enrollment would do without actually enrolling
     */
    AutoEnrollmentResultDto previewAutoEnrollment();
} 