package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.service.SmartTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/smart-timetable")
@RequiredArgsConstructor
@Tag(name = "Smart Timetable Management", description = "AI-powered timetable optimization and management")
@ConditionalOnProperty(name = "optaplanner.enabled", havingValue = "true", matchIfMissing = false)
public class SmartTimetableController {
    
    private final SmartTimetableService smartTimetableService;
    
    // ===== AI OPTIMIZATION =====
    
    @PostMapping("/optimize")
    @Operation(summary = "Optimize timetable with AI")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> optimizeWithAI(
            @Valid @RequestBody TimetableOptimizationRequest request) {
        log.debug("Starting AI optimization for timetable: {}", request.getTimetableId());
        
        TimetableOptimizationResult result = smartTimetableService.optimizeWithAI(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Timetable optimization completed successfully", result));
    }
    
    @PostMapping("/{timetableId}/reoptimize")
    @Operation(summary = "Re-optimize timetable with additional constraints")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> reoptimizeWithConstraints(
            @PathVariable Long timetableId,
            @RequestBody List<String> additionalConstraints) {
        log.debug("Re-optimizing timetable: {} with constraints: {}", timetableId, additionalConstraints);
        
        TimetableOptimizationResult result = smartTimetableService.reoptimizeWithConstraints(timetableId, additionalConstraints);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable re-optimization completed successfully", result));
    }
    
    // ===== TEACHER WORKLOAD MANAGEMENT =====
    
    @GetMapping("/workload/teacher/{teacherId}")
    @Operation(summary = "Analyze teacher workload")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<TeacherWorkloadAnalysis>> analyzeTeacherWorkload(
            @PathVariable Long teacherId) {
        log.debug("Analyzing workload for teacher: {}", teacherId);
        
        TeacherWorkloadAnalysis analysis = smartTimetableService.analyzeTeacherWorkload(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher workload analysis completed", analysis));
    }
    
    @GetMapping("/workload/all")
    @Operation(summary = "Analyze all teacher workloads")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<TeacherWorkloadAnalysis>>> analyzeAllTeacherWorkloads() {
        log.debug("Analyzing workloads for all teachers");
        
        List<TeacherWorkloadAnalysis> analyses = smartTimetableService.analyzeAllTeacherWorkloads();
        return ResponseEntity.ok(new ApiSuccessResponse<>("All teacher workload analyses completed", analyses));
    }
    
    @PostMapping("/{timetableId}/balance-workloads")
    @Operation(summary = "Balance teacher workloads")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> balanceTeacherWorkloads(
            @PathVariable Long timetableId) {
        log.debug("Balancing teacher workloads for timetable: {}", timetableId);
        
        TimetableOptimizationResult result = smartTimetableService.balanceTeacherWorkloads(timetableId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher workloads balanced successfully", result));
    }
    
    // ===== CONFLICT DETECTION & RESOLUTION =====
    
    @GetMapping("/{timetableId}/conflicts")
    @Operation(summary = "Detect timetable conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<TimetableConflictReport>> detectConflicts(
            @PathVariable Long timetableId) {
        log.debug("Detecting conflicts for timetable: {}", timetableId);
        
        TimetableConflictReport report = smartTimetableService.detectConflicts(timetableId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Conflict detection completed", report));
    }
    
    @PostMapping("/{timetableId}/resolve-conflicts")
    @Operation(summary = "Resolve timetable conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> resolveConflicts(
            @PathVariable Long timetableId,
            @RequestBody List<String> conflictTypes) {
        log.debug("Resolving conflicts for timetable: {}, types: {}", timetableId, conflictTypes);
        
        TimetableOptimizationResult result = smartTimetableService.resolveConflicts(timetableId, conflictTypes);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Conflicts resolved successfully", result));
    }
    
    // ===== ROOM OPTIMIZATION =====
    
    @PostMapping("/{timetableId}/optimize-rooms")
    @Operation(summary = "Optimize room usage")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> optimizeRoomUsage(
            @PathVariable Long timetableId) {
        log.debug("Optimizing room usage for timetable: {}", timetableId);
        
        TimetableOptimizationResult result = smartTimetableService.optimizeRoomUsage(timetableId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Room optimization completed successfully", result));
    }
    
    // ===== ADVANCED FEATURES =====
    
    @PostMapping("/scenarios")
    @Operation(summary = "Generate multiple optimization scenarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> generateMultipleScenarios(
            @Valid @RequestBody TimetableOptimizationRequest request,
            @RequestParam(defaultValue = "3") int scenarioCount) {
        log.debug("Generating {} scenarios for timetable: {}", scenarioCount, request.getTimetableId());
        
        TimetableOptimizationResult result = smartTimetableService.generateMultipleScenarios(request, scenarioCount);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Multiple scenarios generated successfully", result));
    }
    
    @PostMapping("/predict")
    @Operation(summary = "Predict optimal schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Object>> predictOptimalSchedule(
            @Valid @RequestBody TimetableOptimizationRequest request) {
        log.debug("Predicting optimal schedule for timetable: {}", request.getTimetableId());
        
        var prediction = smartTimetableService.predictOptimalSchedule(request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Schedule prediction completed", prediction));
    }
    
    // ===== REAL-TIME UPDATES =====
    
    @PostMapping("/{timetableId}/validate-change")
    @Operation(summary = "Validate schedule change")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> validateScheduleChange(
            @PathVariable Long timetableId,
            @RequestParam Long slotId,
            @RequestParam(required = false) Long newTeacherId,
            @RequestParam(required = false) Long newRoomId) {
        log.debug("Validating schedule change for slot: {}", slotId);
        
        boolean isValid = smartTimetableService.validateScheduleChange(timetableId, slotId, newTeacherId, newRoomId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Schedule change validation completed", isValid));
    }
    
    @PostMapping("/{timetableId}/apply-change")
    @Operation(summary = "Apply schedule change")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableOptimizationResult>> applyScheduleChange(
            @PathVariable Long timetableId,
            @RequestParam Long slotId,
            @RequestParam(required = false) Long newTeacherId,
            @RequestParam(required = false) Long newRoomId) {
        log.debug("Applying schedule change for slot: {}", slotId);
        
        TimetableOptimizationResult result = smartTimetableService.applyScheduleChange(timetableId, slotId, newTeacherId, newRoomId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Schedule change applied successfully", result));
    }
    
    // ===== ANALYTICS & INSIGHTS =====
    
    @GetMapping("/{timetableId}/analytics")
    @Operation(summary = "Get timetable analytics and insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<Object>> getTimetableAnalytics(@PathVariable Long timetableId) {
        log.debug("Getting analytics for timetable: {}", timetableId);
        
        // Combine multiple analyses for comprehensive insights
        TimetableConflictReport conflicts = smartTimetableService.detectConflicts(timetableId);
        List<TeacherWorkloadAnalysis> workloads = smartTimetableService.analyzeAllTeacherWorkloads();
        
        Map<String, Object> analytics = Map.of(
            "conflictReport", conflicts,
            "workloadAnalyses", workloads,
            "overallHealth", conflicts.getTotalConflicts() == 0 ? "EXCELLENT" : 
                           conflicts.getTotalConflicts() < 5 ? "GOOD" : "NEEDS_ATTENTION"
        );
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable analytics retrieved successfully", analytics));
    }
}
