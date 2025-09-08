package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimetableOptimizationRequest {
    
    @NotNull(message = "Timetable ID is required")
    private Long timetableId;
    
    // Optimization Parameters
    @Min(value = 1, message = "Optimization time must be at least 1 second")
    @Max(value = 300, message = "Optimization time cannot exceed 300 seconds")
    private Integer optimizationTimeSeconds = 30;
    
    // Priority Weights (0.0 to 1.0)
    private Double teacherWorkloadWeight = 0.3;
    private Double roomOptimizationWeight = 0.2;
    private Double studentConvenienceWeight = 0.3;
    private Double resourceEfficiencyWeight = 0.2;
    
    // Constraints
    private List<String> hardConstraints;
    private List<String> softConstraints;
    private Map<String, Object> customConstraints;
    
    // Teacher Preferences
    private Map<Long, List<String>> teacherPreferences; // teacherId -> preferred time slots
    private Map<Long, List<String>> teacherAvoidances;  // teacherId -> avoided time slots
    
    // Class Preferences
    private Map<Long, Integer> maxDailyHoursPerClass;   // classId -> max hours per day
    private Map<Long, List<String>> preferredSubjectTimes; // courseId -> preferred times
    
    // Room Constraints
    private Map<Long, List<Long>> roomRestrictions;     // roomId -> allowed course types
    private Map<Long, Integer> roomCapacityOverrides;   // roomId -> capacity override
    
    // Advanced Options
    private Boolean enableAIOptimization = true;
    private Boolean enableWorkloadBalancing = true;
    private Boolean enableConflictResolution = true;
    private Boolean enableRoomOptimization = true;
    
    // Scenario Generation
    private Boolean generateMultipleScenarios = false;
    private Integer scenarioCount = 3;
    
    // Quality Thresholds
    private Double minimumAcceptableScore = 0.7;
    private Integer maxIterationsWithoutImprovement = 100;
}
