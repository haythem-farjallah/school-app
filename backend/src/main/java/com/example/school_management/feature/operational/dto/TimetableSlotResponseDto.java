package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TimetableSlotResponseDto {
    private Long id;
    private DayOfWeek dayOfWeek;
    private String description;
    
    // Period info (flattened)
    private Long periodId;
    private Integer periodIndex;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    
    // Class info (flattened)
    private Long forClassId;
    private String forClassName;
    
    // Course info (flattened)
    private Long forCourseId;
    private String forCourseName;
    private String forCourseCode;
    private String forCourseColor;
    
    // Teacher info (flattened)
    private Long teacherId;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
    
    // Room info (flattened)
    private Long roomId;
    private String roomName;
    private Integer roomCapacity;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

