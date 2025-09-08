package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import com.example.school_management.feature.operational.dto.TeacherAttendanceResponse;
import com.example.school_management.feature.operational.dto.TeacherAttendanceStatistics;

import java.time.LocalDate;
import java.util.List;

public interface TeacherAttendanceService {
    
    // Create teacher attendance record
    TeacherAttendanceResponse createTeacherAttendance(TeacherAttendanceRequest request);
    
    // Get teacher attendance records with optional filters
    List<TeacherAttendanceResponse> getTeacherAttendance(Long teacherId, LocalDate startDate, LocalDate endDate);
    
    // Get attendance records for a specific date
    List<TeacherAttendanceResponse> getAttendanceByDate(LocalDate date);
    
    // Get teacher attendance statistics
    TeacherAttendanceStatistics getTeacherAttendanceStatistics(Long teacherId);
    
    // Update teacher attendance record
    TeacherAttendanceResponse updateTeacherAttendance(Long attendanceId, TeacherAttendanceRequest request);
    
    // Delete teacher attendance record
    void deleteTeacherAttendance(Long attendanceId);
    
    // Check if attendance exists for teacher and date
    boolean attendanceExistsForTeacherAndDate(Long teacherId, LocalDate date);
}
