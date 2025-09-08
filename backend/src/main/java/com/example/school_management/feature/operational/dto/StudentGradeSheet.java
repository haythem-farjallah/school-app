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
public class StudentGradeSheet {
    
    private Long studentId;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;
    private Long classId;
    private String className;
    private Integer yearOfStudy;
    private CreateEnhancedGradeRequest.Semester semester;
    private List<SubjectGrade> subjects;
    private Double totalScore;
    private Double totalMaxScore;
    private Double weightedAverage;
    private Integer classRank;
    private Integer totalStudents;
    private Double attendanceRate;
    private Integer totalAbsences;
    private String generatedAt;
    private ApprovedBy approvedBy;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubjectGrade {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Double coefficient;
        private Long teacherId;
        private String teacherFirstName;
        private String teacherLastName;
        private SubjectGrades grades;
        private Double average;
        private Double weightedScore;
        private String teacherRemarks;
        private String teacherSignature;
        private String letterGrade;
        
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SubjectGrades {
            private Double firstExam;
            private Double secondExam;
            private Double finalExam;
            private List<Double> quizzes;
            private List<Double> assignments;
        }
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApprovedBy {
        private Long staffId;
        private String staffName;
        private String approvedAt;
    }
}
