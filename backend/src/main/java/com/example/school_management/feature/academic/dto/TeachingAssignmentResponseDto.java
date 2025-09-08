package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

import java.time.LocalDateTime;

/**
 * DTO for teaching assignment responses
 */
@JsonFilter("fieldFilter")
@JsonResource("teachingAssignment")
public record TeachingAssignmentResponseDto(
        Long id,
        Long teacherId,
        String teacherFirstName,
        String teacherLastName,
        String teacherEmail,
        Long courseId,
        String courseName,
        String courseCode,
        Long classId,
        String className,
        Integer weeklyHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    
    public String getTeacherFullName() {
        return teacherFirstName + " " + teacherLastName;
    }
    
    public String getCourseDisplay() {
        return courseCode + " - " + courseName;
    }
    
    public String getAssignmentSummary() {
        return getTeacherFullName() + " teaches " + getCourseDisplay() + " to " + className;
    }
}

