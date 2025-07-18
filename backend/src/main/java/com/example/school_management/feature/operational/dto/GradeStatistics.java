package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GradeStatistics {
    
    // Basic statistics
    private Double averageGrade;
    private Float minimumGrade;
    private Float maximumGrade;
    private Long totalGrades;
    
    // Distribution
    private Long excellentCount; // 18-20
    private Long goodCount; // 14-17.99
    private Long satisfactoryCount; // 10-13.99
    private Long needsImprovementCount; // 0-9.99
    
    // Context information
    private Long studentId;
    private String studentName;
    private Long classId;
    private String className;
    private Long courseId;
    private String courseName;
    
    // Time range
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    
    // Recent grades for context
    private List<GradeResponse> recentGrades;
    
    // Trends
    private Double previousPeriodAverage;
    private String trend; // "IMPROVING", "DECLINING", "STABLE"
    
    // Additional metrics
    private Double weightedAverage;
    private String letterGrade; // A, B, C, D, F
    private String passStatus; // "PASS", "FAIL", "PENDING"
} 