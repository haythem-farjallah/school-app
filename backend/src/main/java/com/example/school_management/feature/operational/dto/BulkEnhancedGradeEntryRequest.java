package com.example.school_management.feature.operational.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.util.List;

@Data
public class BulkEnhancedGradeEntryRequest {
    
    @NotNull(message = "Class ID is required")
    private Long classId;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Exam type is required")
    private CreateEnhancedGradeRequest.ExamType examType;
    
    @NotNull(message = "Semester is required")
    private CreateEnhancedGradeRequest.Semester semester;
    
    @NotNull(message = "Max score is required")
    @DecimalMin(value = "1.0", message = "Max score must be at least 1")
    private Double maxScore;
    
    @NotEmpty(message = "Grades list cannot be empty")
    private List<StudentGradeEntry> grades;
    
    private String teacherSignature;
    
    @Data
    public static class StudentGradeEntry {
        @NotNull(message = "Student ID is required")
        private Long studentId;
        
        @NotNull(message = "Score is required")
        @DecimalMin(value = "0.0", message = "Score must be at least 0")
        @DecimalMax(value = "20.0", message = "Score must not exceed 20")
        private Double score;
        
        private String teacherRemarks;
    }
}
