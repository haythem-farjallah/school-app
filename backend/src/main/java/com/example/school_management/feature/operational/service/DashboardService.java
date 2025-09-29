package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.DashboardDto;

import java.time.LocalDate;

public interface DashboardService {
    
    /**
     * Get student dashboard data
     */
    Object getStudentDashboard(Long studentId);
    
    /**
     * Get teacher dashboard data
     */
    Object getTeacherDashboard(Long teacherId);
    
    /**
     * Get parent dashboard data
     */
    Object getParentDashboard(Long parentId);
    
    /**
     * Get admin dashboard data
     */
    Object getAdminDashboard(Long adminId);
    
    /**
     * Get staff dashboard data
     */
    Object getStaffDashboard(Long staffId);
    
    /**
     * Get dashboard data for current authenticated user
     */
    Object getCurrentUserDashboard();
    
    /**
     * Get basic dashboard info (common across all roles)
     */
    DashboardDto getBaseDashboardInfo(Long userId);

    /**
     * Get timetables for all children of a parent
     */
    Object getParentChildrenTimetables(Long parentId);

    /**
     * Get attendance data for all children of a parent
     */
    Object getParentChildrenAttendance(Long parentId, LocalDate startDate, LocalDate endDate);

    /**
     * Get comprehensive statistics for all children of a parent
     */
    Object getParentChildrenStatistics(Long parentId);

    /**
     * Get detailed information for a specific child
     */
    Object getChildDetails(Long parentId, Long studentId);
} 