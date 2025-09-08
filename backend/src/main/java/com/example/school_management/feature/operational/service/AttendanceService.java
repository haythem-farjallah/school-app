package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.dto.AttendanceStatisticsDto;
import com.example.school_management.feature.operational.dto.TeacherAttendanceClassView;
import com.example.school_management.feature.operational.entity.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    
    // Record attendance for a single user
    AttendanceDto recordAttendance(AttendanceDto attendanceDto);
    
    // Record attendance for multiple users (batch)
    List<AttendanceDto> recordBatchAttendance(List<AttendanceDto> attendanceDtos);
    
    // Get attendance for a user in date range
    List<AttendanceDto> getUserAttendance(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Get attendance for a class on a specific date
    List<AttendanceDto> getClassAttendance(Long classId, LocalDate date);
    
    // Get attendance for a course on a specific date
    List<AttendanceDto> getCourseAttendance(Long courseId, LocalDate date);
    
    // Get attendance statistics for a user
    AttendanceStatisticsDto getGeneralAttendanceStatistics(LocalDate startDate, LocalDate endDate);
    
    AttendanceStatisticsDto getUserAttendanceStatistics(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Get attendance statistics for a class
    List<AttendanceStatisticsDto> getClassAttendanceStatistics(Long classId, LocalDate startDate, LocalDate endDate);
    
    // Update attendance record
    AttendanceDto updateAttendance(Long attendanceId, AttendanceDto attendanceDto);
    
    // Delete attendance record
    void deleteAttendance(Long attendanceId);
    
    // Get attendance by user type with pagination
    Page<AttendanceDto> getAttendanceByUserType(UserType userType, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    // Mark attendance as excused
    AttendanceDto markAsExcused(Long attendanceId, String excuse);
    
    // Mark attendance as late
    AttendanceDto markAsLate(Long attendanceId, String remarks);
    
    // Advanced filtering for attendance records
    Page<AttendanceDto> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> requestParams);
    
    // Teacher-specific attendance methods
    
    // Get teacher's current day schedule with attendance status
    List<AttendanceDto> getTeacherTodayScheduleWithAttendance(Long teacherId, LocalDate date);
    
    // Get students for a specific timetable slot (for attendance marking)
    List<AttendanceDto> getStudentsForTimetableSlot(Long timetableSlotId, LocalDate date);
    
    // Mark attendance for all students in a timetable slot
    List<AttendanceDto> markAttendanceForTimetableSlot(Long timetableSlotId, LocalDate date, List<AttendanceDto> attendanceList);
    
    // Check if teacher can mark attendance for a specific slot today
    boolean canTeacherMarkAttendance(Long teacherId, Long timetableSlotId, LocalDate date);
    
    // Get teacher's weekly schedule with attendance summary
    Map<String, List<AttendanceDto>> getTeacherWeeklyAttendanceSummary(Long teacherId, LocalDate startOfWeek);
    
    // Get absent students list for a teacher's class today
    List<AttendanceDto> getAbsentStudentsForTeacher(Long teacherId, LocalDate date);
    
    // Class-based attendance methods (for virtual slots)
    
    // Get students for a class with attendance status
    List<AttendanceDto> getStudentsForClass(Long classId, LocalDate date);
    
    // Get all students in a class (simple list without date filtering)
    List<AttendanceDto> getStudentsForClassSimple(Long classId);
    
    // Get teacher attendance class view (similar to grade system)
    TeacherAttendanceClassView getTeacherAttendanceClass(Long teacherId, Long classId, Long courseId);
    
    // Mark attendance for all students in a class
    List<AttendanceDto> markAttendanceForClass(Long classId, LocalDate date, List<AttendanceDto> attendanceList);
} 