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
public class TimetableConflictReport {
    
    private Long timetableId;
    private String timetableName;
    private LocalDateTime analysisDate;
    private ConflictSeverity overallSeverity;
    
    // Conflict Summary
    private Integer totalConflicts;
    private Integer criticalConflicts;
    private Integer majorConflicts;
    private Integer minorConflicts;
    
    // Conflict Categories
    private List<TeacherConflict> teacherConflicts;
    private List<RoomConflict> roomConflicts;
    private List<ClassConflict> classConflicts;
    private List<CapacityConflict> capacityConflicts;
    private List<PreferenceViolation> preferenceViolations;
    
    // Impact Analysis
    private ConflictImpactAnalysis impactAnalysis;
    
    // Resolution Suggestions
    private List<ConflictResolutionSuggestion> resolutionSuggestions;
    
    // Statistics
    private Map<String, Integer> conflictTypeStatistics;
    private Map<String, Double> conflictSeverityDistribution;
    
    public enum ConflictSeverity {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TeacherConflict {
        private Long teacherId;
        private String teacherName;
        private String conflictType; // DOUBLE_BOOKING, OVERLOAD, PREFERENCE_VIOLATION
        private String description;
        private List<ConflictSlot> conflictingSlots;
        private ConflictSeverity severity;
        private String impact;
        private List<String> resolutionOptions;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomConflict {
        private Long roomId;
        private String roomName;
        private String conflictType; // DOUBLE_BOOKING, CAPACITY_EXCEEDED, EQUIPMENT_MISMATCH
        private String description;
        private List<ConflictSlot> conflictingSlots;
        private ConflictSeverity severity;
        private String impact;
        private List<String> resolutionOptions;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClassConflict {
        private Long classId;
        private String className;
        private String conflictType; // DOUBLE_BOOKING, EXCESSIVE_HOURS, INSUFFICIENT_BREAKS
        private String description;
        private List<ConflictSlot> conflictingSlots;
        private ConflictSeverity severity;
        private String impact;
        private List<String> resolutionOptions;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CapacityConflict {
        private Long roomId;
        private String roomName;
        private Long classId;
        private String className;
        private Integer roomCapacity;
        private Integer classSize;
        private Integer capacityDeficit;
        private ConflictSeverity severity;
        private List<String> resolutionOptions;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PreferenceViolation {
        private String violationType; // TEACHER_PREFERENCE, TIME_PREFERENCE, ROOM_PREFERENCE
        private String description;
        private Long affectedEntityId; // teacher, class, or course ID
        private String affectedEntityName;
        private String preferenceDetails;
        private ConflictSeverity severity;
        private Double impactScore;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConflictSlot {
        private Long slotId;
        private String dayOfWeek;
        private String period;
        private String courseName;
        private String className;
        private String teacherName;
        private String roomName;
        private String conflictReason;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConflictImpactAnalysis {
        private Integer affectedTeachers;
        private Integer affectedClasses;
        private Integer affectedRooms;
        private Integer affectedStudents; // estimated
        private Double scheduleQualityScore;
        private Double teacherSatisfactionScore;
        private Double studentSatisfactionScore;
        private List<String> majorImpacts;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConflictResolutionSuggestion {
        private String suggestionId;
        private String title;
        private String description;
        private String resolutionType; // AUTOMATIC, MANUAL, HYBRID
        private ConflictSeverity targetSeverity;
        private Double expectedImprovement;
        private Integer estimatedTimeMinutes;
        private List<String> steps;
        private Map<String, Object> parameters;
        private List<String> prerequisites;
        private List<String> consequences;
    }
}
