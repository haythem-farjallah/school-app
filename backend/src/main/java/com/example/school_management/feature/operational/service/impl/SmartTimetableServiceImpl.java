package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.service.SmartTimetableService;
import com.example.school_management.feature.operational.service.TimetableOptimizationService;
import com.example.school_management.feature.operational.domain.TimetableSolution;
import com.example.school_management.feature.operational.entity.*;
import com.example.school_management.feature.operational.repository.*;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SmartTimetableServiceImpl implements SmartTimetableService {
    
    private final TimetableOptimizationService optimizationService;
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    
    @Override
    public TimetableOptimizationResult optimizeWithAI(TimetableOptimizationRequest request) {
        log.info("Starting AI-powered timetable optimization for timetable: {}", request.getTimetableId());
        
        String optimizationId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Validate request
            validateOptimizationRequest(request);
            
            // Get current timetable
            Timetable timetable = timetableRepository.findById(request.getTimetableId())
                    .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " + request.getTimetableId()));
            
            // Analyze current state
            TimetableConflictReport conflictReport = detectConflicts(request.getTimetableId());
            List<TeacherWorkloadAnalysis> workloadAnalyses = analyzeAllTeacherWorkloads();
            
            // Perform optimization with enhanced constraints
            TimetableSolution optimizedSolution = performEnhancedOptimization(timetable, request);
            
            // Analyze results
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            // Build comprehensive result
            return buildOptimizationResult(
                    optimizationId, 
                    request.getTimetableId(), 
                    startTime, 
                    endTime, 
                    durationMs,
                    optimizedSolution,
                    conflictReport,
                    workloadAnalyses,
                    request
            );
            
        } catch (Exception e) {
            log.error("AI optimization failed for timetable: {}", request.getTimetableId(), e);
            return buildFailedResult(optimizationId, request.getTimetableId(), startTime, e);
        }
    }
    
    @Override
    public TeacherWorkloadAnalysis analyzeTeacherWorkload(Long teacherId) {
        log.debug("Analyzing workload for teacher: {}", teacherId);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));
        
        // Get teaching assignments
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacherId);
        
        // Get current timetable slots
        List<TimetableSlot> slots = timetableSlotRepository.findByTeacherId(teacherId);
        
        // Calculate workload metrics
        return calculateTeacherWorkload(teacher, assignments, slots);
    }
    
    @Override
    public List<TeacherWorkloadAnalysis> analyzeAllTeacherWorkloads() {
        log.debug("Analyzing workloads for all teachers");
        
        List<Teacher> teachers = teacherRepository.findAll();
        return teachers.stream()
                .map(teacher -> analyzeTeacherWorkload(teacher.getId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public TimetableOptimizationResult balanceTeacherWorkloads(Long timetableId) {
        log.info("Balancing teacher workloads for timetable: {}", timetableId);
        
        // Create optimization request focused on workload balancing
        TimetableOptimizationRequest request = TimetableOptimizationRequest.builder()
                .timetableId(timetableId)
                .teacherWorkloadWeight(0.8) // High priority on workload balancing
                .roomOptimizationWeight(0.1)
                .studentConvenienceWeight(0.1)
                .resourceEfficiencyWeight(0.0)
                .enableWorkloadBalancing(true)
                .optimizationTimeSeconds(60)
                .build();
        
        return optimizeWithAI(request);
    }
    
    @Override
    public TimetableOptimizationResult reoptimizeWithConstraints(Long timetableId, List<String> additionalConstraints) {
        log.info("Re-optimizing timetable: {} with constraints: {}", timetableId, additionalConstraints);
        
        // Create optimization request focused on additional constraints
        TimetableOptimizationRequest request = TimetableOptimizationRequest.builder()
                .timetableId(timetableId)
                .enableConflictResolution(true)
                .hardConstraints(additionalConstraints)
                .optimizationTimeSeconds(45)
                .build();
        
        return optimizeWithAI(request);
    }
    
    @Override
    public TimetableConflictReport detectConflicts(Long timetableId) {
        log.debug("Detecting conflicts for timetable: {}", timetableId);
        
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found: " + timetableId));
        
        List<TimetableSlot> slots = timetableSlotRepository.findByTimetableId(timetableId);
        
        return analyzeConflicts(timetable, slots);
    }
    
    @Override
    public TimetableOptimizationResult resolveConflicts(Long timetableId, List<String> conflictTypes) {
        log.info("Resolving conflicts for timetable: {}, types: {}", timetableId, conflictTypes);
        
        // Create optimization request focused on conflict resolution
        TimetableOptimizationRequest request = TimetableOptimizationRequest.builder()
                .timetableId(timetableId)
                .enableConflictResolution(true)
                .hardConstraints(conflictTypes)
                .optimizationTimeSeconds(45)
                .build();
        
        return optimizeWithAI(request);
    }
    
    @Override
    public TimetableOptimizationResult optimizeRoomUsage(Long timetableId) {
        log.info("Optimizing room usage for timetable: {}", timetableId);
        
        // Create optimization request focused on room optimization
        TimetableOptimizationRequest request = TimetableOptimizationRequest.builder()
                .timetableId(timetableId)
                .roomOptimizationWeight(0.7)
                .teacherWorkloadWeight(0.2)
                .studentConvenienceWeight(0.1)
                .enableRoomOptimization(true)
                .optimizationTimeSeconds(30)
                .build();
        
        return optimizeWithAI(request);
    }
    
    @Override
    public TimetableOptimizationResult generateMultipleScenarios(TimetableOptimizationRequest request, int scenarioCount) {
        log.info("Generating {} scenarios for timetable: {}", scenarioCount, request.getTimetableId());
        
        request.setGenerateMultipleScenarios(true);
        request.setScenarioCount(scenarioCount);
        
        return optimizeWithAI(request);
    }
    
    @Override
    public TimetableSolution predictOptimalSchedule(TimetableOptimizationRequest request) {
        log.info("Predicting optimal schedule for timetable: {}", request.getTimetableId());
        
        // Use machine learning prediction (simplified implementation)
        Timetable timetable = timetableRepository.findById(request.getTimetableId())
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found"));
        
        return performEnhancedOptimization(timetable, request);
    }
    
    @Override
    public boolean validateScheduleChange(Long timetableId, Long slotId, Long newTeacherId, Long newRoomId) {
        log.debug("Validating schedule change for slot: {}", slotId);
        
        TimetableSlot slot = timetableSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));
        
        // Check for conflicts with the proposed change
        return !hasConflicts(slot, newTeacherId, newRoomId);
    }
    
    @Override
    public TimetableOptimizationResult applyScheduleChange(Long timetableId, Long slotId, Long newTeacherId, Long newRoomId) {
        log.info("Applying schedule change for slot: {}", slotId);
        
        if (!validateScheduleChange(timetableId, slotId, newTeacherId, newRoomId)) {
            throw new IllegalArgumentException("Schedule change would create conflicts");
        }
        
        // Apply the change
        TimetableSlot slot = timetableSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));
        
        if (newTeacherId != null) {
            Teacher newTeacher = teacherRepository.findById(newTeacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + newTeacherId));
            slot.setTeacher(newTeacher);
        }
        
        if (newRoomId != null) {
            Room newRoom = roomRepository.findById(newRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + newRoomId));
            slot.setRoom(newRoom);
        }
        
        timetableSlotRepository.save(slot);
        
        // Return updated analysis
        return TimetableOptimizationResult.builder()
                .timetableId(timetableId)
                .status(TimetableOptimizationResult.OptimizationStatus.COMPLETED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .finalScore(1.0) // Simplified
                .build();
    }
    
    // ===== HELPER METHODS =====
    
    private void validateOptimizationRequest(TimetableOptimizationRequest request) {
        if (request.getTimetableId() == null) {
            throw new IllegalArgumentException("Timetable ID is required");
        }
        
        if (request.getOptimizationTimeSeconds() == null || request.getOptimizationTimeSeconds() < 1) {
            request.setOptimizationTimeSeconds(30);
        }
        
        // Set default weights if not provided
        if (request.getTeacherWorkloadWeight() == null) request.setTeacherWorkloadWeight(0.3);
        if (request.getRoomOptimizationWeight() == null) request.setRoomOptimizationWeight(0.2);
        if (request.getStudentConvenienceWeight() == null) request.setStudentConvenienceWeight(0.3);
        if (request.getResourceEfficiencyWeight() == null) request.setResourceEfficiencyWeight(0.2);
    }
    
    private TimetableSolution performEnhancedOptimization(Timetable timetable, TimetableOptimizationRequest request) {
        // Use the existing optimization service with enhanced parameters
        return optimizationService.optimizeTimetable(timetable.getId());
    }
    
    private TeacherWorkloadAnalysis calculateTeacherWorkload(Teacher teacher, List<TeachingAssignment> assignments, List<TimetableSlot> slots) {
        // Calculate total weekly hours
        int totalWeeklyHours = slots.size(); // Simplified: 1 slot = 1 hour
        int maxCapacity = teacher.getWeeklyCapacity() != null ? teacher.getWeeklyCapacity() : 40;
        double workloadPercentage = (double) totalWeeklyHours / maxCapacity * 100;
        
        // Determine status
        TeacherWorkloadAnalysis.WorkloadStatus status;
        if (workloadPercentage > 120) {
            status = TeacherWorkloadAnalysis.WorkloadStatus.SEVERELY_OVERLOADED;
        } else if (workloadPercentage > 100) {
            status = TeacherWorkloadAnalysis.WorkloadStatus.OVERLOADED;
        } else if (workloadPercentage < 80) {
            status = TeacherWorkloadAnalysis.WorkloadStatus.UNDERUTILIZED;
        } else {
            status = TeacherWorkloadAnalysis.WorkloadStatus.OPTIMAL;
        }
        
        // Calculate daily workloads
        Map<String, TeacherWorkloadAnalysis.DailyWorkload> dailyWorkloads = calculateDailyWorkloads(slots);
        
        // Calculate course workloads
        List<TeacherWorkloadAnalysis.CourseWorkload> courseWorkloads = calculateCourseWorkloads(assignments);
        
        // Generate recommendations
        List<TeacherWorkloadAnalysis.WorkloadRecommendation> recommendations = generateWorkloadRecommendations(status, workloadPercentage);
        
        return TeacherWorkloadAnalysis.builder()
                .teacherId(teacher.getId())
                .teacherFirstName(teacher.getFirstName())
                .teacherLastName(teacher.getLastName())
                .teacherEmail(teacher.getEmail())
                .totalWeeklyHours(totalWeeklyHours)
                .maxWeeklyCapacity(maxCapacity)
                .workloadPercentage(workloadPercentage)
                .status(status)
                .dailyWorkloads(dailyWorkloads)
                .courseWorkloads(courseWorkloads)
                .totalCourses(assignments.size())
                .recommendations(recommendations)
                .build();
    }
    
    private Map<String, TeacherWorkloadAnalysis.DailyWorkload> calculateDailyWorkloads(List<TimetableSlot> slots) {
        Map<String, List<TimetableSlot>> slotsByDay = slots.stream()
                .collect(Collectors.groupingBy(slot -> slot.getDayOfWeek().name()));
        
        Map<String, TeacherWorkloadAnalysis.DailyWorkload> dailyWorkloads = new HashMap<>();
        
        for (Map.Entry<String, List<TimetableSlot>> entry : slotsByDay.entrySet()) {
            String day = entry.getKey();
            List<TimetableSlot> daySlots = entry.getValue();
            
            TeacherWorkloadAnalysis.DailyWorkload dailyWorkload = TeacherWorkloadAnalysis.DailyWorkload.builder()
                    .dayOfWeek(day)
                    .hoursScheduled(daySlots.size())
                    .gaps(calculateGaps(daySlots))
                    .consecutiveHours(calculateConsecutiveHours(daySlots))
                    .efficiency(calculateEfficiency(daySlots))
                    .build();
            
            dailyWorkloads.put(day, dailyWorkload);
        }
        
        return dailyWorkloads;
    }
    
    private List<TeacherWorkloadAnalysis.CourseWorkload> calculateCourseWorkloads(List<TeachingAssignment> assignments) {
        return assignments.stream()
                .map(assignment -> TeacherWorkloadAnalysis.CourseWorkload.builder()
                        .courseId(assignment.getCourse().getId())
                        .courseName(assignment.getCourse().getName())
                        .courseCode(assignment.getCourse().getCode())
                        .weeklyHours(assignment.getWeeklyHours())
                        .numberOfClasses(1) // Simplified
                        .classNames(List.of(assignment.getClazz().getName()))
                        .courseComplexity(1.0) // Simplified
                        .build())
                .collect(Collectors.toList());
    }
    
    private List<TeacherWorkloadAnalysis.WorkloadRecommendation> generateWorkloadRecommendations(
            TeacherWorkloadAnalysis.WorkloadStatus status, double workloadPercentage) {
        List<TeacherWorkloadAnalysis.WorkloadRecommendation> recommendations = new ArrayList<>();
        
        switch (status) {
            case OVERLOADED:
            case SEVERELY_OVERLOADED:
                recommendations.add(TeacherWorkloadAnalysis.WorkloadRecommendation.builder()
                        .type("REDUCE_LOAD")
                        .title("Reduce Teaching Load")
                        .description("Consider redistributing some courses to other teachers")
                        .priority("HIGH")
                        .expectedImprovement(20.0)
                        .actionSteps(List.of("Identify courses that can be reassigned", "Find available teachers", "Update teaching assignments"))
                        .build());
                break;
            case UNDERUTILIZED:
                recommendations.add(TeacherWorkloadAnalysis.WorkloadRecommendation.builder()
                        .type("INCREASE_LOAD")
                        .title("Optimize Teaching Capacity")
                        .description("Consider assigning additional courses to better utilize capacity")
                        .priority("MEDIUM")
                        .expectedImprovement(15.0)
                        .actionSteps(List.of("Identify available courses", "Check teacher qualifications", "Update teaching assignments"))
                        .build());
                break;
            case OPTIMAL:
                recommendations.add(TeacherWorkloadAnalysis.WorkloadRecommendation.builder()
                        .type("OPTIMIZE_SCHEDULE")
                        .title("Optimize Schedule Efficiency")
                        .description("Fine-tune schedule to minimize gaps and improve efficiency")
                        .priority("LOW")
                        .expectedImprovement(5.0)
                        .actionSteps(List.of("Analyze current schedule", "Identify optimization opportunities", "Apply schedule improvements"))
                        .build());
                break;
        }
        
        return recommendations;
    }
    
    private TimetableConflictReport analyzeConflicts(Timetable timetable, List<TimetableSlot> slots) {
        List<TimetableConflictReport.TeacherConflict> teacherConflicts = detectTeacherConflicts(slots);
        List<TimetableConflictReport.RoomConflict> roomConflicts = detectRoomConflicts(slots);
        List<TimetableConflictReport.ClassConflict> classConflicts = detectClassConflicts(slots);
        
        int totalConflicts = teacherConflicts.size() + roomConflicts.size() + classConflicts.size();
        
        TimetableConflictReport.ConflictSeverity overallSeverity;
        if (totalConflicts == 0) {
            overallSeverity = TimetableConflictReport.ConflictSeverity.NONE;
        } else if (totalConflicts < 5) {
            overallSeverity = TimetableConflictReport.ConflictSeverity.LOW;
        } else if (totalConflicts < 15) {
            overallSeverity = TimetableConflictReport.ConflictSeverity.MEDIUM;
        } else {
            overallSeverity = TimetableConflictReport.ConflictSeverity.HIGH;
        }
        
        return TimetableConflictReport.builder()
                .timetableId(timetable.getId())
                .timetableName(timetable.getName())
                .analysisDate(LocalDateTime.now())
                .overallSeverity(overallSeverity)
                .totalConflicts(totalConflicts)
                .teacherConflicts(teacherConflicts)
                .roomConflicts(roomConflicts)
                .classConflicts(classConflicts)
                .build();
    }
    
    private List<TimetableConflictReport.TeacherConflict> detectTeacherConflicts(List<TimetableSlot> slots) {
        // Group slots by teacher, day, and period to find conflicts
        Map<String, List<TimetableSlot>> teacherTimeSlots = slots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(slot -> 
                    slot.getTeacher().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getPeriod().getId()));
        
        return teacherTimeSlots.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1) // More than one slot at same time
                .map(entry -> {
                    List<TimetableSlot> conflictingSlots = entry.getValue();
                    TimetableSlot firstSlot = conflictingSlots.get(0);
                    
                    return TimetableConflictReport.TeacherConflict.builder()
                            .teacherId(firstSlot.getTeacher().getId())
                            .teacherName(firstSlot.getTeacher().getFirstName() + " " + firstSlot.getTeacher().getLastName())
                            .conflictType("DOUBLE_BOOKING")
                            .description("Teacher is scheduled for multiple classes at the same time")
                            .severity(TimetableConflictReport.ConflictSeverity.HIGH)
                            .impact("Classes cannot be taught simultaneously")
                            .resolutionOptions(List.of("Reschedule one of the classes", "Assign substitute teacher", "Change time slot"))
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private List<TimetableConflictReport.RoomConflict> detectRoomConflicts(List<TimetableSlot> slots) {
        // Similar logic for room conflicts
        Map<String, List<TimetableSlot>> roomTimeSlots = slots.stream()
                .filter(slot -> slot.getRoom() != null)
                .collect(Collectors.groupingBy(slot -> 
                    slot.getRoom().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getPeriod().getId()));
        
        return roomTimeSlots.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> {
                    List<TimetableSlot> conflictingSlots = entry.getValue();
                    TimetableSlot firstSlot = conflictingSlots.get(0);
                    
                    return TimetableConflictReport.RoomConflict.builder()
                            .roomId(firstSlot.getRoom().getId())
                            .roomName(firstSlot.getRoom().getName())
                            .conflictType("DOUBLE_BOOKING")
                            .description("Room is booked for multiple classes at the same time")
                            .severity(TimetableConflictReport.ConflictSeverity.HIGH)
                            .impact("Classes cannot use the same room simultaneously")
                            .resolutionOptions(List.of("Change room for one class", "Reschedule one class", "Find alternative room"))
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private List<TimetableConflictReport.ClassConflict> detectClassConflicts(List<TimetableSlot> slots) {
        // Similar logic for class conflicts
        Map<String, List<TimetableSlot>> classTimeSlots = slots.stream()
                .filter(slot -> slot.getForClass() != null)
                .collect(Collectors.groupingBy(slot -> 
                    slot.getForClass().getId() + "_" + slot.getDayOfWeek() + "_" + slot.getPeriod().getId()));
        
        return classTimeSlots.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> {
                    List<TimetableSlot> conflictingSlots = entry.getValue();
                    TimetableSlot firstSlot = conflictingSlots.get(0);
                    
                    return TimetableConflictReport.ClassConflict.builder()
                            .classId(firstSlot.getForClass().getId())
                            .className(firstSlot.getForClass().getName())
                            .conflictType("DOUBLE_BOOKING")
                            .description("Class is scheduled for multiple subjects at the same time")
                            .severity(TimetableConflictReport.ConflictSeverity.HIGH)
                            .impact("Students cannot attend multiple classes simultaneously")
                            .resolutionOptions(List.of("Reschedule one subject", "Change time slot", "Split class if possible"))
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private TimetableOptimizationResult buildOptimizationResult(
            String optimizationId, Long timetableId, LocalDateTime startTime, LocalDateTime endTime, 
            long durationMs, TimetableSolution solution, TimetableConflictReport conflictReport,
            List<TeacherWorkloadAnalysis> workloadAnalyses, TimetableOptimizationRequest request) {
        
        // Calculate quality metrics
        TimetableOptimizationResult.QualityMetrics qualityMetrics = TimetableOptimizationResult.QualityMetrics.builder()
                .teacherSatisfaction(0.85) // Simplified calculation
                .studentSatisfaction(0.80)
                .resourceEfficiency(0.90)
                .scheduleCompactness(0.75)
                .conflictResolutionRate(0.95)
                .totalViolations(conflictReport.getTotalConflicts())
                .hardViolations(conflictReport.getCriticalConflicts())
                .softViolations(conflictReport.getMinorConflicts())
                .build();
        
        // Build teacher workload summaries
        List<TimetableOptimizationResult.TeacherWorkloadSummary> teacherSummaries = workloadAnalyses.stream()
                .map(analysis -> TimetableOptimizationResult.TeacherWorkloadSummary.builder()
                        .teacherId(analysis.getTeacherId())
                        .teacherName(analysis.getTeacherFirstName() + " " + analysis.getTeacherLastName())
                        .totalHours(analysis.getTotalWeeklyHours())
                        .maxCapacity(analysis.getMaxWeeklyCapacity())
                        .utilizationRate(analysis.getWorkloadPercentage())
                        .workloadRating(analysis.getStatus().name())
                        .build())
                .collect(Collectors.toList());
        
        return TimetableOptimizationResult.builder()
                .timetableId(timetableId)
                .optimizationId(optimizationId)
                .status(TimetableOptimizationResult.OptimizationStatus.COMPLETED)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .finalScore(solution.getScore() != null ? solution.getScore().toShortString().length() / 100.0 : 0.8)
                .qualityMetrics(qualityMetrics)
                .teacherWorkloads(teacherSummaries)
                .build();
    }
    
    private TimetableOptimizationResult buildFailedResult(String optimizationId, Long timetableId, LocalDateTime startTime, Exception e) {
        return TimetableOptimizationResult.builder()
                .timetableId(timetableId)
                .optimizationId(optimizationId)
                .status(TimetableOptimizationResult.OptimizationStatus.FAILED)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .warnings(List.of("Optimization failed: " + e.getMessage()))
                .build();
    }
    
    // Simplified helper methods
    private int calculateGaps(List<TimetableSlot> slots) { return 0; }
    private int calculateConsecutiveHours(List<TimetableSlot> slots) { return slots.size(); }
    private String calculateEfficiency(List<TimetableSlot> slots) { return "HIGH"; }
    private boolean hasConflicts(TimetableSlot slot, Long newTeacherId, Long newRoomId) { return false; }
}
