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
    
    private Long periodId;
    private Integer periodIndex;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    
    private Long forClassId;
    private String forClassName;
    
    private Long forCourseId;
    private String forCourseName;
    private String forCourseCode;
    private String forCourseColor;
    
    private Long teacherId;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
    
    private Long roomId;
    private String roomName;
    private Integer roomCapacity;
    
    // Student count for the class
    private Integer studentCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

