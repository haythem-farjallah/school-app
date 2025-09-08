package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for teacher's class view
 */
@JsonFilter("fieldFilter")
@JsonResource("teacherClass")
public record TeacherClassDto(
        Long id,
        String name,
        String grade,
        Integer capacity,
        Integer enrolled,
        String room,
        String schedule,
        Double averageGrade,
        String status,
        List<CourseInfo> courses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    
    public record CourseInfo(
            Long id,
            String name,
            String code,
            Integer weeklyHours
    ) {}
    
    public double getCapacityPercentage() {
        if (capacity == null || capacity == 0) return 0.0;
        return (enrolled != null ? enrolled : 0) * 100.0 / capacity;
    }
    
    public String getCapacityStatus() {
        double percentage = getCapacityPercentage();
        if (percentage >= 95) return "full";
        if (percentage >= 85) return "high";
        return "normal";
    }
    
    public String getGradeStatus() {
        if (averageGrade == null) return "no-data";
        if (averageGrade >= 85) return "excellent";
        if (averageGrade >= 75) return "good";
        if (averageGrade >= 65) return "average";
        return "needs-improvement";
    }
}
