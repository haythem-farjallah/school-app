package com.example.school_management.feature.operational.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

import java.util.Set;

@Value
public class CreateTimetableRequest {
    @NotBlank(message = "Timetable name is required")
    String name;
    
    String description;
    
    @NotBlank(message = "Academic year is required")
    String academicYear;
    
    @NotBlank(message = "Semester is required")
    String semester;
    
    Set<Long> classIds;
    Set<Long> teacherIds;
    Set<Long> roomIds;
} 