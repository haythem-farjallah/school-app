package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffGradeReview {
    
    private Long studentId;
    private String studentFirstName;
    private String studentLastName;
    private Long classId;
    private String className;
    private CreateEnhancedGradeRequest.Semester semester;
    private List<StaffSubjectReview> subjects;
    private Double overallAverage;
    private Integer classRank;
    private Double attendanceRate;
    private Boolean isApproved;
    private String approvedAt;
    private String approvedBy;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StaffSubjectReview {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Double coefficient;
        private String teacherName;
        private SubjectGrades grades;
        private Double average;
        private String teacherRemarks;
        private Boolean needsReview;
        
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SubjectGrades {
            private Double firstExam;
            private Double secondExam;
            private Double finalExam;
        }
    }
}
