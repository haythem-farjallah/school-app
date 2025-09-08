package com.example.school_management.feature.operational.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

@Data
public class CreateEnhancedGradeRequest {
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Class ID is required")
    private Long classId;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Exam type is required")
    private ExamType examType;
    
    @NotNull(message = "Semester is required")
    private Semester semester;
    
    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be at least 0")
    @DecimalMax(value = "20.0", message = "Score must not exceed 20")
    private Double score;
    
    @NotNull(message = "Max score is required")
    @DecimalMin(value = "1.0", message = "Max score must be at least 1")
    private Double maxScore;
    
    private String teacherRemarks;
    
    private String teacherSignature;
    
    public enum ExamType {
        FIRST_EXAM,
        SECOND_EXAM,
        FINAL_EXAM,
        QUIZ,
        ASSIGNMENT,
        PROJECT,
        PARTICIPATION
    }
    
    public enum Semester {
        FIRST,
        SECOND,
        THIRD,
        FINAL
    }
}
