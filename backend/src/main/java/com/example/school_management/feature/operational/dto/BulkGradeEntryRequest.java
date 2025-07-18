package com.example.school_management.feature.operational.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkGradeEntryRequest {
    private Long classId;
    private Long courseId;
    private String assessmentType; // e.g., EXAM, ORAL, PROJECT
    private String term; // e.g., 2023-T1
    private List<StudentGradeEntry> grades;

    @Data
    public static class StudentGradeEntry {
        private Long studentId;
        private Double value;
        private String comment;
        private Float weight; // Optional, default to 1.0 if null
    }
} 