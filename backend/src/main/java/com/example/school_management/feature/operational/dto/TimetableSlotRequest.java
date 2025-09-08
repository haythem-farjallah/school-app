package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimetableSlotRequest {
    
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;
    
    @NotNull(message = "Period ID is required")
    private Long periodId;
    
    private Long forClassId;
    
    private Long forCourseId;
    
    private Long teacherId;
    
    private Long roomId;
    
    private String description;
}
