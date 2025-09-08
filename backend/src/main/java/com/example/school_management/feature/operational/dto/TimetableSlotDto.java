package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.academic.dto.CourseDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class TimetableSlotDto {
    // Read-write fields
    Long id;
    @NotNull DayOfWeek dayOfWeek;
    String description;
    
    // Period info
    PeriodDto period;
    
    // Associated entities
    Long forClassId;
    CourseDto forCourse;
    TeacherDto teacher;
    RoomDto room;
    
    // For creation/update requests
    Long periodId;
    Long forCourseId;
    Long teacherId;
    Long roomId;
} 