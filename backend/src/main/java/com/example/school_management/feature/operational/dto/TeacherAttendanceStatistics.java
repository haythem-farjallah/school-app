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
public class TeacherAttendanceStatistics {
    
    private Long teacherId;
    private String teacherName;
    private Integer totalDays;
    private Integer presentDays;
    private Integer absentDays;
    private Integer lateDays;
    private Integer sickLeaveDays;
    private Integer personalLeaveDays;
    private Double attendanceRate;
    private List<MonthlyBreakdown> monthlyBreakdown;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyBreakdown {
        private String month;
        private Integer present;
        private Integer absent;
        private Double rate;
    }
}
