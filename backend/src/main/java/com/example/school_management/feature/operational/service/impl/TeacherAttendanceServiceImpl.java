package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import com.example.school_management.feature.operational.dto.TeacherAttendanceResponse;
import com.example.school_management.feature.operational.dto.TeacherAttendanceStatistics;
import com.example.school_management.feature.operational.entity.TeacherAttendance;
import com.example.school_management.feature.operational.repository.TeacherAttendanceRepository;
import com.example.school_management.feature.operational.service.TeacherAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAttendanceServiceImpl implements TeacherAttendanceService {
    
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    
    @Override
    public TeacherAttendanceResponse createTeacherAttendance(TeacherAttendanceRequest request) {
        log.debug("Creating teacher attendance record for teacher: {} on date: {}", request.getTeacherId(), request.getDate());
        
        // Check if attendance already exists for this teacher and date
        if (attendanceExistsForTeacherAndDate(request.getTeacherId(), request.getDate())) {
            throw new IllegalArgumentException("Attendance record already exists for teacher " + request.getTeacherId() + " on date " + request.getDate());
        }
        
        TeacherAttendance attendance = new TeacherAttendance();
        attendance.setTeacherId(request.getTeacherId());
        attendance.setTeacherFirstName(request.getTeacherFirstName());
        attendance.setTeacherLastName(request.getTeacherLastName());
        attendance.setTeacherEmail(request.getTeacherEmail());
        attendance.setDate(request.getDate());
        attendance.setStatus(request.getStatus());
        attendance.setCourseId(request.getCourseId());
        attendance.setCourseName(request.getCourseName());
        attendance.setClassId(request.getClassId());
        attendance.setClassName(request.getClassName());
        attendance.setRemarks(request.getRemarks());
        attendance.setExcuse(request.getExcuse());
        attendance.setSubstituteTeacherId(request.getSubstituteTeacherId());
        attendance.setSubstituteTeacherName(request.getSubstituteTeacherName());
        attendance.setRecordedById(request.getRecordedById());
        attendance.setRecordedByName(request.getRecordedByName());
        
        TeacherAttendance savedAttendance = teacherAttendanceRepository.save(attendance);
        log.debug("Teacher attendance record created with ID: {}", savedAttendance.getId());
        
        return mapToResponse(savedAttendance);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeacherAttendanceResponse> getTeacherAttendance(Long teacherId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting teacher attendance records for teacher: {} between {} and {}", teacherId, startDate, endDate);
        
        List<TeacherAttendance> attendanceRecords;
        
        if (teacherId != null && startDate != null && endDate != null) {
            attendanceRecords = teacherAttendanceRepository.findByTeacherIdAndDateBetween(teacherId, startDate, endDate);
        } else if (teacherId != null) {
            attendanceRecords = teacherAttendanceRepository.findByTeacherId(teacherId);
        } else if (startDate != null && endDate != null) {
            attendanceRecords = teacherAttendanceRepository.findByDateBetween(startDate, endDate);
        } else {
            attendanceRecords = teacherAttendanceRepository.findAll();
        }
        
        return attendanceRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeacherAttendanceResponse> getAttendanceByDate(LocalDate date) {
        log.debug("Getting teacher attendance records for date: {}", date);
        
        List<TeacherAttendance> attendanceRecords = teacherAttendanceRepository.findByDate(date);
        return attendanceRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public TeacherAttendanceStatistics getTeacherAttendanceStatistics(Long teacherId) {
        log.debug("Getting teacher attendance statistics for teacher: {}", teacherId);
        
        // Calculate overall statistics
        Long totalDays = teacherAttendanceRepository.countByTeacherIdAndDateBetween(
                teacherId, LocalDate.now().minusMonths(12), LocalDate.now());
        
        Long presentDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetween(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.PRESENT, 
                LocalDate.now().minusMonths(12), LocalDate.now());
        
        Long absentDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetween(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.ABSENT, 
                LocalDate.now().minusMonths(12), LocalDate.now());
        
        Long lateDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetween(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.LATE, 
                LocalDate.now().minusMonths(12), LocalDate.now());
        
        Long sickLeaveDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetween(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.SICK_LEAVE, 
                LocalDate.now().minusMonths(12), LocalDate.now());
        
        Long personalLeaveDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetween(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.PERSONAL_LEAVE, 
                LocalDate.now().minusMonths(12), LocalDate.now());
        
        Double attendanceRate = totalDays > 0 ? (presentDays.doubleValue() / totalDays.doubleValue()) * 100 : 0.0;
        
        // Get monthly breakdown
        List<Object[]> monthlyData = teacherAttendanceRepository.getMonthlyStatistics(teacherId);
        List<TeacherAttendanceStatistics.MonthlyBreakdown> monthlyBreakdown = monthlyData.stream()
                .map(data -> {
                    Integer month = ((Number) data[0]).intValue();
                    Integer year = ((Number) data[1]).intValue();
                    Long present = ((Number) data[2]).longValue();
                    Long absent = ((Number) data[3]).longValue();
                    Long total = present + absent;
                    Double rate = total > 0 ? (present.doubleValue() / total.doubleValue()) * 100 : 0.0;
                    
                    return TeacherAttendanceStatistics.MonthlyBreakdown.builder()
                            .month(DateTimeFormatter.ofPattern("MMMM yyyy").format(LocalDate.of(year, month, 1)))
                            .present(present.intValue())
                            .absent(absent.intValue())
                            .rate(rate)
                            .build();
                })
                .collect(Collectors.toList());
        
        return TeacherAttendanceStatistics.builder()
                .teacherId(teacherId)
                .teacherName("Teacher " + teacherId) // This should be fetched from Teacher entity
                .totalDays(totalDays.intValue())
                .presentDays(presentDays.intValue())
                .absentDays(absentDays.intValue())
                .lateDays(lateDays.intValue())
                .sickLeaveDays(sickLeaveDays.intValue())
                .personalLeaveDays(personalLeaveDays.intValue())
                .attendanceRate(attendanceRate)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }
    
    @Override
    public TeacherAttendanceResponse updateTeacherAttendance(Long attendanceId, TeacherAttendanceRequest request) {
        log.debug("Updating teacher attendance record with ID: {}", attendanceId);
        
        TeacherAttendance attendance = teacherAttendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher attendance record not found with ID: " + attendanceId));
        
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        attendance.setExcuse(request.getExcuse());
        attendance.setSubstituteTeacherId(request.getSubstituteTeacherId());
        attendance.setSubstituteTeacherName(request.getSubstituteTeacherName());
        
        TeacherAttendance updatedAttendance = teacherAttendanceRepository.save(attendance);
        log.debug("Teacher attendance record updated with ID: {}", updatedAttendance.getId());
        
        return mapToResponse(updatedAttendance);
    }
    
    @Override
    public void deleteTeacherAttendance(Long attendanceId) {
        log.debug("Deleting teacher attendance record with ID: {}", attendanceId);
        
        if (!teacherAttendanceRepository.existsById(attendanceId)) {
            throw new IllegalArgumentException("Teacher attendance record not found with ID: " + attendanceId);
        }
        
        teacherAttendanceRepository.deleteById(attendanceId);
        log.debug("Teacher attendance record deleted with ID: {}", attendanceId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean attendanceExistsForTeacherAndDate(Long teacherId, LocalDate date) {
        return teacherAttendanceRepository.findByTeacherIdAndDate(teacherId, date).isPresent();
    }
    
    private TeacherAttendanceResponse mapToResponse(TeacherAttendance attendance) {
        return TeacherAttendanceResponse.builder()
                .id(attendance.getId())
                .teacherId(attendance.getTeacherId())
                .teacherFirstName(attendance.getTeacherFirstName())
                .teacherLastName(attendance.getTeacherLastName())
                .teacherEmail(attendance.getTeacherEmail())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .courseId(attendance.getCourseId())
                .courseName(attendance.getCourseName())
                .classId(attendance.getClassId())
                .className(attendance.getClassName())
                .remarks(attendance.getRemarks())
                .excuse(attendance.getExcuse())
                .substituteTeacherId(attendance.getSubstituteTeacherId())
                .substituteTeacherName(attendance.getSubstituteTeacherName())
                .recordedById(attendance.getRecordedById())
                .recordedByName(attendance.getRecordedByName())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
