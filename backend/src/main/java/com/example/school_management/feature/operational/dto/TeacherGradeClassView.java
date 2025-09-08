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
public class TeacherGradeClassView {
    
    private Long classId;
    private String className;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Double coefficient;
    private List<TeacherGradeStudent> students;
    private CreateEnhancedGradeRequest.Semester semester;
    private List<CreateEnhancedGradeRequest.ExamType> examTypes;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TeacherGradeStudent {
        private Long studentId;
        private String firstName;
        private String lastName;
        private String email;
        private Long enrollmentId;
        private CurrentGrades currentGrades;
        private Double average;
        private Double attendanceRate;
        
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class CurrentGrades {
            private ExamGrade firstExam;
            private ExamGrade secondExam;
            private ExamGrade finalExam;
            private List<ExamGrade> quizzes;
            private List<ExamGrade> assignments;
            
            @Data
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            public static class ExamGrade {
                private Double score;
                private Double maxScore;
                private Double percentage;
                private String teacherRemarks;
                private String gradedAt;
            }
        }
    }
}
