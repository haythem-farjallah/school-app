package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.auth.entity.UserRole;
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
import org.springframework.security.access.AccessDeniedException;
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
import com.example.school_management.commons.utils.FilterFields;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    /** Paths accepted by GET /api/v1/attendance/filter; its sortable paths also bound GET /type/{userType}. */
    private static final FilterFields FILTER_FIELDS = new FilterFields(
            Set.of("user.id", "classId", "course.id", "date", "status", "userType"),
            Set.of("date", "status", "recordedAt"));

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
    private final CurrentSchoolResolver currentSchool;
    private final SchoolMembershipRepository memberships;

    private BaseUser getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @Override
    public AttendanceDto recordAttendance(AttendanceDto attendanceDto) {
        log.debug("Recording attendance for user: {}", attendanceDto.getUserId());
        School school = currentSchool.resolve();
        BaseUser user = requireAttendanceUser(attendanceDto);
        Attendance attendance = new Attendance();
        attendance.setSchool(school);
        attendance.setUser(user);
        attendance.setUserType(attendanceDto.getUserType());
        attendance.setDate(attendanceDto.getDate());
        attendance.setStatus(attendanceDto.getStatus());
        attendance.setRemarks(attendanceDto.getRemarks());
        attendance.setExcuse(attendanceDto.getExcuse());
        attendance.setMedicalNote(attendanceDto.getMedicalNote());
        attendance.setRecordedAt(LocalDateTime.now());
        applyAttendanceContext(attendance, attendanceDto, attendanceDto.getTimetableSlotId());

        // Validate every supplied resource before an existing row can short-circuit the write.
        Attendance existingAttendance = findExistingAttendance(attendance);
        if (existingAttendance != null) {
            log.warn("Attendance record already exists for user {} on date {}",
                    attendanceDto.getUserId(), attendanceDto.getDate());
            return mapper.toAttendanceDto(existingAttendance);
        }
        attendance.setRecordedBy(getCurrentUser());
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Attendance recorded for user {} on date {}", user.getId(), attendanceDto.getDate());
        if (user instanceof Student student && savedAttendance.isAbsent()) {
            sendAbsenceNotifications(student, savedAttendance);
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
        
        requireSchoolUserForRead(userId);
        List<Attendance> attendances = attendanceRepository.findByUserIdAndDateBetweenAndSchoolId(userId, startDate, endDate, currentSchool.resolve().getId());
        return attendances.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDto> getClassAttendance(Long classId, LocalDate date) {
        log.debug("Getting class attendance for class {} on date {}", classId, date);
        
        requireSchoolClass(classId);
        List<Attendance> attendances = attendanceRepository.findByClassIdAndDateAndSchoolId(classId, date, currentSchool.resolve().getId());
        return attendances.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDto> getCourseAttendance(Long courseId, LocalDate date) {
        log.debug("Getting course attendance for course {} on date {}", courseId, date);
        
        requireSchoolCourse(courseId);
        List<Attendance> attendances = attendanceRepository.findByCourseIdAndDateAndSchoolId(courseId, date, currentSchool.resolve().getId());
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
        
        List<Attendance> attendances = attendanceRepository.findByDateBetweenAndSchoolId(startDate, endDate, currentSchool.resolve().getId());
        
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
        
        BaseUser user = requireSchoolUserForRead(userId);
        List<Attendance> attendances = attendanceRepository.findByUserIdAndDateBetweenAndSchoolId(
                userId, startDate, endDate, currentSchool.resolve().getId());
        return userStatistics(user, attendances, startDate, endDate);
    }

    private AttendanceStatisticsDto userStatistics(BaseUser user, List<Attendance> attendances,
                                                   LocalDate startDate, LocalDate endDate) {
        long totalDays = attendances.size();
        long presentDays = attendances.stream().filter(Attendance::isPresent).count();
        long absentDays = attendances.stream().filter(Attendance::isAbsent).count();
        long lateDays = attendances.stream().filter(Attendance::isLate).count();
        long excusedDays = attendances.stream().filter(Attendance::isExcused).count();
        
        double attendancePercentage = totalDays > 0 ? (double) presentDays / totalDays * 100 : 0;
        double absencePercentage = totalDays > 0 ? (double) absentDays / totalDays * 100 : 0;
        
        return new AttendanceStatisticsDto(
                user.getId(),
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
        
        requireSchoolClass(classId);
        List<Attendance> attendances = attendanceRepository.findByClassIdAndDateBetweenAndSchoolId(
                classId, startDate, endDate, currentSchool.resolve().getId());
        return attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()))
                .values().stream()
                .map(rows -> userStatistics(rows.get(0).getUser(), rows, startDate, endDate))
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceDto updateAttendance(Long attendanceId, AttendanceDto attendanceDto) {
        log.debug("Updating attendance {}", attendanceId);
        
        Attendance attendance = requireSchoolAttendance(attendanceId);
        
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
        
        attendanceRepository.delete(requireSchoolAttendance(attendanceId));
    }

    @Override
    public Page<AttendanceDto> getAttendanceByUserType(UserType userType, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        log.debug("Getting attendance by user type {} from {} to {}", userType, startDate, endDate);
        
        Page<Attendance> attendances = attendanceRepository.findByUserTypeAndDateBetweenAndSchoolId(userType, startDate, endDate, currentSchool.resolve().getId(), pageable);
        return attendances.map(mapper::toAttendanceDto);
    }

    @Override
    public AttendanceDto markAsExcused(Long attendanceId, String excuse) {
        log.debug("Marking attendance {} as excused", attendanceId);
        
        Attendance attendance = requireSchoolAttendance(attendanceId);
        
        attendance.setStatus(AttendanceStatus.EXCUSED);
        attendance.setExcuse(excuse);
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return mapper.toAttendanceDto(updatedAttendance);
    }

    @Override
    public AttendanceDto markAsLate(Long attendanceId, String remarks) {
        log.debug("Marking attendance {} as late", attendanceId);
        
        Attendance attendance = requireSchoolAttendance(attendanceId);
        
        attendance.setStatus(AttendanceStatus.LATE);
        attendance.setRemarks(remarks);
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        return mapper.toAttendanceDto(updatedAttendance);
    }

    @Override
    public Page<AttendanceDto> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> requestParams) {
        log.debug("Finding attendance with advanced filters");
        
        // Parse request parameters into FilterCriteria
        FilterCriteria filterCriteria = FilterCriteriaParser.parseRequestParams(requestParams, pageable, FILTER_FIELDS);
        
        // Build JPA Specification from FilterCriteria
        Long schoolId = currentSchool.resolve().getId();
        Specification<Attendance> schoolSpec = (root, query, cb) -> cb.equal(root.get("school").get("id"), schoolId);
        Specification<Attendance> specification = schoolSpec.and(DynamicSpecificationBuilder.build(filterCriteria));
        
        // Execute query with pagination
        Page<Attendance> attendances = attendanceRepository.findAll(specification, pageable);
        
        // Map to DTOs
        return attendances.map(mapper::toAttendanceDto);
    }

    private Attendance findExistingAttendance(Attendance attendance) {
        Long schoolId = attendance.getSchool().getId();
        Long userId = attendance.getUser().getId();
        if (attendance.getCourse() != null) {
            return attendanceRepository.findByUserIdAndCourseIdAndDateAndSchoolId(
                    userId, attendance.getCourse().getId(), attendance.getDate(), schoolId).orElse(null);
        } else if (attendance.getClassEntity() != null) {
            return attendanceRepository.findByUserIdAndClassIdAndDateAndSchoolId(
                    userId, attendance.getClassEntity().getId(), attendance.getDate(), schoolId).orElse(null);
        }
        return null;
    }

    private Attendance requireSchoolAttendance(Long attendanceId) {
        return attendanceRepository.findByIdAndSchoolId(attendanceId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
    }

    private ClassEntity requireSchoolClass(Long classId) {
        return classRepository.findByIdAndAcademicYearSchoolId(classId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private Course requireSchoolCourse(Long courseId) {
        return courseRepository.findByIdAndSchoolId(courseId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private void requireMembershipRole(Long userId, Long schoolId, MembershipRole role, String resource) {
        memberships.findByUserIdAndSchoolId(userId, schoolId)
                .filter(m -> m.getRoles().contains(role))
                .orElseThrow(() -> new ResourceNotFoundException(resource + " not found"));
    }

    private Student requireSchoolStudent(Long studentId) {
        requireMembershipRole(studentId, currentSchool.resolve().getId(), MembershipRole.STUDENT, "Student");
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private Teacher requireSchoolTeacher(Long teacherId) {
        requireMembershipRole(teacherId, currentSchool.resolve().getId(), MembershipRole.TEACHER, "Teacher");
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private BaseUser requireAttendanceUser(AttendanceDto dto) {
        return switch (dto.getUserType()) {
            case STUDENT -> requireSchoolStudent(dto.getUserId());
            case TEACHER -> requireSchoolTeacher(dto.getUserId());
            // STAFF has no MembershipRole; preserve the generic legacy target-user behavior.
            case STAFF -> {
                BaseUser user = userRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                // A client-supplied type cannot turn a foreign Student or Teacher into a STAFF resource.
                Long schoolId = currentSchool.resolve().getId();
                if (user instanceof Student) {
                    requireMembershipRole(user.getId(), schoolId, MembershipRole.STUDENT, "Student");
                } else if (user instanceof Teacher) {
                    requireMembershipRole(user.getId(), schoolId, MembershipRole.TEACHER, "Teacher");
                }
                yield user;
            }
        };
    }

    private BaseUser requireSchoolUserForRead(Long userId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long schoolId = currentSchool.resolve().getId();
        // Canonical School-owned history remains readable even if membership later disappears.
        if (!attendanceRepository.existsByUserIdAndSchoolId(userId, schoolId)) {
            if (user instanceof Student) {
                requireMembershipRole(userId, schoolId, MembershipRole.STUDENT, "Student");
            } else if (user instanceof Teacher) {
                requireMembershipRole(userId, schoolId, MembershipRole.TEACHER, "Teacher");
            }
        }
        return user;
    }

    private TimetableSlot requireSchoolSlot(Long slotId) {
        Long schoolId = currentSchool.resolve().getId();
        TimetableSlot slot = timetableSlotRepository.findByIdAndPeriodSchoolId(slotId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
        requireConsistentSlot(slot, schoolId);
        return slot;
    }

    private void requireConsistentSlot(TimetableSlot slot, Long schoolId) {
        if ((slot.getForClass() != null && !schoolId.equals(slot.getForClass().getAcademicYear().getSchool().getId()))
                || (slot.getForCourse() != null && !schoolId.equals(slot.getForCourse().getSchool().getId()))) {
            throw new ConflictException("Timetable slot has inconsistent School ownership");
        }
    }

    private void applyAttendanceContext(Attendance attendance, AttendanceDto dto, Long slotId) {
        ClassEntity clazz = dto.getClassId() == null ? null : requireSchoolClass(dto.getClassId());
        Course course = dto.getCourseId() == null ? null : requireSchoolCourse(dto.getCourseId());
        TimetableSlot slot = slotId == null ? null : requireSchoolSlot(slotId);
        if (slot != null) {
            requireMatchingSlotContext(slot, clazz, course);
            if (clazz == null) clazz = slot.getForClass();
            if (course == null) course = slot.getForCourse();
        }
        attendance.setClassEntity(clazz);
        attendance.setCourse(course);
        attendance.setTimetableSlot(slot);
    }

    private void requireMatchingSlotContext(TimetableSlot slot, ClassEntity clazz, Course course) {
        if (clazz != null && slot.getForClass() != null && !clazz.getId().equals(slot.getForClass().getId())) {
            throw new ConflictException("Class does not match the timetable slot");
        }
        if (course != null && slot.getForCourse() != null && !course.getId().equals(slot.getForCourse().getId())) {
            throw new ConflictException("Course does not match the timetable slot");
        }
    }

    // Teacher-specific attendance methods implementation

    @Override
    public List<AttendanceDto> getTeacherTodayScheduleWithAttendance(Long teacherId, LocalDate date) {
        log.debug("Getting today's schedule with attendance for teacher {} on date {}", teacherId, date);
        
        // Verify teacher exists
        Teacher teacher = requireSchoolTeacher(teacherId);
        
        // Get teacher's timetable slots for today
        com.example.school_management.feature.operational.entity.enums.DayOfWeek dayOfWeek = convertToDayOfWeek(date.getDayOfWeek());
        List<TimetableSlot> allSlots = timetableSlotRepository.findByTeacherIdAndPeriodSchoolId(teacherId, currentSchool.resolve().getId());

        allSlots.forEach(slot -> requireConsistentSlot(slot, currentSchool.resolve().getId()));
        List<TimetableSlot> todaySlots = allSlots.stream()
                .filter(slot -> slot.getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());

        log.debug("Found {} of {} timetable slots for teacher {} on {}", todaySlots.size(), allSlots.size(), teacherId, dayOfWeek);
        
        List<AttendanceDto> result = new ArrayList<>();
        
        // If no timetable slots exist, create virtual slots from teacher's assigned classes
        if (todaySlots.isEmpty()) {
            log.debug("No timetable slots for teacher {} on {}, creating virtual slots from assigned classes", teacherId, date);
            
            // Get teacher's assigned classes through teaching assignments
            List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId());
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
            }

            log.debug("Created {} virtual slots for teacher {} on {}", result.size(), teacherId, date);
        } else {
            // Process existing timetable slots
            for (TimetableSlot slot : todaySlots) {
                // Get existing attendance records for this slot
                List<Attendance> existingAttendance = attendanceRepository.findStudentAttendanceBySlotAndDateAndSchoolId(slot.getId(), date, currentSchool.resolve().getId());
                
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
        
        TimetableSlot slot = requireSchoolSlot(timetableSlotId);
        BaseUser currentUser = getCurrentUser();
        requireOwnSlotForTeacher(slot, currentUser);
        requireScheduledDayForTeacher(slot, date, currentUser);
        
        if (slot.getForClass() == null) {
            throw new ConflictException("Timetable slot must have an associated class");
        }
        
        // Get all students in the class
        List<Student> students = studentRepository.findByClassIds(List.of(slot.getForClass().getId()));
        
        // Check for existing attendance records
        List<Attendance> existingAttendance = attendanceRepository.findStudentAttendanceBySlotAndDateAndSchoolId(timetableSlotId, date, currentSchool.resolve().getId());
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
        
        TimetableSlot slot = requireSchoolSlot(timetableSlotId);
        
        BaseUser currentUser = getCurrentUser();
        if (currentUser.getRole() == UserRole.TEACHER) {
            requireOwnSlotForTeacher(slot, currentUser);
            requireScheduledDayForTeacher(slot, date, currentUser);
            requireSlotRoster(slot, attendanceList);
        }
        if (slot.getForClass() == null) {
            throw new ConflictException("Timetable slot must have an associated class");
        }
        School school = currentSchool.resolve();
        List<AttendanceDto> result = new ArrayList<>();

        for (AttendanceDto attendanceDto : attendanceList) {
            Student student = requireSchoolStudent(attendanceDto.getUserId());
            if (currentUser.getRole() != UserRole.TEACHER
                    && !enrollmentRepository.existsActiveInClass(student.getId(), slot.getForClass().getId())) {
                throw new ConflictException("Student is not actively enrolled in this class");
            }
            if (attendanceDto.getTimetableSlotId() != null) {
                TimetableSlot suppliedSlot = requireSchoolSlot(attendanceDto.getTimetableSlotId());
                if (!slot.getId().equals(suppliedSlot.getId())) {
                    throw new ConflictException("Timetable slot does not match the marking route");
                }
            }
            requireMatchingSlotContext(slot,
                    attendanceDto.getClassId() == null ? null : requireSchoolClass(attendanceDto.getClassId()),
                    attendanceDto.getCourseId() == null ? null : requireSchoolCourse(attendanceDto.getCourseId()));

            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByUserIdAndClassIdAndDateAndSchoolId(student.getId(), slot.getForClass().getId(), date, school.getId());
            
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
                attendance.setSchool(currentSchool.resolve());
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
        
        requireSchoolTeacher(teacherId);
        // Check if the timetable slot belongs to the teacher
        TimetableSlot slot = requireSchoolSlot(timetableSlotId);
        
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
        
        requireSchoolTeacher(teacherId);
        List<Attendance> weeklyAttendance = attendanceRepository.findStudentAttendanceByTeacherAndDateAndSchoolId(teacherId, startOfWeek, currentSchool.resolve().getId());
        weeklyAttendance.forEach(row -> requireConsistentSlot(row.getTimetableSlot(), currentSchool.resolve().getId()));
        
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
        
        requireSchoolTeacher(teacherId);
        List<Attendance> absentStudents = attendanceRepository.findAbsentStudentsByTeacherAndDateAndSchoolId(teacherId, date, currentSchool.resolve().getId());
        absentStudents.forEach(row -> requireConsistentSlot(row.getTimetableSlot(), currentSchool.resolve().getId()));
        
        return absentStudents.stream()
                .map(mapper::toAttendanceDto)
                .collect(Collectors.toList());
    }

    /*
     * Temporary guards for the teacher slot workflow only, until attendance is authorized through
     * canonical TeachingAssignment authorization: the slot's teacher guards authorization and ACTIVE
     * Enrollment supplies its current roster. These guards apply only to Attendance.
     */
    private void requireOwnSlotForTeacher(TimetableSlot slot, BaseUser caller) {
        if (caller.getRole() == UserRole.TEACHER) {
            requireSchoolTeacher(caller.getId());
            if (slot.getTeacher() == null || !slot.getTeacher().getId().equals(caller.getId())) {
                throw new AccessDeniedException("You can only take attendance for your own timetable slots");
            }
        }
    }

    // A teacher reads and marks a slot's attendance only for a date on the slot's weekday.
    private void requireScheduledDayForTeacher(TimetableSlot slot, LocalDate date, BaseUser caller) {
        if (caller.getRole() == UserRole.TEACHER && slot.getDayOfWeek() != convertToDayOfWeek(date.getDayOfWeek())) {
            throw new AccessDeniedException("Attendance for this slot can only be taken on its scheduled day");
        }
    }

    private void requireSlotRoster(TimetableSlot slot, List<AttendanceDto> attendanceList) {
        Set<Long> roster = slot.getForClass() == null ? Set.of()
                : new HashSet<>(enrollmentRepository.findActiveStudentIdsByClassId(slot.getForClass().getId()));
        for (AttendanceDto attendance : attendanceList) {
            if (!roster.contains(attendance.getUserId())) {
                throw new AccessDeniedException("Attendance can only be marked for students of this slot's class");
            }
        }
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
                student.getId(), parents.size());

        } catch (Exception e) {
            log.error("Failed to send absence notifications for student {}", student.getId(), e);
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
        ClassEntity classEntity = requireSchoolClass(classId);
        
        // Get all students in the class through enrollments (the correct way)
        List<Student> students = studentRepository.findByClassIds(List.of(classId));
        
        // Check for existing attendance records for this class on this date
        List<Attendance> existingAttendance = attendanceRepository.findByClassIdAndDateAndSchoolId(classId, date, currentSchool.resolve().getId());
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
        ClassEntity classEntity = requireSchoolClass(classId);
        
        // Get all students in the class through enrollments (the correct way)
        List<Student> students = studentRepository.findByClassIds(List.of(classId));

        // Convert to AttendanceDto format for consistency
        List<AttendanceDto> result = new ArrayList<>();

        for (Student student : students) {
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
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherAttendanceClassView getTeacherAttendanceClass(Long teacherId, Long classId, Long courseId) {
        log.debug("Getting attendance class view for teacher: {}, class: {}, course: {}", teacherId, classId, courseId);
        
        requireSchoolTeacher(teacherId);
        requireSchoolClass(classId);
        requireSchoolCourse(courseId);
        // Verify teaching assignment exists (similar to grade system)
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId());
        TeachingAssignment assignment = assignments.stream()
                .filter(ta -> ta.getClazz().getId().equals(classId) && ta.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Teaching assignment not found"));
        
        // Get all students enrolled in this class
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        
        List<TeacherAttendanceClassView.TeacherAttendanceStudent> students = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            LocalDate lastDate = attendanceRepository.findLastStudentAttendanceDate(
                    enrollment.getStudent().getId(), classId, courseId, currentSchool.resolve().getId());
            
            TeacherAttendanceClassView.TeacherAttendanceStudent student = TeacherAttendanceClassView.TeacherAttendanceStudent.builder()
                    .studentId(enrollment.getStudent().getId())
                    .firstName(enrollment.getStudent().getFirstName())
                    .lastName(enrollment.getStudent().getLastName())
                    .email(enrollment.getStudent().getEmail())
                    .enrollmentId(enrollment.getId())
                    .currentStatus(null) // No date or slot defines a current attendance record.
                    .attendanceRate(null)
                    .lastAttendanceDate(lastDate == null ? null : lastDate.toString())
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
        ClassEntity classEntity = requireSchoolClass(classId);
        
        BaseUser currentUser = getCurrentUser();
        List<AttendanceDto> result = new ArrayList<>();
        
        for (AttendanceDto attendanceDto : attendanceList) {
            // Verify student is in the class
            Student student = requireSchoolStudent(attendanceDto.getUserId());
            
            if (!enrollmentRepository.existsActiveInClass(student.getId(), classId)) {
                throw new ConflictException("Student is not actively enrolled in this class");
            }
            
            Attendance requestedContext = new Attendance();
            // Class roster DTOs use -1 to identify their virtual slot, with no persisted slot relationship.
            Long slotId = Long.valueOf(-1L).equals(attendanceDto.getTimetableSlotId())
                    ? null : attendanceDto.getTimetableSlotId();
            applyAttendanceContext(requestedContext, attendanceDto, slotId);
            if (requestedContext.getClassEntity() != null
                    && !classId.equals(requestedContext.getClassEntity().getId())) {
                throw new ConflictException("Class does not match the marking route");
            }
            requestedContext.setClassEntity(classEntity);

            // Check if attendance record already exists
            Attendance existingAttendance = attendanceRepository.findByUserIdAndClassIdAndDateAndSchoolId(
                    attendanceDto.getUserId(), classId, date, currentSchool.resolve().getId()).orElse(null);
            
            Attendance attendance;
            if (existingAttendance != null) {
                // Update existing record
                attendance = existingAttendance;
                attendance.setStatus(attendanceDto.getStatus());
                attendance.setRemarks(attendanceDto.getRemarks());
                attendance.setExcuse(attendanceDto.getExcuse());
                attendance.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new record with validated, normalized context.
                attendance = requestedContext;
                attendance.setSchool(currentSchool.resolve());
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

}
