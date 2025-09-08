package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnhancedGradeResponse {
    
    private Long id;
    private Long studentId;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;
    private Long classId;
    private String className;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Double courseCoefficient;
    private Long teacherId;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
    private CreateEnhancedGradeRequest.ExamType examType;
    private CreateEnhancedGradeRequest.Semester semester;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private String teacherRemarks;
    private String teacherSignature;
    private LocalDateTime gradedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
