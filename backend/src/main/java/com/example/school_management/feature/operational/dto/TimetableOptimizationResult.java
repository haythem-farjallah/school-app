package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimetableOptimizationResult {
    
    private Long timetableId;
    private String optimizationId;
    private OptimizationStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    
    // Scoring
    private Double finalScore;
    private Double hardConstraintScore;
    private Double softConstraintScore;
    private Double improvementPercentage;
    
    // Quality Metrics
    private QualityMetrics qualityMetrics;
    
    // Conflicts & Issues
    private List<ConflictInfo> resolvedConflicts;
    private List<ConflictInfo> remainingConflicts;
    private List<String> warnings;
    
    // Teacher Analysis
    private List<TeacherWorkloadSummary> teacherWorkloads;
    private Double averageTeacherWorkload;
    private Double workloadStandardDeviation;
    
    // Room Analysis
    private List<RoomUtilizationSummary> roomUtilizations;
    private Double averageRoomUtilization;
    
    // Student Analysis
    private Map<Long, StudentScheduleAnalysis> studentAnalysis; // classId -> analysis
    
    // Alternative Scenarios (if requested)
    private List<TimetableScenario> alternativeScenarios;
    
    // Recommendations
    private List<OptimizationRecommendation> recommendations;
    
    public enum OptimizationStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        PARTIALLY_COMPLETED
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QualityMetrics {
        private Double teacherSatisfaction;
        private Double studentSatisfaction;
        private Double resourceEfficiency;
        private Double scheduleCompactness;
        private Double conflictResolutionRate;
        private Integer totalViolations;
        private Integer hardViolations;
        private Integer softViolations;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConflictInfo {
        private String conflictType;
        private String description;
        private List<Long> affectedSlotIds;
        private List<Long> affectedTeacherIds;
        private List<Long> affectedClassIds;
        private List<Long> affectedRoomIds;
        private String severity; // HIGH, MEDIUM, LOW
        private String resolution;
        private Boolean resolved;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TeacherWorkloadSummary {
        private Long teacherId;
        private String teacherName;
        private Integer totalHours;
        private Integer maxCapacity;
        private Double utilizationRate;
        private Integer gapHours;
        private Integer consecutiveHours;
        private List<String> workloadIssues;
        private String workloadRating; // OPTIMAL, OVERLOADED, UNDERUTILIZED
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomUtilizationSummary {
        private Long roomId;
        private String roomName;
        private Integer capacity;
        private Integer totalHoursUsed;
        private Integer maxPossibleHours;
        private Double utilizationRate;
        private List<String> utilizationIssues;
        private String utilizationRating; // OPTIMAL, OVERUSED, UNDERUSED
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StudentScheduleAnalysis {
        private Long classId;
        private String className;
        private Integer totalHours;
        private Integer maxConsecutiveHours;
        private Integer totalGaps;
        private Double scheduleCompactness;
        private List<String> scheduleIssues;
        private String scheduleRating; // EXCELLENT, GOOD, NEEDS_IMPROVEMENT
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimetableScenario {
        private String scenarioId;
        private String scenarioName;
        private Double score;
        private String description;
        private Map<String, Object> characteristics;
        private List<String> advantages;
        private List<String> disadvantages;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptimizationRecommendation {
        private String type; // TEACHER_ADJUSTMENT, ROOM_CHANGE, SCHEDULE_MODIFICATION
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
        private Double expectedImprovement;
        private List<String> actionSteps;
        private Map<String, Object> parameters;
    }
}
