package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.auth.dto.BaseUserDtoMarker;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for creating a new teaching assignment
 */
public record CreateTeachingAssignmentDto(
        @NotNull(message = "Teacher ID is required")
        Long teacherId,
        
        @NotNull(message = "Course ID is required") 
        Long courseId,
        
        @NotNull(message = "Class ID is required")
        Long classId,
        
        @PositiveOrZero(message = "Weekly hours must be zero or positive")
        Integer weeklyHours
) implements BaseUserDtoMarker {
}

