package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.controller.TeacherClassController.TeacherClassStatsDto;
import com.example.school_management.feature.academic.dto.TeacherClassDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeacherClassService {
    
    /**
     * Get teacher's assigned classes with pagination
     */
    Page<TeacherClassDto> getTeacherClasses(String teacherEmail, Pageable pageable, String search);
    
    /**
     * Get all teacher's assigned classes without pagination
     */
    List<TeacherClassDto> getAllTeacherClasses(String teacherEmail, String search);
    
    /**
     * Get teacher's class statistics
     */
    TeacherClassStatsDto getTeacherClassStats(String teacherEmail);
    
    /**
     * Get debug information for teacher
     */
    Object getDebugInfo(String teacherEmail);
}
