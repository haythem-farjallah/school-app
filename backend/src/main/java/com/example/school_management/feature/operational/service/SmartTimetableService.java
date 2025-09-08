package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.TimetableOptimizationRequest;
import com.example.school_management.feature.operational.dto.TimetableOptimizationResult;
import com.example.school_management.feature.operational.dto.TeacherWorkloadAnalysis;
import com.example.school_management.feature.operational.dto.TimetableConflictReport;
import com.example.school_management.feature.operational.domain.TimetableSolution;

import java.util.List;

public interface SmartTimetableService {
    
    // Smart Optimization
    TimetableOptimizationResult optimizeWithAI(TimetableOptimizationRequest request);
    TimetableOptimizationResult reoptimizeWithConstraints(Long timetableId, List<String> additionalConstraints);
    
    // Teacher Workload Management
    TeacherWorkloadAnalysis analyzeTeacherWorkload(Long teacherId);
    List<TeacherWorkloadAnalysis> analyzeAllTeacherWorkloads();
    TimetableOptimizationResult balanceTeacherWorkloads(Long timetableId);
    
    // Conflict Detection & Resolution
    TimetableConflictReport detectConflicts(Long timetableId);
    TimetableOptimizationResult resolveConflicts(Long timetableId, List<String> conflictTypes);
    
    // Room Optimization
    TimetableOptimizationResult optimizeRoomUsage(Long timetableId);
    
    // Advanced Features
    TimetableOptimizationResult generateMultipleScenarios(TimetableOptimizationRequest request, int scenarioCount);
    TimetableSolution predictOptimalSchedule(TimetableOptimizationRequest request);
    
    // Real-time Updates
    boolean validateScheduleChange(Long timetableId, Long slotId, Long newTeacherId, Long newRoomId);
    TimetableOptimizationResult applyScheduleChange(Long timetableId, Long slotId, Long newTeacherId, Long newRoomId);
}
