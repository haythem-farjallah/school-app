package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.dto.AttendanceStatisticsDto;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final BaseUserRepository<BaseUser> userRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final OperationalMapper mapper;

    private BaseUser getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @Override
    public AttendanceDto recordAttendance(AttendanceDto attendanceDto) {
        log.debug("Recording attendance for user: {}", attendanceDto.getUserId());
        
        // Validate user exists
        BaseUser user = userRepository.findById(attendanceDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check for existing attendance record
        Attendance existingAttendance = findExistingAttendance(attendanceDto);
        if (existingAttendance != null) {
            log.warn("Attendance record already exists for user {} on date {}", 
                    attendanceDto.getUserId(), attendanceDto.getDate());
            return mapper.toAttendanceDto(existingAttendance);
        }
        
        // Create new attendance record
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setDate(attendanceDto.getDate());
        attendance.setStatus(attendanceDto.getStatus());
        attendance.setRemarks(attendanceDto.getRemarks());
        attendance.setExcuse(attendanceDto.getExcuse());
        attendance.setMedicalNote(attendanceDto.getMedicalNote());
        attendance.setRecordedAt(LocalDateTime.now());
        
        // Set course if provided
        if (attendanceDto.getCourseId() != null) {
            Course course = courseRepository.findById(attendanceDto.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
            attendance.setCourse(course);
        }
        
        // Set class if provided
        if (attendanceDto.getClassId() != null) {
            ClassEntity classEntity = classRepository.findById(attendanceDto.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
            attendance.setClassEntity(classEntity);
        }
        
        // Set timetable slot if provided
        if (attendanceDto.getTimetableSlotId() != null) {
            TimetableSlot slot = timetableSlotRepository.findById(attendanceDto.getTimetableSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
            attendance.setTimetableSlot(slot);
        }
        
        // Set recorded by (current user from security context)
        BaseUser currentUser = getCurrentUser();
        attendance.setRecordedBy(currentUser);
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Attendance recorded for user {} on date {}", user.getId(), attendanceDto.getDate());
        
        return mapper.toAttendanceDto(savedAttendance);
    }

    @Override
    public List<AttendanceDto> recordBatchAttendance(List<AttendanceDto> attendanceDtos) {
        log.debug("Recording batch attendance for {} records", attendanceDtos.size());
        
        return attendanceDtos.stream()
                .map(this::recordAttendance)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDto> getUserAttendance(Long userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting attendance for user {} from {} to {}", userId, startDate, endDate);
        
        List<Attendance> attendances = attendanceRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return attendances.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDto> getClassAttendance(Long classId, LocalDate date) {
        log.debug("Getting class attendance for class {} on date {}", classId, date);
        
        List<Attendance> attendances = attendanceRepository.findByClassIdAndDate(classId, date);
        return attendances.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDto> getCourseAttendance(Long courseId, LocalDate date) {
        log.debug("Getting course attendance for course {} on date {}", courseId, date);
        
        List<Attendance> attendances = attendanceRepository.findByCourseIdAndDate(courseId, date);
        return attendances.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceStatisticsDto getUserAttendanceStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting attendance statistics for user {} from {} to {}", userId, startDate, endDate);
        
        List<Attendance> attendances = attendanceRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        
        long totalDays = attendances.size();
        long presentDays = attendances.stream().filter(Attendance::isPresent).count();
        long absentDays = attendances.stream().filter(Attendance::isAbsent).count();
        long lateDays = attendances.stream().filter(Attendance::isLate).count();
        long excusedDays = attendances.stream().filter(Attendance::isExcused).count();
        
        double attendancePercentage = totalDays > 0 ? (double) presentDays / totalDays * 100 : 0;
        double absencePercentage = totalDays > 0 ? (double) absentDays / totalDays * 100 : 0;
        
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return new AttendanceStatisticsDto(
                userId,
                user.getFirstName() + " " + user.getLastName(),
                attendances.isEmpty() ? "UNKNOWN" : attendances.get(0).getUserType().name(),
                startDate,
                endDate,
                totalDays,
                presentDays,
                absentDays,
                lateDays,
                excusedDays,
                attendancePercentage,
                absencePercentage
        );
    }

    @Override
    public List<AttendanceStatisticsDto> getClassAttendanceStatistics(Long classId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting class attendance statistics for class {} from {} to {}", classId, startDate, endDate);
        
        List<Attendance> attendances = attendanceRepository.findByClassIdAndDate(classId, startDate);
        
        return attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()))
                .values()
                .stream()
                .map(userAttendances -> {
                    Long userId = userAttendances.get(0).getUser().getId();
                    return getUserAttendanceStatistics(userId, startDate, endDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceDto updateAttendance(Long attendanceId, AttendanceDto attendanceDto) {
        log.debug("Updating attendance {}", attendanceId);
        
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        
        attendance.setStatus(attendanceDto.getStatus());
        attendance.setRemarks(attendanceDto.getRemarks());
        attendance.setExcuse(attendanceDto.getExcuse());
        attendance.setMedicalNote(attendanceDto.getMedicalNote());
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return mapper.toAttendanceDto(updatedAttendance);
    }

    @Override
    public void deleteAttendance(Long attendanceId) {
        log.debug("Deleting attendance {}", attendanceId);
        
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance not found");
        }
        
        attendanceRepository.deleteById(attendanceId);
    }

    @Override
    public Page<AttendanceDto> getAttendanceByUserType(UserType userType, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.debug("Getting attendance by user type {} from {} to {}", userType, startDate, endDate);
        
        Page<Attendance> attendances = attendanceRepository.findByUserTypeAndDateBetween(userType, startDate, endDate, pageable);
        return attendances.map(mapper::toAttendanceDto);
    }

    @Override
    public AttendanceDto markAsExcused(Long attendanceId, String excuse) {
        log.debug("Marking attendance {} as excused", attendanceId);
        
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        
        attendance.setStatus(AttendanceStatus.EXCUSED);
        attendance.setExcuse(excuse);
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return mapper.toAttendanceDto(updatedAttendance);
    }

    @Override
    public AttendanceDto markAsLate(Long attendanceId, String remarks) {
        log.debug("Marking attendance {} as late", attendanceId);
        
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        
        attendance.setStatus(AttendanceStatus.LATE);
        attendance.setRemarks(remarks);
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return mapper.toAttendanceDto(updatedAttendance);
    }

    @Override
    public Page<AttendanceDto> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> requestParams) {
        log.debug("Finding attendance with advanced filters");
        
        // Parse request parameters into FilterCriteria
        FilterCriteria filterCriteria = FilterCriteriaParser.parseRequestParams(requestParams);
        
        // Build JPA Specification from FilterCriteria
        Specification<Attendance> specification = DynamicSpecificationBuilder.build(filterCriteria);
        
        // Execute query with pagination
        Page<Attendance> attendances = attendanceRepository.findAll(specification, pageable);
        
        // Map to DTOs
        return attendances.map(mapper::toAttendanceDto);
    }

    private Attendance findExistingAttendance(AttendanceDto attendanceDto) {
        if (attendanceDto.getCourseId() != null) {
            return attendanceRepository.findByUserIdAndCourseIdAndDate(
                    attendanceDto.getUserId(), attendanceDto.getCourseId(), attendanceDto.getDate()).orElse(null);
        } else if (attendanceDto.getClassId() != null) {
            return attendanceRepository.findByUserIdAndClassIdAndDate(
                    attendanceDto.getUserId(), attendanceDto.getClassId(), attendanceDto.getDate()).orElse(null);
        }
        return null;
    }
} 