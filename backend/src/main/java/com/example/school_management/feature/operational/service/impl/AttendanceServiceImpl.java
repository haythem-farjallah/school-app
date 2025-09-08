package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.dto.AttendanceStatisticsDto;
import com.example.school_management.feature.operational.dto.TeacherAttendanceClassView;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.AttendanceService;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.operational.entity.Notification;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import com.example.school_management.feature.operational.repository.NotificationRepository;
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
import java.util.*;
import java.util.stream.Collectors;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
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
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RealTimeNotificationService realTimeNotificationService;
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
        
        // Send absence notifications if student is marked absent
        if (user instanceof Student && savedAttendance.getStatus() == AttendanceStatus.ABSENT) {
            sendAbsenceNotifications((Student) user, savedAttendance);
        }
        
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
    public AttendanceStatisticsDto getGeneralAttendanceStatistics(LocalDate startDate, LocalDate endDate) {
        log.debug("Getting general attendance statistics from {} to {}", startDate, endDate);
        
        // If dates are not provided, default to current month
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        List<Attendance> attendances = attendanceRepository.findByDateBetween(startDate, endDate);
        
        long totalRecords = attendances.size();
        long presentCount = attendances.stream().filter(Attendance::isPresent).count();
        long absentCount = attendances.stream().filter(Attendance::isAbsent).count();
        long lateCount = attendances.stream().filter(Attendance::isLate).count();
        long excusedCount = attendances.stream().filter(Attendance::isExcused).count();
        
        double attendancePercentage = totalRecords > 0 ? (double) presentCount / totalRecords * 100 : 0;
        double absencePercentage = totalRecords > 0 ? (double) absentCount / totalRecords * 100 : 0;
        
        return new AttendanceStatisticsDto(
                null,
                "Overall Statistics",
                "GENERAL",
                startDate,
                endDate,
                totalRecords,
                presentCount,
                absentCount,
                lateCount,
                excusedCount,
                attendancePercentage,
                absencePercentage
        );
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

    // Teacher-specific attendance methods implementation

    @Override
    public List<AttendanceDto> getTeacherTodayScheduleWithAttendance(Long teacherId, LocalDate date) {
        log.debug("Getting today's schedule with attendance for teacher {} on date {}", teacherId, date);
        
        // Verify teacher exists
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        
        // Get teacher's timetable slots for today
        com.example.school_management.feature.operational.entity.enums.DayOfWeek dayOfWeek = convertToDayOfWeek(date.getDayOfWeek());
        List<TimetableSlot> allSlots = timetableSlotRepository.findByTeacherId(teacherId);
        log.info("Found {} total timetable slots for teacher {}", allSlots.size(), teacherId);
        
        // Log all slots for debugging
        for (TimetableSlot slot : allSlots) {
            log.info("Slot {}: day={}, class={}, course={}", slot.getId(), slot.getDayOfWeek(), 
                    slot.getForClass() != null ? slot.getForClass().getName() : "null",
                    slot.getForCourse() != null ? slot.getForCourse().getName() : "null");
        }
        
        List<TimetableSlot> todaySlots = allSlots.stream()
                .filter(slot -> slot.getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());
        
        log.info("Found {} timetable slots for teacher {} on {} (day: {})", todaySlots.size(), teacherId, date, dayOfWeek);
        
        List<AttendanceDto> result = new ArrayList<>();
        
        // If no timetable slots exist, create virtual slots from teacher's assigned classes
        if (todaySlots.isEmpty()) {
            log.info("No timetable slots found for teacher {} on {}, creating virtual slots from assigned classes", teacherId, date);
            
            // Get teacher's assigned classes through teaching assignments
            List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacherId);
            log.debug("Found {} teaching assignments for teacher {}", assignments.size(), teacherId);
            
            // Group assignments by class to avoid duplicates
            Map<Long, TeachingAssignment> classAssignments = assignments.stream()
                    .collect(Collectors.toMap(
                            ta -> ta.getClazz().getId(),
                            ta -> ta,
                            (existing, replacement) -> existing // Keep first if duplicate
                    ));
            
            for (TeachingAssignment assignment : classAssignments.values()) {
                // Create a virtual timetable slot for attendance marking
                AttendanceDto virtualSlot = new AttendanceDto();
                virtualSlot.setTimetableSlotId(-1L); // Use -1 to indicate virtual slot
                virtualSlot.setDate(date);
                virtualSlot.setStatus(AttendanceStatus.PRESENT); // Default status
                virtualSlot.setClassId(assignment.getClazz().getId());
                virtualSlot.setCourseId(assignment.getCourse().getId());
                virtualSlot.setClassName(assignment.getClazz().getName());
                virtualSlot.setCourseName(assignment.getCourse().getName());
                virtualSlot.setTeacherId(teacherId);
                virtualSlot.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName());
                result.add(virtualSlot);
                log.debug("Created virtual slot for class {} and course {}", assignment.getClazz().getName(), assignment.getCourse().getName());
            }
            
            log.info("Created {} virtual slots for teacher {} on {}", result.size(), teacherId, date);
        } else {
            // Process existing timetable slots
            for (TimetableSlot slot : todaySlots) {
                // Get existing attendance records for this slot
                List<Attendance> existingAttendance = attendanceRepository.findStudentAttendanceBySlotAndDate(slot.getId(), date);
                
                if (existingAttendance.isEmpty()) {
                    // Create placeholder attendance record for the slot
                    AttendanceDto slotDto = new AttendanceDto();
                    slotDto.setTimetableSlotId(slot.getId());
                    slotDto.setDate(date);
                    slotDto.setStatus(AttendanceStatus.PRESENT); // Default status
                    slotDto.setClassId(slot.getForClass() != null ? slot.getForClass().getId() : null);
                    slotDto.setCourseId(slot.getForCourse() != null ? slot.getForCourse().getId() : null);
                    slotDto.setClassName(slot.getForClass() != null ? slot.getForClass().getName() : null);
                    slotDto.setCourseName(slot.getForCourse() != null ? slot.getForCourse().getName() : null);
                    slotDto.setTeacherId(teacherId);
                    slotDto.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName());
                    result.add(slotDto);
                } else {
                    // Add existing attendance records
                    result.addAll(existingAttendance.stream()
                            .map(mapper::toAttendanceDto)
                            .collect(Collectors.toList()));
                }
            }
        }
        
        return result;
    }

    @Override
    public List<AttendanceDto> getStudentsForTimetableSlot(Long timetableSlotId, LocalDate date) {
        log.debug("Getting students for timetable slot {} on date {}", timetableSlotId, date);
        
        List<AttendanceDto> result = new ArrayList<>();
        
        // Handle virtual slots (when timetableSlotId is -1)
        if (timetableSlotId == -1L) {
            log.info("Handling virtual timetable slot for date {}", date);
            // For virtual slots, we need to get the class and course from the request context
            // This is a simplified approach - in a real scenario, you might pass class/course info
            throw new IllegalArgumentException("Virtual slots require additional context. Please use class-based attendance marking.");
        }
        
        TimetableSlot slot = timetableSlotRepository.findById(timetableSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
        
        if (slot.getForClass() == null) {
            throw new IllegalArgumentException("Timetable slot must have an associated class");
        }
        
        // Get all students in the class
        List<Student> students = new ArrayList<>(slot.getForClass().getStudents());
        
        // Check for existing attendance records
        List<Attendance> existingAttendance = attendanceRepository.findStudentAttendanceBySlotAndDate(timetableSlotId, date);
        Map<Long, Attendance> attendanceMap = existingAttendance.stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a));
        
        for (Student student : students) {
            AttendanceDto dto;
            if (attendanceMap.containsKey(student.getId())) {
                // Use existing attendance record
                dto = mapper.toAttendanceDto(attendanceMap.get(student.getId()));
            } else {
                // Create new attendance record with default status
                dto = new AttendanceDto();
                dto.setUserId(student.getId());
                dto.setTimetableSlotId(timetableSlotId);
                dto.setDate(date);
                dto.setStatus(AttendanceStatus.PRESENT); // Default to present
                dto.setUserType(UserType.STUDENT);
                dto.setClassId(slot.getForClass().getId());
                dto.setCourseId(slot.getForCourse() != null ? slot.getForCourse().getId() : null);
                dto.setClassName(slot.getForClass().getName());
                dto.setCourseName(slot.getForCourse() != null ? slot.getForCourse().getName() : null);
                dto.setUserName(student.getFirstName() + " " + student.getLastName());
            }
            result.add(dto);
        }
        
        return result;
    }

    @Override
    public List<AttendanceDto> markAttendanceForTimetableSlot(Long timetableSlotId, LocalDate date, List<AttendanceDto> attendanceList) {
        log.debug("Marking attendance for timetable slot {} on date {} for {} students", 
                timetableSlotId, date, attendanceList.size());
        
        TimetableSlot slot = timetableSlotRepository.findById(timetableSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
        
        BaseUser currentUser = getCurrentUser();
        List<AttendanceDto> result = new ArrayList<>();
        
        for (AttendanceDto attendanceDto : attendanceList) {
            // Validate student exists
            BaseUser student = userRepository.findById(attendanceDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            
            // Check for existing attendance record
            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByUserIdAndClassIdAndDate(attendanceDto.getUserId(), slot.getForClass().getId(), date);
            
            Attendance attendance;
            if (existingAttendance.isPresent()) {
                // Update existing record
                attendance = existingAttendance.get();
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setRemarks(attendanceDto.getRemarks());
                attendance.setExcuse(attendanceDto.getExcuse());
                attendance.setMedicalNote(attendanceDto.getMedicalNote());
            } else {
                // Create new record
                attendance = new Attendance();
                attendance.setUser(student);
                attendance.setTimetableSlot(slot);
                attendance.setDate(date);
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setUserType(UserType.STUDENT);
                attendance.setClassEntity(slot.getForClass());
                attendance.setCourse(slot.getForCourse());
                attendance.setRemarks(attendanceDto.getRemarks());
                attendance.setExcuse(attendanceDto.getExcuse());
                attendance.setMedicalNote(attendanceDto.getMedicalNote());
                attendance.setRecordedBy(currentUser);
                attendance.setRecordedAt(LocalDateTime.now());
            }
            
            Attendance savedAttendance = attendanceRepository.save(attendance);
            result.add(mapper.toAttendanceDto(savedAttendance));
        }
        
        log.info("Marked attendance for {} students in slot {} on date {}", result.size(), timetableSlotId, date);
        return result;
    }

    @Override
    public boolean canTeacherMarkAttendance(Long teacherId, Long timetableSlotId, LocalDate date) {
        log.debug("Checking if teacher {} can mark attendance for slot {} on date {}", teacherId, timetableSlotId, date);
        
        // Check if the timetable slot belongs to the teacher
        TimetableSlot slot = timetableSlotRepository.findById(timetableSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
        
        if (slot.getTeacher() == null || !slot.getTeacher().getId().equals(teacherId)) {
            return false;
        }
        
        // Check if the date matches the slot's day of week
        com.example.school_management.feature.operational.entity.enums.DayOfWeek dayOfWeek = convertToDayOfWeek(date.getDayOfWeek());
        if (slot.getDayOfWeek() != dayOfWeek) {
            return false;
        }
        
        // Additional business rules can be added here
        // For example: check if it's within the allowed time window
        
        return true;
    }

    @Override
    public Map<String, List<AttendanceDto>> getTeacherWeeklyAttendanceSummary(Long teacherId, LocalDate startOfWeek) {
        log.debug("Getting weekly attendance summary for teacher {} starting from {}", teacherId, startOfWeek);
        
        List<Attendance> weeklyAttendance = attendanceRepository.findStudentAttendanceByTeacherAndDate(teacherId, startOfWeek);
        
        Map<String, List<AttendanceDto>> summary = new HashMap<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            String dayKey = currentDate.getDayOfWeek().name();
            
            List<AttendanceDto> dayAttendance = weeklyAttendance.stream()
                    .filter(a -> a.getDate().equals(currentDate))
                    .map(mapper::toAttendanceDto)
                    .collect(Collectors.toList());
            
            summary.put(dayKey, dayAttendance);
        }
        
        return summary;
    }

    @Override
    public List<AttendanceDto> getAbsentStudentsForTeacher(Long teacherId, LocalDate date) {
        log.debug("Getting absent students for teacher {} on date {}", teacherId, date);
        
        List<Attendance> absentStudents = attendanceRepository.findAbsentStudentsByTeacherAndDate(teacherId, date);
        
        return absentStudents.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    // Helper method to convert Java DayOfWeek to our custom enum
    private com.example.school_management.feature.operational.entity.enums.DayOfWeek convertToDayOfWeek(java.time.DayOfWeek javaDayOfWeek) {
        switch (javaDayOfWeek) {
            case MONDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.MONDAY;
            case TUESDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.TUESDAY;
            case WEDNESDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.WEDNESDAY;
            case THURSDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.THURSDAY;
            case FRIDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.FRIDAY;
            case SATURDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.SATURDAY;
            case SUNDAY: return com.example.school_management.feature.operational.entity.enums.DayOfWeek.SUNDAY;
            default: throw new IllegalArgumentException("Invalid day of week: " + javaDayOfWeek);
        }
    }
    
    /**
     * Send absence notifications to student and their parents
     */
    private void sendAbsenceNotifications(Student student, Attendance attendance) {
        try {
            String studentName = student.getFirstName() + " " + student.getLastName();
            String className = attendance.getClassEntity() != null ? attendance.getClassEntity().getName() : "Unknown Class";
            String courseName = attendance.getCourse() != null ? attendance.getCourse().getName() : "General";
            String dateStr = attendance.getDate().toString();
            
            // Create notification title and message
            String title = "Absence Notification";
            String message = String.format("%s was marked absent from %s on %s", 
                studentName, courseName, dateStr);
            
            // Send notification to the student
            createNotificationForUser(student, title, message, attendance);
            
            // Find and notify all parents of this student
            List<Parent> parents = parentRepository.findByStudentId(student.getId());
            for (Parent parent : parents) {
                String parentMessage = String.format("Your child %s was marked absent from %s (%s) on %s", 
                    studentName, courseName, className, dateStr);
                createNotificationForUser(parent, title, parentMessage, attendance);
            }
            
            // Send real-time notifications
            if (!parents.isEmpty()) {
                Set<Long> parentIds = parents.stream().map(Parent::getId).collect(java.util.stream.Collectors.toSet());
                Set<Long> allUserIds = new java.util.HashSet<>(parentIds);
                allUserIds.add(student.getId());
                
                realTimeNotificationService.notifySpecificUsers(
                    title,
                    message,
                    "HIGH", // Absence notifications are high priority
                    allUserIds
                );
            }
            
            log.info("Sent absence notifications for student {} to {} parents", 
                studentName, parents.size());
                
        } catch (Exception e) {
            log.error("Failed to send absence notifications for student {}: {}", 
                student.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Create a database notification for a user
     */
    private void createNotificationForUser(BaseUser user, String title, String message, Attendance attendance) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(NotificationType.ATTENDANCE_MARKED);
        notification.setEntityType("ATTENDANCE");
        notification.setEntityId(attendance.getId());
        notification.setActionUrl("/attendance/" + attendance.getId());
        notification.setReadStatus(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        
        notificationRepository.save(notification);
    }
    
    // Class-based attendance methods (for virtual slots)
    
    @Override
    public List<AttendanceDto> getStudentsForClass(Long classId, LocalDate date) {
        log.debug("Getting students for class {} on date {}", classId, date);
        
        // Get class entity
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        // Get all students in the class through enrollments (the correct way)
        List<Student> students = studentRepository.findByClassIds(List.of(classId));
        
        // Check for existing attendance records for this class on this date
        List<Attendance> existingAttendance = attendanceRepository.findByClassIdAndDate(classId, date);
        Map<Long, Attendance> attendanceMap = existingAttendance.stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a));
        
        List<AttendanceDto> result = new ArrayList<>();
        
        for (Student student : students) {
            AttendanceDto dto;
            if (attendanceMap.containsKey(student.getId())) {
                // Use existing attendance record
                dto = mapper.toAttendanceDto(attendanceMap.get(student.getId()));
            } else {
                // Create new attendance record with default status
                dto = new AttendanceDto();
                dto.setUserId(student.getId());
                dto.setTimetableSlotId(-1L); // Virtual slot
                dto.setDate(date);
                dto.setStatus(AttendanceStatus.PRESENT); // Default to present
                dto.setUserType(UserType.STUDENT);
                dto.setClassId(classId);
                dto.setClassName(classEntity.getName());
                dto.setUserName(student.getFirstName() + " " + student.getLastName());
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<AttendanceDto> getStudentsForClassSimple(Long classId) {
        log.debug("Getting students for class {} (simple)", classId);
        
        // Get class entity
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        log.info("🔍 Class found: {} (ID: {})", classEntity.getName(), classId);
        
        // Get all students in the class through enrollments (the correct way)
        List<Student> students = studentRepository.findByClassIds(List.of(classId));
        log.info("🔍 Found {} students for class {} through enrollments", students.size(), classId);
        
        // Also check direct relationship for comparison
        List<Student> directStudents = new ArrayList<>(classEntity.getStudents());
        log.info("🔍 Direct class.students relationship has {} students", directStudents.size());
        
        // Check enrollments directly
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        log.info("🔍 Found {} active enrollments for class {}", enrollments.size(), classId);
        
        // Convert to AttendanceDto format for consistency
        List<AttendanceDto> result = new ArrayList<>();
        
        for (Student student : students) {
            log.debug("🔍 Processing student: {} {} (ID: {})", student.getFirstName(), student.getLastName(), student.getId());
            AttendanceDto dto = new AttendanceDto();
            dto.setUserId(student.getId());
            dto.setTimetableSlotId(-1L); // Virtual slot
            dto.setDate(LocalDate.now()); // Default to today
            dto.setStatus(AttendanceStatus.PRESENT); // Default to present
            dto.setUserType(UserType.STUDENT);
            dto.setClassId(classId);
            dto.setClassName(classEntity.getName());
            dto.setUserName(student.getFirstName() + " " + student.getLastName());
            result.add(dto);
        }
        
        log.info("🔍 Returning {} attendance records for class {}", result.size(), classId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAttendanceClassView getTeacherAttendanceClass(Long teacherId, Long classId, Long courseId) {
        log.debug("Getting attendance class view for teacher: {}, class: {}, course: {}", teacherId, classId, courseId);
        
        // Verify teaching assignment exists (similar to grade system)
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacherId);
        TeachingAssignment assignment = assignments.stream()
                .filter(ta -> ta.getClazz().getId().equals(classId) && ta.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Teaching assignment not found for teacher " + teacherId + ", class " + classId + ", course " + courseId));
        
        // Get all students enrolled in this class
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        
        List<TeacherAttendanceClassView.TeacherAttendanceStudent> students = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            // Calculate attendance rate for this student
            Double attendanceRate = calculateStudentAttendanceRate(enrollment.getStudent().getId());
            
            // Get last attendance date
            String lastAttendanceDate = getLastAttendanceDate(enrollment.getStudent().getId());
            
            TeacherAttendanceClassView.TeacherAttendanceStudent student = TeacherAttendanceClassView.TeacherAttendanceStudent.builder()
                    .studentId(enrollment.getStudent().getId())
                    .firstName(enrollment.getStudent().getFirstName())
                    .lastName(enrollment.getStudent().getLastName())
                    .email(enrollment.getStudent().getEmail())
                    .enrollmentId(enrollment.getId())
                    .currentStatus(AttendanceStatus.PRESENT) // Default status
                    .attendanceRate(attendanceRate)
                    .lastAttendanceDate(lastAttendanceDate)
                    .build();
            
            students.add(student);
        }
        
        return TeacherAttendanceClassView.builder()
                .classId(classId)
                .className(assignment.getClazz().getName())
                .courseId(courseId)
                .courseName(assignment.getCourse().getName())
                .courseCode(assignment.getCourse().getCode())
                .coefficient(assignment.getCourse().getCredit() != null ? assignment.getCourse().getCredit().doubleValue() : 1.0)
                .semester("FIRST") // Default semester
                .attendanceTypes(Arrays.asList("PRESENT", "ABSENT", "LATE", "EXCUSED"))
                .students(students)
                .build();
    }
    
    @Override
    @Transactional
    public List<AttendanceDto> markAttendanceForClass(Long classId, LocalDate date, List<AttendanceDto> attendanceList) {
        log.debug("Marking attendance for class {} on date {} for {} students", classId, date, attendanceList.size());
        
        // Verify class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        BaseUser currentUser = getCurrentUser();
        List<AttendanceDto> result = new ArrayList<>();
        
        for (AttendanceDto attendanceDto : attendanceList) {
            // Verify student is in the class
            Student student = studentRepository.findById(attendanceDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            
            if (!classEntity.getStudents().contains(student)) {
                throw new IllegalArgumentException("Student " + student.getId() + " is not enrolled in class " + classId);
            }
            
            // Check if attendance record already exists
            Attendance existingAttendance = attendanceRepository.findByUserIdAndClassIdAndDate(
                    attendanceDto.getUserId(), classId, date).orElse(null);
            
            Attendance attendance;
            if (existingAttendance != null) {
                // Update existing record
                attendance = existingAttendance;
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setRemarks(attendanceDto.getRemarks());
                attendance.setExcuse(attendanceDto.getExcuse());
                attendance.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new record
                attendance = new Attendance();
                attendance.setUser(student);
                attendance.setDate(date);
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setUserType(UserType.STUDENT);
                attendance.setClassEntity(classEntity);
                attendance.setRemarks(attendanceDto.getRemarks());
                attendance.setExcuse(attendanceDto.getExcuse());
                attendance.setRecordedAt(LocalDateTime.now());
                attendance.setRecordedBy(currentUser);
            }
            
            Attendance savedAttendance = attendanceRepository.save(attendance);
            AttendanceDto savedDto = mapper.toAttendanceDto(savedAttendance);
            result.add(savedDto);
            
            // Send notifications for absent students
            if (savedAttendance.getStatus() == AttendanceStatus.ABSENT) {
                sendAbsenceNotifications(student, savedAttendance);
            }
        }
        
        return result;
    }
    
    // Helper methods for attendance class view
    private Double calculateStudentAttendanceRate(Long studentId) {
        // Calculate attendance rate for a student (simplified implementation)
        List<Attendance> studentAttendance = attendanceRepository.findByUserIdAndUserType(studentId, UserType.STUDENT);
        if (studentAttendance.isEmpty()) {
            return 100.0; // Default to 100% if no attendance records
        }
        
        long presentCount = studentAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();
        
        return (double) presentCount / studentAttendance.size() * 100.0;
    }
    
    private String getLastAttendanceDate(Long studentId) {
        // Get the last attendance date for a student
        List<Attendance> studentAttendance = attendanceRepository.findByUserIdAndUserTypeOrderByDateDesc(studentId, UserType.STUDENT);
        if (studentAttendance.isEmpty()) {
            return null;
        }
        
        return studentAttendance.get(0).getDate().toString();
    }
} 