package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.auth.dto.BaseUserDtoMarker;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for updating an existing teaching assignment
 */
public record UpdateTeachingAssignmentDto(
        Long teacherId,
        Long courseId, 
        Long classId,
        
        @PositiveOrZero(message = "Weekly hours must be zero or positive")
        Integer weeklyHours
) implements BaseUserDtoMarker {
}

