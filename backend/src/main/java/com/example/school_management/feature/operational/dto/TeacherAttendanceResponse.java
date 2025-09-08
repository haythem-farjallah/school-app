package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAttendanceResponse {
    
    private Long id;
    private Long teacherId;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
    private LocalDate date;
    private TeacherAttendanceRequest.TeacherAttendanceStatus status;
    private Long courseId;
    private String courseName;
    private Long classId;
    private String className;
    private String remarks;
    private String excuse;
    private Long substituteTeacherId;
    private String substituteTeacherName;
    private Long recordedById;
    private String recordedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
