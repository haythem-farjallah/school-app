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
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.communication.entity.Notification;
import com.example.school_management.feature.communication.repository.CommunicationNotificationRepository;
import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.communication.service.impl.EmailServiceImpl;
import com.example.school_management.feature.communication.dto.EmailRequest;
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
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final CommunicationNotificationRepository communicationNotificationRepository;
    private final EmailService emailService;
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
        if (savedAttendance.getStatus() == AttendanceStatus.ABSENT) {
            sendAbsenceNotifications(user, savedAttendance);
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
        
        // Validate access permissions for parents
        validateUserAccess(userId);
        
        List<Attendance> attendances = attendanceRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return attendances.stream()
                .map(attendance -> {
                    AttendanceDto dto = mapper.toAttendanceDto(attendance);
                    
                    // Manually populate the fields that were ignored in the mapper
                    if (attendance.getUser() != null) {
                        dto.setUserName(attendance.getUser().getFirstName() + " " + attendance.getUser().getLastName());
                    }
                    if (attendance.getCourse() != null) {
                        dto.setCourseName(attendance.getCourse().getName());
                    }
                    if (attendance.getClassEntity() != null) {
                        dto.setClassName(attendance.getClassEntity().getName());
                    }
                    if (attendance.getRecordedBy() != null) {
                        dto.setRecordedByName(attendance.getRecordedBy().getFirstName() + " " + attendance.getRecordedBy().getLastName());
                    }
                    
                    return dto;
                })
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
        
        // Group attendance by date to calculate daily attendance rates (PER COURSE CALCULATION)
        Map<LocalDate, List<Attendance>> attendanceByDate = attendances.stream()
                .collect(Collectors.groupingBy(Attendance::getDate));
        
        double totalAttendanceRate = 0.0;
        int totalDaysWithAttendance = 0;
        long totalAbsentCourses = 0;
        long totalPresentCourses = 0;
        long totalLateCourses = 0;
        long totalExcusedCourses = 0;
        long totalCoursesTracked = attendances.size();
        
        for (Map.Entry<LocalDate, List<Attendance>> entry : attendanceByDate.entrySet()) {
            List<Attendance> dayAttendance = entry.getValue();
            
            long presentCourses = dayAttendance.stream().filter(Attendance::isPresent).count();
            long absentCourses = dayAttendance.stream().filter(Attendance::isAbsent).count();
            long lateCourses = dayAttendance.stream().filter(Attendance::isLate).count();
            long excusedCourses = dayAttendance.stream().filter(Attendance::isExcused).count();
            
            totalPresentCourses += presentCourses;
            totalAbsentCourses += absentCourses;
            totalLateCourses += lateCourses;
            totalExcusedCourses += excusedCourses;
            
            // Calculate daily attendance rate (present courses / total courses that day)
            double dailyAttendanceRate = dayAttendance.size() > 0 ? 
                (double) presentCourses / dayAttendance.size() * 100 : 0.0;
            
            totalAttendanceRate += dailyAttendanceRate;
            totalDaysWithAttendance++;
        }
        
        // Calculate overall attendance rate (average of daily rates)
        double attendancePercentage = totalDaysWithAttendance > 0 ? 
            totalAttendanceRate / totalDaysWithAttendance : 0.0;
        double absencePercentage = totalCoursesTracked > 0 ? 
            (double) totalAbsentCourses / totalCoursesTracked * 100 : 0.0;
        
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return new AttendanceStatisticsDto(
                userId,
                user.getFirstName() + " " + user.getLastName(),
                attendances.isEmpty() ? "UNKNOWN" : attendances.get(0).getUserType().name(),
                startDate,
                endDate,
                (long) totalDaysWithAttendance, // Total days with attendance records
                totalPresentCourses, // Total present courses
                totalAbsentCourses, // Total absent courses
                totalLateCourses, // Total late courses
                totalExcusedCourses, // Total excused courses
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
    private void sendAbsenceNotifications(BaseUser student, Attendance attendance) {
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
     * Create a database notification for a user using the communication system
     */
    private void createNotificationForUser(BaseUser user, String title, String message, Attendance attendance) {
        try {
            // Determine recipient type based on user role
            Notification.RecipientType recipientType = getRecipientType(user);
            
            // Create metadata - handle null attendance for parent notifications
            String metadata;
            if (attendance != null) {
                metadata = String.format("{\"attendanceId\":%d,\"entityType\":\"ATTENDANCE\"}", attendance.getId());
            } else {
                metadata = "{\"entityType\":\"ATTENDANCE_ALERT\"}";
            }
            
            // Create notification using standard constructor and setters to avoid Hibernate issues
            Notification notification = new Notification();
            notification.setRecipientId(user.getId());
            notification.setRecipientType(recipientType);
            notification.setNotificationType(Notification.NotificationType.ATTENDANCE_ALERT);
            notification.setChannel(Notification.NotificationChannel.IN_APP);
            notification.setTitle(title);
            notification.setContent(message);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification.setPriority(Notification.Priority.HIGH); // Attendance alerts are high priority
            notification.setMetadata(metadata);
            // CreatedAt and UpdatedAt will be set automatically by @CreatedDate and @LastModifiedDate
            
            Notification savedNotification = communicationNotificationRepository.save(notification);
            log.info("📧 Created communication notification ID {} for user: {} ({})", 
                savedNotification.getId(), user.getEmail(), recipientType);
            
            // Send real-time notification via WebSocket - DISABLED (only use notification bell)
            // sendRealTimeNotification(user, title, message, savedNotification.getId());
            
        } catch (Exception e) {
            log.error("❌ Failed to create notification for user {}: {}", user.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Send real-time notification via WebSocket
     */
    private void sendRealTimeNotification(BaseUser user, String title, String message, Long notificationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", notificationId);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("type", "ATTENDANCE_ALERT");
            payload.put("priority", "HIGH");
            payload.put("timestamp", LocalDateTime.now().toString());
            
            // Send to specific user via real-time notification service
            realTimeNotificationService.notifySpecificUsers(
                title,
                message,
                "HIGH",
                Set.of(user.getId())
            );
            
            log.info("📡 Real-time notification sent to user: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("❌ Failed to send real-time notification to user {}: {}", user.getId(), e.getMessage());
        }
    }
    
    /**
     * Helper method to determine recipient type from user
     */
    private Notification.RecipientType getRecipientType(BaseUser user) {
        if (user instanceof Student) {
            return Notification.RecipientType.STUDENT;
        } else if (user instanceof Teacher) {
            return Notification.RecipientType.TEACHER;
        } else if (user instanceof Parent) {
            return Notification.RecipientType.PARENT;
        } else {
            return Notification.RecipientType.STAFF; // Default fallback
        }
    }
    
    // Class-based attendance methods (for virtual slots)
    
    @Override
    public List<AttendanceDto> getStudentsForClass(Long classId, LocalDate date) {
        log.debug("Getting students for class {} on date {}", classId, date);
        
        // Get class entity
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        // Get all students in the class through enrollments ONLY (the correct way)
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        List<Student> students = enrollments.stream()
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
        
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
        log.info("🚀 START: Getting students for class {} (simple)", classId);
        
        // Get class entity
        log.info("🔍 STEP 1: Looking for class with ID: {}", classId);
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        log.info("✅ STEP 1 SUCCESS: Class found: {} (ID: {})", classEntity.getName(), classId);
        
        // Get all students in the class through enrollments ONLY (the correct way)
        log.info("🔍 STEP 2: Querying enrollments for class {} with status ACTIVE", classId);
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        log.info("✅ STEP 2 SUCCESS: Found {} active enrollments for class {}", enrollments.size(), classId);
        
        // Log each enrollment in detail
        for (int i = 0; i < enrollments.size(); i++) {
            Enrollment enrollment = enrollments.get(i);
            log.info("📋 ENROLLMENT {}: ID={}, StudentID={}, ClassID={}, Status={}", 
                i + 1, enrollment.getId(), 
                enrollment.getStudent() != null ? enrollment.getStudent().getId() : "NULL",
                enrollment.getClassEntity() != null ? enrollment.getClassEntity().getId() : "NULL", 
                enrollment.getStatus());
        }
        
        if (enrollments.isEmpty()) {
            log.warn("⚠️ No active enrollments found for class {}. Returning empty list.", classId);
            return new ArrayList<>();
        }
        
        // Get student IDs from enrollments with error handling
        log.info("🔍 STEP 3: Extracting student IDs from {} enrollments", enrollments.size());
        List<Long> studentIds = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            try {
                if (enrollment.getStudent() != null) {
                    Long studentId = enrollment.getStudent().getId();
                    studentIds.add(studentId);
                    log.info("✅ STEP 3.{}: Added student ID {} from enrollment {}", 
                        studentIds.size(), studentId, enrollment.getId());
                } else {
                    log.warn("⚠️ STEP 3.{}: Enrollment {} has null student reference", 
                        studentIds.size() + 1, enrollment.getId());
                }
            } catch (Exception e) {
                log.error("❌ STEP 3.{}: Error accessing student from enrollment {}: {}", 
                    studentIds.size() + 1, enrollment.getId(), e.getMessage());
            }
        }
        log.info("✅ STEP 3 SUCCESS: Student IDs from enrollments: {}", studentIds);
        
        if (studentIds.isEmpty()) {
            log.warn("⚠️ STEP 3 FAILED: No valid student IDs found from enrollments for class {}. Returning empty list.", classId);
            return new ArrayList<>();
        }
        
        // Fetch students by IDs to avoid lazy loading issues
        log.info("🔍 STEP 4: Fetching {} users by IDs: {}", studentIds.size(), studentIds);
        List<BaseUser> users = userRepository.findAllById(studentIds);
        log.info("✅ STEP 4 SUCCESS: Found {} users for student IDs", users.size());
        
        // Log each user found
        for (int i = 0; i < users.size(); i++) {
            BaseUser user = users.get(i);
            log.info("👤 USER {}: ID={}, Name={} {}, Email={}, Role={}", 
                i + 1, user.getId(), user.getFirstName(), user.getLastName(), 
                user.getEmail(), user.getRole());
        }
        
        // Convert to AttendanceDto format for consistency
        log.info("🔍 STEP 5: Converting {} users to AttendanceDto format", users.size());
        List<AttendanceDto> result = new ArrayList<>();
        
        for (int i = 0; i < users.size(); i++) {
            BaseUser user = users.get(i);
            log.info("🔍 STEP 5.{}: Processing user {}", i + 1, user != null ? user.getId() : "NULL");
            
            if (user == null) {
                log.warn("⚠️ STEP 5.{}: Skipping null user", i + 1);
                continue;
            }
            
            if (!user.getRole().name().equals("STUDENT")) {
                log.warn("⚠️ STEP 5.{}: Skipping non-student user: {} (Role: {})", 
                    i + 1, user.getId(), user.getRole());
                continue;
            }
            
            log.info("✅ STEP 5.{}: Processing enrolled student: {} {} (ID: {})", 
                i + 1, user.getFirstName(), user.getLastName(), user.getId());
            
            AttendanceDto dto = new AttendanceDto();
            dto.setUserId(user.getId());
            dto.setTimetableSlotId(-1L); // Virtual slot
            dto.setDate(LocalDate.now()); // Default to today
            dto.setStatus(AttendanceStatus.PRESENT); // Default to present
            dto.setUserType(UserType.STUDENT);
            dto.setClassId(classId);
            dto.setClassName(classEntity.getName());
            dto.setUserName(user.getFirstName() + " " + user.getLastName());
            result.add(dto);
            
            log.info("✅ STEP 5.{}: Created attendance DTO for student: {} (Total DTOs: {})", 
                i + 1, user.getFirstName(), result.size());
        }
        
        log.info("🎯 FINAL RESULT: Returning {} attendance records for class {}", result.size(), classId);
        return result;
    }

    @Override
    @Transactional
    public List<AttendanceDto> markEnhancedAttendanceForClass(Long classId, LocalDate date, String time, 
            String course, Long teacherId, List<AttendanceDto> attendanceList) {
        log.info("🎯 ENHANCED ATTENDANCE: Marking attendance for class {} on {} at {} for course {}", 
            classId, date, time, course);
        
        // Find or create a course entity for the attendance records
        Course courseEntity = findOrCreateCourse(course);
        log.info("📚 Using course: {} (ID: {})", courseEntity.getName(), courseEntity.getId());
        
        // First, save the attendance records with course information
        List<AttendanceDto> savedAttendance = markAttendanceForClassWithCourse(classId, date, courseEntity, attendanceList);
        log.info("✅ Saved {} attendance records", savedAttendance.size());
        
        // Send notifications for non-present students
        sendParentNotifications(classId, date, time, course, teacherId, attendanceList);
        
        // Add to admin/teacher feed for excused students
        addToFeedForExcusedStudents(classId, date, time, course, teacherId, attendanceList);
        
        return savedAttendance;
    }

    /**
     * Find a course by name or create/use a default one
     */
    private Course findOrCreateCourse(String courseName) {
        try {
            // Try to find an existing course with similar name
            if (courseName != null && !courseName.trim().isEmpty()) {
                List<Course> allCourses = courseRepository.findAll();
                for (Course course : allCourses) {
                    if (course.getName().toLowerCase().contains(courseName.toLowerCase()) ||
                        courseName.toLowerCase().contains(course.getName().toLowerCase())) {
                        log.info("📚 Found matching course: {} for search term: {}", course.getName(), courseName);
                        return course;
                    }
                }
            }
            
            // If no match found, use the first available course as default
            List<Course> allCourses = courseRepository.findAll();
            if (!allCourses.isEmpty()) {
                Course defaultCourse = allCourses.get(0);
                log.info("📚 Using default course: {} for course name: {}", defaultCourse.getName(), courseName);
                return defaultCourse;
            }
            
            // If no courses exist at all, this would be a system error
            throw new ResourceNotFoundException("No courses found in the system");
            
        } catch (Exception e) {
            log.error("❌ Error finding course, using first available: {}", e.getMessage());
            // Fallback: get the first course
            return courseRepository.findAll().get(0);
        }
    }

    /**
     * Mark attendance for all students in a class with course information
     */
    private List<AttendanceDto> markAttendanceForClassWithCourse(Long classId, LocalDate date, Course courseEntity, List<AttendanceDto> attendanceList) {
        log.debug("Marking attendance for class {} on date {} for course {}", classId, date, courseEntity.getName());
        
        // Verify class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        BaseUser currentUser = getCurrentUser();
        List<AttendanceDto> result = new ArrayList<>();
        
        for (AttendanceDto attendanceDto : attendanceList) {
            // Verify student is enrolled in the class using enrollments table
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndClassIdAndStatus(
                attendanceDto.getUserId(), classId, EnrollmentStatus.ACTIVE);
            
            if (!isEnrolled) {
                throw new IllegalArgumentException("Student " + attendanceDto.getUserId() + " is not enrolled in class " + classId);
            }
            
            // Check if attendance record already exists
            Attendance existingAttendance = attendanceRepository.findByUserIdAndClassIdAndDate(
                    attendanceDto.getUserId(), classId, date).orElse(null);
            
            // Get the student/user for this attendance record
            BaseUser student = userRepository.findById(attendanceDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            
            Attendance attendance;
            if (existingAttendance != null) {
                // Update existing record
                attendance = existingAttendance;
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setRemarks(attendanceDto.getRemarks());
                attendance.setExcuse(attendanceDto.getExcuse());
                attendance.setCourse(courseEntity); // Set the course
                attendance.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new record
                attendance = new Attendance();
                attendance.setUser(student);
                attendance.setDate(date);
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setUserType(UserType.STUDENT);
                attendance.setClassEntity(classEntity);
                attendance.setCourse(courseEntity); // Set the course
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

    /**
     * Send notification to a specific parent about their child's attendance
     */
    private void sendNotificationToParent(Parent parent, BaseUser student, AttendanceStatus status, 
            String className, String course, LocalDate date, String time, String teacherName) {
        try {
            String statusText = switch (status) {
                case ABSENT -> "was absent from";
                case LATE -> "was late to";
                case EXCUSED -> "was excused from";
                default -> "attended";
            };
            
            String subject = String.format("Attendance Update: %s %s %s class", 
                student.getFirstName() + " " + student.getLastName(), statusText, className);
            
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "This is to inform you that your child %s %s %s class on %s at %s.\n\n" +
                "Class: %s\n" +
                "Course: %s\n" +
                "Date: %s\n" +
                "Time: %s\n" +
                "Status: %s\n" +
                "Recorded by: %s\n\n" +
                "If you have any questions, please contact the school.\n\n" +
                "Best regards,\n" +
                "School Administration",
                parent.getFirstName(),
                student.getFirstName(), student.getLastName(), statusText, date, time,
                className, course, date, time, status.toString(), teacherName
            );
            
            // ALWAYS create in-app notification
            String inAppMessage = String.format(
                "Your child %s %s %s class (%s) on %s", 
                student.getFirstName(), statusText, className, course, date
            );
            
            // Create database notification for parent
            // Note: We pass null for attendance since this is a parent notification, not student attendance
            createNotificationForUser(parent, subject, inAppMessage, null);
            log.info("📱 IN-APP NOTIFICATION CREATED for parent: {} ({})", parent.getFirstName(), parent.getEmail());
            
            // CONDITIONAL EMAIL - Only send if parent has email as preferred contact method
            var contactMethod = parent.getPreferredContactMethod();
            if (contactMethod != null && contactMethod.name().equals("EMAIL")) {
                // Send email notification with parent ID to avoid null constraint
                sendEmailToParent(parent.getEmail(), subject, emailBody, parent.getId());
                log.info("📧 EMAIL SENT to parent: {} ({}) - Contact method: {}", parent.getFirstName(), parent.getEmail(), contactMethod);
            } else {
                log.info("📧 EMAIL SKIPPED for parent: {} - Contact method: {}", parent.getFirstName(), contactMethod);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to send notification to parent {}: {}", 
                parent.getEmail(), e.getMessage(), e);
        }
    }
    
    /**
     * Send email to parent using the communication email service
     */
    private void sendEmailToParent(String email, String subject, String body, Long parentId) {
        try {
            // Use EmailServiceImpl directly to access sendEmailWithRecipientId
            if (emailService instanceof com.example.school_management.feature.communication.service.impl.EmailServiceImpl) {
                com.example.school_management.feature.communication.service.impl.EmailServiceImpl emailServiceImpl = 
                    (com.example.school_management.feature.communication.service.impl.EmailServiceImpl) emailService;
                
                // Create email request
                EmailRequest emailRequest = EmailRequest.builder()
                    .recipientEmail(email)
                    .subject(subject)
                    .content(body)
                    .isHtml(false) // Plain text email
                    .enableTracking(true)
                    .priority(Notification.Priority.HIGH)
                    .build();
                
                // Send email with recipient ID to avoid null constraint
                var emailResponse = emailServiceImpl.sendEmailWithRecipientId(emailRequest, parentId);
                
                if (emailResponse.getSuccess()) {
                    log.info("📧 EMAIL SENT SUCCESSFULLY to parent: {} - MessageID: {}", email, emailResponse.getMessageId());
                } else {
                    log.error("❌ EMAIL FAILED to parent: {} - Error: {}", email, emailResponse.getErrorMessage());
                }
            } else {
                log.error("❌ EmailService is not instance of EmailServiceImpl, cannot send email with recipient ID");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", email, e.getMessage(), e);
        }
    }

    private void sendParentNotifications(Long classId, LocalDate date, String time, String course, 
            Long teacherId, List<AttendanceDto> attendanceList) {
        try {
            log.info("📨 Sending parent notifications for non-present students");
            
            // Get class name
            ClassEntity classEntity = classRepository.findById(classId)
                .orElse(null);
            String className = classEntity != null ? classEntity.getName() : "Class " + classId;
            
            // Get teacher name
            BaseUser teacher = userRepository.findById(teacherId)
                .orElse(null);
            String teacherName = teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : "Teacher";
            
            for (AttendanceDto attendance : attendanceList) {
                // Skip present students
                if (attendance.getStatus() == AttendanceStatus.PRESENT) {
                    continue;
                }
                
                try {
                    // Get student
                    BaseUser student = userRepository.findById(attendance.getUserId())
                        .orElse(null);
                    
                    if (student == null) {
                        log.warn("⚠️ Student not found: {}", attendance.getUserId());
                        continue;
                    }
                    
                    // Find parents using the repository method - pass child ID, get parents list
                    List<Parent> parents = parentRepository.findByStudentId(student.getId());
                    
                    if (!parents.isEmpty()) {
                        log.info("✅ Found {} parent(s) for student: {} {}", parents.size(), student.getFirstName(), student.getLastName());
                        
                        // Send notifications to all parents
                        for (Parent parent : parents) {
                            sendNotificationToParent(parent, student, attendance.getStatus(), 
                                className, course, date, time, teacherName);
                            log.info("📧 Sent notification to parent: {} ({})", parent.getFirstName() + " " + parent.getLastName(), parent.getEmail());
                        }
                    } else {
                        log.info("📝 No parent found for student: {} {} (ID: {})", student.getFirstName(), student.getLastName(), student.getId());
                    }
                    
                } catch (Exception e) {
                    log.error("❌ Error sending notification for student {}: {}", attendance.getUserId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error in sendParentNotifications: {}", e.getMessage(), e);
        }
    }


    private void addToFeedForExcusedStudents(Long classId, LocalDate date, String time, String course, 
            Long teacherId, List<AttendanceDto> attendanceList) {
        try {
            List<AttendanceDto> excusedStudents = attendanceList.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.EXCUSED)
                .collect(Collectors.toList());
            
            if (!excusedStudents.isEmpty()) {
                log.info("📋 Adding {} excused students to admin/teacher feed", excusedStudents.size());
                
                // Create feed entries for excused students
                for (AttendanceDto attendance : excusedStudents) {
                    BaseUser student = userRepository.findById(attendance.getUserId()).orElse(null);
                    if (student != null) {
                        String feedMessage = String.format(
                            "Student %s %s was excused from %s class on %s at %s",
                            student.getFirstName(), student.getLastName(), course, date, time
                        );
                        
                        // feedService.addToAdminFeed(feedMessage);
                        // feedService.addToTeacherFeed(teacherId, feedMessage);
                        log.info("📋 FEED ENTRY: {}", feedMessage);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error adding to feed: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> debugEnrollmentQuery(Long classId) {
        log.info("🔧 DEBUG: Testing enrollment repository query for class {}", classId);
        
        try {
            List<Object[]> results = enrollmentRepository.findEnrollmentDebugData(classId);
            log.info("🔧 DEBUG: Raw query returned {} rows", results.size());
            
            for (int i = 0; i < results.size(); i++) {
                Object[] row = results.get(i);
                log.info("🔧 DEBUG ROW {}: enrollmentId={}, studentId={}, classId={}, status={}, firstName={}, lastName={}", 
                    i + 1, row[0], row[1], row[2], row[3], row[4], row[5]);
            }
            
            return results;
        } catch (Exception e) {
            log.error("🔧 DEBUG ERROR: {}", e.getMessage(), e);
            throw e;
        }
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
            // Verify student is enrolled in the class using enrollments table
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndClassIdAndStatus(
                attendanceDto.getUserId(), classId, EnrollmentStatus.ACTIVE);
            
            if (!isEnrolled) {
                throw new IllegalArgumentException("Student " + attendanceDto.getUserId() + " is not enrolled in class " + classId);
            }
            
            // Check if attendance record already exists
            Attendance existingAttendance = attendanceRepository.findByUserIdAndClassIdAndDate(
                    attendanceDto.getUserId(), classId, date).orElse(null);
            
            // Get the student/user for this attendance record
            BaseUser student = userRepository.findById(attendanceDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            
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
        // For now, return a default rate since we don't have the specific query
        // TODO: Implement proper attendance rate calculation when needed
        return 85.0; // Default attendance rate
    }
    
    private String getLastAttendanceDate(Long studentId) {
        // For now, return today's date as default
        // TODO: Implement proper last attendance date lookup when needed
        return LocalDate.now().toString();
    }
    
    /**
     * Validates that the current user has permission to access the requested user's attendance data.
     * - Admins, Staff, and Teachers can access any user's data
     * - Students can only access their own data
     * - Parents can only access their children's data
     */
    private void validateUserAccess(Long requestedUserId) {
        BaseUser currentUser = getCurrentUser();
        
        // Admins, Staff, and Teachers have access to all attendance data
        if (currentUser instanceof Teacher || 
            "ADMIN".equals(currentUser.getRole().name()) || 
            "STAFF".equals(currentUser.getRole().name())) {
            return;
        }
        
        // Students can only access their own attendance
        if (currentUser instanceof Student) {
            if (!currentUser.getId().equals(requestedUserId)) {
                throw new SecurityException("Students can only access their own attendance data");
            }
            return;
        }
        
        // Parents can only access their children's attendance
        if (currentUser instanceof Parent) {
            Parent parent = (Parent) currentUser;
            boolean isChild = parent.getChildren().stream()
                    .anyMatch(child -> child.getId().equals(requestedUserId));
            
            if (!isChild) {
                throw new SecurityException("Parents can only access their children's attendance data");
            }
            return;
        }
        
        // If we reach here, the user type is not recognized or not allowed
        throw new SecurityException("Access denied: Insufficient permissions to view attendance data");
    }
} 