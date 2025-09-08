package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherWorkloadAnalysis {
    
    private Long teacherId;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
    
    // Workload Metrics
    private Integer totalWeeklyHours;
    private Integer maxWeeklyCapacity;
    private Double workloadPercentage;
    private WorkloadStatus status;
    
    // Daily Breakdown
    private Map<String, DailyWorkload> dailyWorkloads; // day -> workload
    
    // Schedule Quality
    private Integer totalGaps;
    private Integer maxConsecutiveHours;
    private Double scheduleEfficiency;
    private List<String> scheduleIssues;
    
    // Course Distribution
    private List<CourseWorkload> courseWorkloads;
    private Integer totalCourses;
    private Integer totalClasses;
    
    // Room Usage
    private List<RoomUsage> roomUsages;
    private Integer totalRoomsUsed;
    
    // Recommendations
    private List<WorkloadRecommendation> recommendations;
    
    // Comparison Metrics
    private Double averageWorkloadComparison; // compared to other teachers
    private String workloadRanking; // TOP_25, AVERAGE, BOTTOM_25
    
    public enum WorkloadStatus {
        OPTIMAL,           // 80-100% capacity
        UNDERUTILIZED,     // < 80% capacity
        OVERLOADED,        // > 100% capacity
        SEVERELY_OVERLOADED // > 120% capacity
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyWorkload {
        private String dayOfWeek;
        private Integer hoursScheduled;
        private Integer gaps;
        private Integer consecutiveHours;
        private List<TimeSlot> timeSlots;
        private String efficiency; // HIGH, MEDIUM, LOW
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeSlot {
        private String period;
        private String courseName;
        private String className;
        private String roomName;
        private Boolean isConsecutive;
        private Boolean isPreferredTime;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseWorkload {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Integer weeklyHours;
        private Integer numberOfClasses;
        private List<String> classNames;
        private Double courseComplexity; // based on course difficulty
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomUsage {
        private Long roomId;
        private String roomName;
        private Integer hoursUsed;
        private Double utilizationRate;
        private Boolean isOptimalRoom; // based on course requirements
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkloadRecommendation {
        private String type; // REDUCE_LOAD, OPTIMIZE_SCHEDULE, CHANGE_ROOMS, etc.
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
        private Double expectedImprovement;
        private List<String> actionSteps;
        private Map<String, Object> parameters;
    }
}
