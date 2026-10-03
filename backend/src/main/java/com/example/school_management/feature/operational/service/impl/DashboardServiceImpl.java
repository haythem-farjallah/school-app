package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.dto.DashboardDto;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.operational.repository.NotificationRepository;
import com.example.school_management.feature.operational.service.DashboardService;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final BaseUserRepository<BaseUser> userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final NotificationRepository notificationRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final SchoolMembershipRepository schoolMembershipRepository;
    private final CurrentSchoolResolver currentSchool;
    private final CurrentAcademicYearResolver currentAcademicYear;

    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @Override
    public Object getStudentDashboard(Long studentId) {
        Long schoolId = currentSchool.resolve().getId();
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("baseInfo", createBaseDashboard(student));
        dashboard.put("type", "STUDENT");
        dashboard.put("stats", createStudentStats(studentId, schoolId));
        dashboard.put("recentGrades", createRecentGrades(studentId, schoolId));
        dashboard.put("upcomingEvents", List.of());
        dashboard.put("enrolledClasses", createEnrolledClasses(studentId, schoolId));
        return dashboard;
    }

    @Override
    public Object getTeacherDashboard(Long teacherId) {
        Long schoolId = currentSchool.resolve().getId();
        Teacher teacher = teacherRepository.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        List<ClassEntity> classes = classRepository.findByTeacherIdAndSchoolId(teacherId, schoolId);
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherIdAndSchoolId(teacherId, schoolId);
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("baseInfo", createBaseDashboard(teacher));
        dashboard.put("type", "TEACHER");
        dashboard.put("stats", createTeacherStats(classes, assignments, schoolId));
        dashboard.put("classes", createTeacherClasses(classes, schoolId));
        dashboard.put("pendingTasks", List.of());
        dashboard.put("studentAlerts", List.of());
        return dashboard;
    }

    @Override
    public Object getParentDashboard(Long parentId) {
        Long schoolId = currentSchool.resolve().getId();
        Parent parent = parentRepository.findByIdAndSchoolId(parentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("baseInfo", createBaseDashboard(parent));
        dashboard.put("type", "PARENT");
        dashboard.put("children", createParentChildrenInfo(parentId, schoolId));
        dashboard.put("schoolUpdates", List.of());
        dashboard.put("upcomingEvents", List.of());
        return dashboard;
    }

    @Override
    public Object getAdminDashboard(Long adminId) {
        Long schoolId = currentSchool.resolve().getId();
        Long targetId = adminId == null ? getCurrentUser().getId() : adminId;
        schoolMembershipRepository.findByUserIdAndSchoolId(targetId, schoolId)
                .filter(membership -> membership.getRoles().contains(MembershipRole.ADMIN))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        BaseUser admin = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("baseInfo", createBaseDashboard(admin));
        dashboard.put("type", "ADMIN");
        dashboard.put("systemStats", createSystemStats(schoolId));
        dashboard.put("systemAlerts", List.of());
        dashboard.put("enrollmentTrends", Map.of());
        dashboard.put("performanceMetrics", Map.of());
        dashboard.put("recentSystemActivities", List.of());
        return dashboard;
    }

    @Override
    public Object getStaffDashboard(Long staffId) {
        BaseUser staff = staffId == null ? getCurrentUser() : getVisibleUser(staffId);
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("baseInfo", createBaseDashboard(staff));
        dashboard.put("type", "STAFF");
        dashboard.put("assignedTasks", List.of());
        dashboard.put("maintenanceAlerts", List.of());
        dashboard.put("recentActivities", List.of());
        return dashboard;
    }

    @Override
    public Object getCurrentUserDashboard() {
        BaseUser user = getCurrentUser();
        return switch (user.getRole()) {
            case STUDENT -> getStudentDashboard(user.getId());
            case TEACHER -> getTeacherDashboard(user.getId());
            case PARENT -> getParentDashboard(user.getId());
            case ADMIN -> getAdminDashboard(user.getId());
            case STAFF -> getStaffDashboard(user.getId());
        };
    }

    @Override
    public DashboardDto getBaseDashboardInfo(Long userId) {
        return createBaseDashboard(getVisibleUser(userId));
    }

    private BaseUser getVisibleUser(Long userId) {
        Long schoolId = currentSchool.resolve().getId();
        boolean self = getCurrentUser().getId().equals(userId);
        if (!self && !schoolMembershipRepository.existsByUserIdAndSchoolId(userId, schoolId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private DashboardDto createBaseDashboard(BaseUser user) {
        DashboardDto.UserInfo info = new DashboardDto.UserInfo(user.getId(),
                user.getFirstName() + " " + user.getLastName(), user.getEmail(), user.getRole().name(), null);
        return new DashboardDto(info, List.of(), createNotifications(user.getId()), null);
    }

    private List<DashboardDto.Notification> createNotifications(Long userId) {
        // UserNotification remains an account-level stream, independent of the current School.
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .getContent().stream().filter(notification -> !notification.getReadStatus())
                .map(notification -> new DashboardDto.Notification(notification.getId(), notification.getTitle(),
                        notification.getMessage(), notification.getType().name(), notification.getReadStatus(),
                        notification.getCreatedAt(), notification.getActionUrl()))
                .toList();
    }

    private Map<String, Object> createStudentStats(Long studentId, Long schoolId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEnrollments", enrollmentRepository.countByStudentIdAndSchoolId(studentId, schoolId));
        stats.put("averageGrade", averageGrade(gradeRepository.findByStudentIdAndSchoolId(studentId, schoolId)));
        stats.put("completedCourses", 0);
        stats.put("totalAssignments", 0);
        stats.put("currentGPA", "N/A");
        stats.put("academicStanding", "N/A");
        return stats;
    }

    private List<Map<String, Object>> createRecentGrades(Long studentId, Long schoolId) {
        return gradeRepository.findByStudentIdAndSchoolId(studentId, schoolId, PageRequest.of(0, 5))
                .getContent().stream().map(grade -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("courseName", grade.getEnrollment().getClassEntity().getName());
                    data.put("score", grade.getScore());
                    data.put("content", grade.getContent());
                    data.put("gradedAt", grade.getGradedAt());
                    return data;
                }).toList();
    }

    private Optional<Enrollment> currentEnrollment(Long studentId, Long schoolId) {
        if (enrollmentRepository.countByStudentIdAndSchoolId(studentId, schoolId) == 0) {
            return Optional.empty();
        }
        Long yearId = currentAcademicYear.resolve().getId();
        return enrollmentRepository.findActiveByStudentIdAndAcademicYearIdAndSchoolId(studentId, yearId, schoolId);
    }

    private List<Map<String, Object>> createEnrolledClasses(Long studentId, Long schoolId) {
        return currentEnrollment(studentId, schoolId).stream().map(enrollment -> {
            ClassEntity clazz = enrollment.getClassEntity();
            Map<String, Object> data = new HashMap<>();
            data.put("classId", clazz.getId());
            data.put("className", clazz.getName());
            data.put("teacherName", teachingAssignmentRepository.findAllByClassId(clazz.getId()).stream()
                    .map(assignment -> assignment.getTeacher().getFirstName() + " " + assignment.getTeacher().getLastName())
                    .distinct().collect(Collectors.joining(", ")));
            data.put("totalStudents", enrollmentRepository.countActiveByClassId(clazz.getId()));
            data.put("schedule", "");
            return data;
        }).toList();
    }

    private Map<String, Object> createTeacherStats(List<ClassEntity> classes, List<TeachingAssignment> assignments, Long schoolId) {
        Map<Long, Course> courses = new LinkedHashMap<>();
        Map<Long, Course> activeCourses = new LinkedHashMap<>();
        for (TeachingAssignment assignment : assignments) {
            courses.put(assignment.getCourse().getId(), assignment.getCourse());
            if (assignment.getClazz().getAcademicYear().isActive()) {
                activeCourses.put(assignment.getCourse().getId(), assignment.getCourse());
            }
        }
        for (ClassEntity clazz : classes) {
            for (Course course : clazz.getCourses()) {
                if (!schoolId.equals(course.getSchool().getId())) {
                    throw new ConflictException("Class contains a Course outside the current School");
                }
                courses.put(course.getId(), course);
                if (clazz.getAcademicYear().isActive()) activeCourses.put(course.getId(), course);
            }
        }
        List<Long> classIds = classes.stream().map(ClassEntity::getId).toList();
        long students = enrollmentRepository.findActiveRosterRows(classIds).stream()
                .map(EnrollmentRepository.RosterRow::getStudentId).distinct().count();
        List<Grade> grades = classes.stream()
                .flatMap(clazz -> gradeRepository.findByClassIdAndSchoolId(clazz.getId(), schoolId).stream()).toList();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClasses", classes.size());
        stats.put("totalStudents", students);
        stats.put("totalCourses", courses.size());
        stats.put("pendingGrades", 0);
        stats.put("averageClassGrade", averageGrade(grades));
        stats.put("activeCourses", activeCourses.size());
        return stats;
    }

    private List<Map<String, Object>> createTeacherClasses(List<ClassEntity> classes, Long schoolId) {
        return classes.stream().map(clazz -> {
            List<Grade> grades = gradeRepository.findByClassIdAndSchoolId(clazz.getId(), schoolId);
            Map<String, Object> data = new HashMap<>();
            data.put("classId", clazz.getId());
            data.put("className", clazz.getName());
            data.put("enrolledStudents", enrollmentRepository.countActiveByClassId(clazz.getId()));
            data.put("totalAssignments", 0);
            data.put("pendingGrades", 0);
            data.put("averageGrade", averageGrade(grades));
            data.put("lastActivity", grades.stream().map(Grade::getGradedAt)
                    .filter(java.util.Objects::nonNull).max(java.util.Comparator.naturalOrder()).orElse(null));
            return data;
        }).toList();
    }

    private List<Map<String, Object>> createParentChildrenInfo(Long parentId, Long schoolId) {
        return studentRepository.findByParentIdAndSchoolId(parentId, schoolId).stream().map(child -> {
            Map<String, Object> data = new HashMap<>();
            data.put("studentId", child.getId());
            data.put("name", child.getFirstName() + " " + child.getLastName());
            data.put("currentClass", currentEnrollment(child.getId(), schoolId)
                    .map(enrollment -> enrollment.getClassEntity().getName()).orElse(""));
            data.put("averageGrade", averageGrade(gradeRepository.findByStudentIdAndSchoolId(child.getId(), schoolId)));
            data.put("totalAbsences", attendanceRepository.countByUserIdAndStatusAndUserTypeAndSchoolId(
                    child.getId(), AttendanceStatus.ABSENT, UserType.STUDENT, schoolId));
            data.put("academicStanding", "N/A");
            return data;
        }).toList();
    }

    private Map<String, Object> createSystemStats(Long schoolId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", schoolMembershipRepository.countUsersBySchoolIdAndRole(schoolId, MembershipRole.STUDENT));
        stats.put("totalTeachers", schoolMembershipRepository.countUsersBySchoolIdAndRole(schoolId, MembershipRole.TEACHER));
        stats.put("totalParents", schoolMembershipRepository.countUsersBySchoolIdAndRole(schoolId, MembershipRole.GUARDIAN));
        stats.put("totalClasses", classRepository.countBySchoolId(schoolId));
        stats.put("totalCourses", courseRepository.countBySchoolId(schoolId));
        stats.put("activeEnrollments", enrollmentRepository.countBySchoolIdAndStatus(schoolId, EnrollmentStatus.ACTIVE));
        stats.put("systemHealth", 0.0);
        stats.put("serverStatus", "N/A");
        return stats;
    }

    private double averageGrade(List<Grade> grades) {
        return grades.stream().map(Grade::getScore).filter(java.util.Objects::nonNull)
                .mapToDouble(Float::doubleValue).average().orElse(0.0);
    }
}
