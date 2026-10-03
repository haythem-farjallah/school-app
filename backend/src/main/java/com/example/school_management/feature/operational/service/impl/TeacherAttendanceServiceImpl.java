package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import com.example.school_management.feature.operational.dto.TeacherAttendanceResponse;
import com.example.school_management.feature.operational.dto.TeacherAttendanceStatistics;
import com.example.school_management.feature.operational.entity.TeacherAttendance;
import com.example.school_management.feature.operational.repository.TeacherAttendanceRepository;
import com.example.school_management.feature.operational.service.TeacherAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CurrentSchoolResolver currentSchool;
    
    @Override
    public TeacherAttendanceResponse createTeacherAttendance(TeacherAttendanceRequest request) {
        log.debug("Creating teacher attendance record for teacher: {} on date: {}", request.getTeacherId(), request.getDate());
        
        School school = currentSchool.resolve();
        Teacher teacher = requireTeacher(request.getTeacherId(), school.getId());
        ClassEntity clazz = request.getClassId() == null ? null
                : classRepository.findByIdAndAcademicYearSchoolId(request.getClassId(), school.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        Course course = request.getCourseId() == null ? null
                : courseRepository.findByIdAndSchoolId(request.getCourseId(), school.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        Teacher substitute = request.getSubstituteTeacherId() == null ? null
                : requireTeacher(request.getSubstituteTeacherId(), school.getId());
        BaseUser recorder = currentAccount();
        if (teacherAttendanceRepository.findByTeacherIdAndDateAndSchoolId(
                teacher.getId(), request.getDate(), school.getId()).isPresent()) {
            throw new IllegalArgumentException("Attendance record already exists for teacher " + teacher.getId() + " on date " + request.getDate());
        }

        TeacherAttendance attendance = new TeacherAttendance();
        attendance.setSchool(school);
        attendance.setTeacherId(teacher.getId());
        attendance.setTeacherFirstName(teacher.getFirstName());
        attendance.setTeacherLastName(teacher.getLastName());
        attendance.setTeacherEmail(teacher.getEmail());
        attendance.setDate(request.getDate());
        attendance.setStatus(request.getStatus());
        attendance.setCourseId(course == null ? null : course.getId());
        attendance.setCourseName(course == null ? null : course.getName());
        attendance.setClassId(clazz == null ? null : clazz.getId());
        attendance.setClassName(clazz == null ? null : clazz.getName());
        attendance.setRemarks(request.getRemarks());
        attendance.setExcuse(request.getExcuse());
        setSubstitute(attendance, substitute);
        attendance.setRecordedById(recorder.getId());
        attendance.setRecordedByName(fullName(recorder));

        TeacherAttendance savedAttendance = teacherAttendanceRepository.save(attendance);
        log.debug("Teacher attendance record created with ID: {}", savedAttendance.getId());
        
        return mapToResponse(savedAttendance);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeacherAttendanceResponse> getTeacherAttendance(Long teacherId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting teacher attendance records for teacher: {} between {} and {}", teacherId, startDate, endDate);
        
        Long schoolId = currentSchool.resolve().getId();
        if (teacherId != null) requireTeacher(teacherId, schoolId);
        List<TeacherAttendance> attendanceRecords;
        
        if (teacherId != null && startDate != null && endDate != null) {
            attendanceRecords = teacherAttendanceRepository.findByTeacherIdAndDateBetweenAndSchoolId(teacherId, startDate, endDate, schoolId);
        } else if (teacherId != null) {
            attendanceRecords = teacherAttendanceRepository.findByTeacherIdAndSchoolId(teacherId, schoolId);
        } else if (startDate != null && endDate != null) {
            attendanceRecords = teacherAttendanceRepository.findByDateBetweenAndSchoolId(startDate, endDate, schoolId);
        } else {
            attendanceRecords = teacherAttendanceRepository.findBySchoolId(schoolId);
        }
        
        return attendanceRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeacherAttendanceResponse> getAttendanceByDate(LocalDate date) {
        log.debug("Getting teacher attendance records for date: {}", date);
        
        List<TeacherAttendance> attendanceRecords = teacherAttendanceRepository.findByDateAndSchoolId(date, currentSchool.resolve().getId());
        return attendanceRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public TeacherAttendanceStatistics getTeacherAttendanceStatistics(Long teacherId) {
        log.debug("Getting teacher attendance statistics for teacher: {}", teacherId);
        
        Long schoolId = currentSchool.resolve().getId();
        Teacher teacher = requireTeacher(teacherId, schoolId);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(12);
        Long totalDays = teacherAttendanceRepository.countByTeacherIdAndDateBetweenAndSchoolId(
                teacherId, start, end, schoolId);
        
        Long presentDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetweenAndSchoolId(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.PRESENT, 
                start, end, schoolId);
        
        Long absentDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetweenAndSchoolId(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.ABSENT, 
                start, end, schoolId);
        
        Long lateDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetweenAndSchoolId(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.LATE, 
                start, end, schoolId);
        
        Long sickLeaveDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetweenAndSchoolId(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.SICK_LEAVE, 
                start, end, schoolId);
        
        Long personalLeaveDays = teacherAttendanceRepository.countByTeacherIdAndStatusAndDateBetweenAndSchoolId(
                teacherId, TeacherAttendanceRequest.TeacherAttendanceStatus.PERSONAL_LEAVE, 
                start, end, schoolId);
        
        Double attendanceRate = totalDays > 0 ? (presentDays.doubleValue() / totalDays.doubleValue()) * 100 : 0.0;
        
        // Get monthly breakdown
        List<Object[]> monthlyData = teacherAttendanceRepository.getMonthlyStatisticsByTeacherIdAndSchoolId(teacherId, schoolId);
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
                .teacherName(fullName(teacher))
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
        
        Long schoolId = currentSchool.resolve().getId();
        TeacherAttendance attendance = requireAttendance(attendanceId, schoolId);
        Teacher substitute = request.getSubstituteTeacherId() == null ? null
                : requireTeacher(request.getSubstituteTeacherId(), schoolId);
        
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        attendance.setExcuse(request.getExcuse());
        setSubstitute(attendance, substitute);
        
        TeacherAttendance updatedAttendance = teacherAttendanceRepository.save(attendance);
        log.debug("Teacher attendance record updated with ID: {}", updatedAttendance.getId());
        
        return mapToResponse(updatedAttendance);
    }
    
    @Override
    public void deleteTeacherAttendance(Long attendanceId) {
        log.debug("Deleting teacher attendance record with ID: {}", attendanceId);
        
        TeacherAttendance attendance = requireAttendance(attendanceId, currentSchool.resolve().getId());
        teacherAttendanceRepository.delete(attendance);
        log.debug("Teacher attendance record deleted with ID: {}", attendanceId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean attendanceExistsForTeacherAndDate(Long teacherId, LocalDate date) {
        Long schoolId = currentSchool.resolve().getId();
        requireTeacher(teacherId, schoolId);
        return teacherAttendanceRepository.findByTeacherIdAndDateAndSchoolId(teacherId, date, schoolId).isPresent();
    }
    
    private Teacher requireTeacher(Long teacherId, Long schoolId) {
        return teacherRepository.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private TeacherAttendance requireAttendance(Long attendanceId, Long schoolId) {
        return teacherAttendanceRepository.findByIdAndSchoolId(attendanceId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher attendance not found"));
    }

    private BaseUser currentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authenticated account required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account required"));
    }

    private String fullName(BaseUser account) {
        return account.getFirstName() + " " + account.getLastName();
    }

    private void setSubstitute(TeacherAttendance attendance, Teacher substitute) {
        attendance.setSubstituteTeacherId(substitute == null ? null : substitute.getId());
        attendance.setSubstituteTeacherName(substitute == null ? null : fullName(substitute));
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
