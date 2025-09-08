package com.example.school_management.feature.operational.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class TeacherAttendanceRequest {
    
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;
    
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Status is required")
    private TeacherAttendanceStatus status;
    
    private Long courseId;
    private String courseName;
    private Long classId;
    private String className;
    private String remarks;
    private String excuse;
    private Long substituteTeacherId;
    private String substituteTeacherName;
    
    @NotNull(message = "Recorded by ID is required")
    private Long recordedById;
    
    @NotNull(message = "Recorded by name is required")
    private String recordedByName;
    
    public enum TeacherAttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        SICK_LEAVE,
        PERSONAL_LEAVE,
        PROFESSIONAL_DEVELOPMENT,
        SUBSTITUTE_ARRANGED
    }
}
