package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GradeResponse {
    
    private Long id;
    private String content;
    private Float score;
    private Float weight;
    private LocalDateTime gradedAt;
    
    // Enrollment information
    private Long enrollmentId;
    
    // Student information
    private Long studentId;
    private String studentName;
    private String studentEmail;
    
    // Class information
    private Long classId;
    private String className;
    
    // Course information
    private Long courseId;
    private String courseName;
    private String courseCode;
    
    // Teacher information
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    
    // Additional fields for advanced operations
    private String level; // From class
    private String subject; // From course
    private boolean canEdit; // Based on current user permissions
    private boolean canDelete; // Based on current user permissions
} 