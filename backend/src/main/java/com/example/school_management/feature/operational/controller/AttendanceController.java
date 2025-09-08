package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.dto.AttendanceStatisticsDto;
import com.example.school_management.feature.operational.dto.TeacherAttendanceClassView;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.service.AttendanceService;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance Management", description = "APIs for managing attendance records")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    @PostMapping
    @Operation(summary = "Record attendance for a single user")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> recordAttendance(
            @Valid @RequestBody AttendanceDto attendanceDto) {
        log.debug("Recording attendance for user: {}", attendanceDto.getUserId());
        
        AttendanceDto recorded = attendanceService.recordAttendance(attendanceDto);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
        return builder.body(new ApiSuccessResponse<>("success", recorded));
    }

    @PostMapping("/batch")
    @Operation(summary = "Record attendance for multiple users")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> recordBatchAttendance(
            @Valid @RequestBody List<AttendanceDto> attendanceDtos) {
        log.debug("Recording batch attendance for {} users", attendanceDtos.size());
        
        List<AttendanceDto> recorded = attendanceService.recordBatchAttendance(attendanceDtos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", recorded));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get attendance for a specific user in date range")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getUserAttendance(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting attendance for user {} from {} to {}", userId, startDate, endDate);
        
        List<AttendanceDto> attendances = attendanceService.getUserAttendance(userId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", attendances));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get attendance for a specific class on a date")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getClassAttendance(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Getting class attendance for class {} on date {}", classId, date);
        
        List<AttendanceDto> attendances = attendanceService.getClassAttendance(classId, date);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", attendances));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get attendance for a specific course on a date")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getCourseAttendance(
            @PathVariable Long courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Getting course attendance for course {} on date {}", courseId, date);
        
        List<AttendanceDto> attendances = attendanceService.getCourseAttendance(courseId, date);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", attendances));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get general attendance statistics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceStatisticsDto>> getAttendanceStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting general attendance statistics from {} to {}", startDate, endDate);
        
        AttendanceStatisticsDto statistics = attendanceService.getGeneralAttendanceStatistics(startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", statistics));
    }

    @GetMapping("/statistics/user/{userId}")
    @Operation(summary = "Get attendance statistics for a user")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<AttendanceStatisticsDto>> getUserAttendanceStatistics(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting attendance statistics for user {} from {} to {}", userId, startDate, endDate);
        
        AttendanceStatisticsDto statistics = attendanceService.getUserAttendanceStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", statistics));
    }

    @GetMapping("/statistics/class/{classId}")
    @Operation(summary = "Get attendance statistics for a class")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceStatisticsDto>>> getClassAttendanceStatistics(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting attendance statistics for class {} from {} to {}", classId, startDate, endDate);
        
        List<AttendanceStatisticsDto> statistics = attendanceService.getClassAttendanceStatistics(classId, startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", statistics));
    }

    @PutMapping("/{attendanceId}")
    @Operation(summary = "Update attendance record")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> updateAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody AttendanceDto attendanceDto) {
        log.debug("Updating attendance {}", attendanceId);
        
        AttendanceDto updated = attendanceService.updateAttendance(attendanceId, attendanceDto);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", updated));
    }

    @DeleteMapping("/{attendanceId}")
    @Operation(summary = "Delete attendance record")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteAttendance(@PathVariable Long attendanceId) {
        log.debug("Deleting attendance {}", attendanceId);
        
        attendanceService.deleteAttendance(attendanceId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @GetMapping("/type/{userType}")
    @Operation(summary = "Get attendance by user type with pagination")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Page<AttendanceDto>>> getAttendanceByUserType(
            @PathVariable UserType userType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        log.debug("Getting attendance by user type {} from {} to {}", userType, startDate, endDate);
        
        Page<AttendanceDto> attendances = attendanceService.getAttendanceByUserType(userType, startDate, endDate, pageable);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", attendances));
    }

    @PatchMapping("/{attendanceId}/excuse")
    @Operation(summary = "Mark attendance as excused")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> markAsExcused(
            @PathVariable Long attendanceId,
            @RequestParam String excuse) {
        log.debug("Marking attendance {} as excused", attendanceId);
        
        AttendanceDto updated = attendanceService.markAsExcused(attendanceId, excuse);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", updated));
    }

    @PatchMapping("/{attendanceId}/late")
    @Operation(summary = "Mark attendance as late")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<AttendanceDto>> markAsLate(
            @PathVariable Long attendanceId,
            @RequestParam String remarks) {
        log.debug("Marking attendance {} as late", attendanceId);
        
        AttendanceDto updated = attendanceService.markAsLate(attendanceId, remarks);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", updated));
    }

    @GetMapping("/filter")
    @Operation(summary = "Advanced filtering for attendance records")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<PageDto<AttendanceDto>>> filterAttendance(
            @PageableDefault(size = 20) Pageable pageable,
            jakarta.servlet.http.HttpServletRequest request) {
        log.debug("Advanced filtering attendance records");
        
        Page<AttendanceDto> attendances = attendanceService.findWithAdvancedFilters(pageable, request.getParameterMap());
        var dto = new PageDto<>(attendances);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    // Teacher-specific attendance endpoints

    @GetMapping("/teacher/{teacherId}/today")
    @Operation(summary = "Get teacher's today schedule with attendance status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getTeacherTodaySchedule(
            @PathVariable Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Getting today's schedule for teacher {} on date {}", teacherId, targetDate);
        
        List<AttendanceDto> schedule = attendanceService.getTeacherTodayScheduleWithAttendance(teacherId, targetDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", schedule));
    }

    @GetMapping("/debug/teacher/{teacherId}/assignments")
    @Operation(summary = "Debug endpoint to check teacher assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Object>> debugTeacherAssignments(@PathVariable Long teacherId) {
        log.debug("Debug: Checking assignments for teacher {}", teacherId);
        
        // Get teaching assignments
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacherId);
        log.debug("Debug: Found {} teaching assignments for teacher {}", assignments.size(), teacherId);
        
        // Get timetable slots
        List<TimetableSlot> slots = timetableSlotRepository.findByTeacherId(teacherId);
        log.debug("Debug: Found {} timetable slots for teacher {}", slots.size(), teacherId);
        
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("teacherId", teacherId);
        debugInfo.put("teachingAssignmentsCount", assignments.size());
        debugInfo.put("timetableSlotsCount", slots.size());
        debugInfo.put("assignments", assignments.stream().map(ta -> Map.of(
            "id", ta.getId(),
            "classId", ta.getClazz().getId(),
            "className", ta.getClazz().getName(),
            "courseId", ta.getCourse().getId(),
            "courseName", ta.getCourse().getName()
        )).collect(Collectors.toList()));
        debugInfo.put("slots", slots.stream().map(ts -> Map.of(
            "id", ts.getId(),
            "dayOfWeek", ts.getDayOfWeek(),
            "startTime", ts.getPeriod() != null ? ts.getPeriod().getStartTime() : null,
            "endTime", ts.getPeriod() != null ? ts.getPeriod().getEndTime() : null,
            "classId", ts.getForClass() != null ? ts.getForClass().getId() : null,
            "className", ts.getForClass() != null ? ts.getForClass().getName() : null
        )).collect(Collectors.toList()));
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", debugInfo));
    }

    @GetMapping("/teacher/{teacherId}/absent-students")
    @Operation(summary = "Get absent students for teacher today")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getAbsentStudentsForTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Getting absent students for teacher {} on date {}", teacherId, targetDate);
        
        List<AttendanceDto> absentStudents = attendanceService.getAbsentStudentsForTeacher(teacherId, targetDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", absentStudents));
    }

    @GetMapping("/teacher/{teacherId}/weekly-summary")
    @Operation(summary = "Get teacher's weekly attendance summary")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, List<AttendanceDto>>>> getTeacherWeeklySummary(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startOfWeek) {
        log.debug("Getting weekly attendance summary for teacher {} starting from {}", teacherId, startOfWeek);
        
        Map<String, List<AttendanceDto>> summary = attendanceService.getTeacherWeeklyAttendanceSummary(teacherId, startOfWeek);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", summary));
    }

    @GetMapping("/slot/{slotId}/students")
    @Operation(summary = "Get students for a timetable slot with attendance status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getStudentsForSlot(
            @PathVariable Long slotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Getting students for timetable slot {} on date {}", slotId, targetDate);
        
        List<AttendanceDto> students = attendanceService.getStudentsForTimetableSlot(slotId, targetDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", students));
    }

    @PostMapping("/slot/{slotId}/mark")
    @Operation(summary = "Mark attendance for all students in a timetable slot")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> markAttendanceForSlot(
            @PathVariable Long slotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody List<AttendanceDto> attendanceList) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Marking attendance for slot {} on date {} for {} students", slotId, targetDate, attendanceList.size());
        
        List<AttendanceDto> markedAttendance = attendanceService.markAttendanceForTimetableSlot(slotId, targetDate, attendanceList);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", markedAttendance));
    }

    @GetMapping("/teacher/{teacherId}/can-mark/{slotId}")
    @Operation(summary = "Check if teacher can mark attendance for a specific slot")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> canTeacherMarkAttendance(
            @PathVariable Long teacherId,
            @PathVariable Long slotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Checking if teacher {} can mark attendance for slot {} on date {}", teacherId, slotId, targetDate);
        
        boolean canMark = attendanceService.canTeacherMarkAttendance(teacherId, slotId, targetDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", canMark));
    }

    @GetMapping("/class/{classId}/students")
    @Operation(summary = "Get students for a class with attendance status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getStudentsForClass(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Getting students for class {} on date {}", classId, targetDate);
        
        List<AttendanceDto> students = attendanceService.getStudentsForClass(classId, targetDate);
        log.info("Controller returning {} students for class {} on date {}", students.size(), classId, targetDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", students));
    }

    @GetMapping("/class/{classId}/students-simple")
    @Operation(summary = "Get all students in a class (simple list)")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> getStudentsForClassSimple(@PathVariable Long classId) {
        log.debug("Getting students for class {} (simple)", classId);
        
        List<AttendanceDto> students = attendanceService.getStudentsForClassSimple(classId);
        log.info("🔍 Controller returning {} students for class {}", students.size(), classId);
        
        // Log first few students for debugging
        if (!students.isEmpty()) {
            log.info("🔍 First student: {}", students.get(0).getUserName());
        } else {
            log.warn("🔍 No students found for class {}", classId);
        }
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Students retrieved successfully", students));
    }

    @GetMapping("/teacher/{teacherId}/class/{classId}/course/{courseId}")
    @Operation(summary = "Get teacher attendance class view (similar to grade system)")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TeacherAttendanceClassView>> getTeacherAttendanceClass(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long courseId) {
        TeacherAttendanceClassView classView = attendanceService.getTeacherAttendanceClass(teacherId, classId, courseId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher attendance class retrieved successfully", classView));
    }

    @PostMapping("/class/{classId}/mark")
    @Operation(summary = "Mark attendance for all students in a class")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<AttendanceDto>>> markAttendanceForClass(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody List<AttendanceDto> attendanceList) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Marking attendance for class {} on date {} for {} students", classId, targetDate, attendanceList.size());
        
        List<AttendanceDto> markedAttendance = attendanceService.markAttendanceForClass(classId, targetDate, attendanceList);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", markedAttendance));
    }
} 