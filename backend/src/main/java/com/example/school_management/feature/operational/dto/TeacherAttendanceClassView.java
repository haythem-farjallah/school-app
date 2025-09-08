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
public class TeacherAttendanceClassView {
    
    private Long classId;
    private String className;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Double coefficient;
    private List<TeacherAttendanceStudent> students;
    private String semester;
    private List<String> attendanceTypes;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TeacherAttendanceStudent {
        private Long studentId;
        private String firstName;
        private String lastName;
        private String email;
        private Long enrollmentId;
        private AttendanceStatus currentStatus;
        private Double attendanceRate;
        private String lastAttendanceDate;
    }
}
