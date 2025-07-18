package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.operational.dto.DashboardDto;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.operational.repository.NotificationRepository;
import com.example.school_management.feature.operational.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
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

    private BaseUser getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @Override
    public Object getStudentDashboard(Long studentId) {
        log.debug("Getting student dashboard for student: {}", studentId);
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        // Create basic dashboard info
        DashboardDto baseInfo = createBaseDashboard(student);
        
        // Create student-specific data
        Map<String, Object> studentDashboard = new HashMap<>();
        studentDashboard.put("baseInfo", baseInfo);
        studentDashboard.put("type", "STUDENT");
        studentDashboard.put("stats", createStudentStats(studentId));
        studentDashboard.put("recentGrades", createRecentGrades(studentId));
        studentDashboard.put("upcomingEvents", createUpcomingEvents(studentId));
        studentDashboard.put("enrolledClasses", createEnrolledClasses(studentId));
        
        return studentDashboard;
    }

    @Override
    public Object getTeacherDashboard(Long teacherId) {
        log.debug("Getting teacher dashboard for teacher: {}", teacherId);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        
        // Create basic dashboard info
        DashboardDto baseInfo = createBaseDashboard(teacher);
        
        // Create teacher-specific data
        Map<String, Object> teacherDashboard = new HashMap<>();
        teacherDashboard.put("baseInfo", baseInfo);
        teacherDashboard.put("type", "TEACHER");
        teacherDashboard.put("stats", createTeacherStats(teacherId));
        teacherDashboard.put("classes", createTeacherClasses(teacherId));
        teacherDashboard.put("pendingTasks", createPendingTasks(teacherId));
        teacherDashboard.put("studentAlerts", createStudentAlerts(teacherId));
        
        return teacherDashboard;
    }

    @Override
    public Object getParentDashboard(Long parentId) {
        log.debug("Getting parent dashboard for parent: {}", parentId);
        
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        
        // Create basic dashboard info
        DashboardDto baseInfo = createBaseDashboard(parent);
        
        // Create parent-specific data
        Map<String, Object> parentDashboard = new HashMap<>();
        parentDashboard.put("baseInfo", baseInfo);
        parentDashboard.put("type", "PARENT");
        parentDashboard.put("children", createParentChildrenInfo(parentId));
        parentDashboard.put("schoolUpdates", createSchoolUpdates());
        parentDashboard.put("upcomingEvents", createParentUpcomingEvents(parentId));
        
        return parentDashboard;
    }

    @Override
    public Object getAdminDashboard(Long adminId) {
        log.debug("Getting admin dashboard for admin: {}", adminId);
        
        BaseUser admin = null;
        if (adminId != null) {
            admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        } else {
            admin = getCurrentUser();
        }
        
        // Create basic dashboard info
        DashboardDto baseInfo = createBaseDashboard(admin);
        
        // Create admin-specific data
        Map<String, Object> adminDashboard = new HashMap<>();
        adminDashboard.put("baseInfo", baseInfo);
        adminDashboard.put("type", "ADMIN");
        adminDashboard.put("systemStats", createSystemStats());
        adminDashboard.put("systemAlerts", createSystemAlerts());
        adminDashboard.put("enrollmentTrends", createEnrollmentTrends());
        adminDashboard.put("performanceMetrics", createPerformanceMetrics());
        adminDashboard.put("recentSystemActivities", createRecentSystemActivities());
        
        return adminDashboard;
    }

    @Override
    public Object getStaffDashboard(Long staffId) {
        log.debug("Getting staff dashboard for staff: {}", staffId);
        
        BaseUser staff = null;
        if (staffId != null) {
            staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        } else {
            staff = getCurrentUser();
        }
        
        // Create basic dashboard info
        DashboardDto baseInfo = createBaseDashboard(staff);
        
        // Create staff-specific data
        Map<String, Object> staffDashboard = new HashMap<>();
        staffDashboard.put("baseInfo", baseInfo);
        staffDashboard.put("type", "STAFF");
        staffDashboard.put("assignedTasks", createStaffTasks(staffId));
        staffDashboard.put("maintenanceAlerts", createMaintenanceAlerts());
        staffDashboard.put("recentActivities", createRecentActivities(staffId));
        
        return staffDashboard;
    }

    @Override
    public Object getCurrentUserDashboard() {
        log.debug("Getting dashboard for current user");
        
        BaseUser currentUser = getCurrentUser();
        String role = currentUser.getRole().name();
        
        return switch (role) {
            case "STUDENT" -> getStudentDashboard(currentUser.getId());
            case "TEACHER" -> getTeacherDashboard(currentUser.getId());
            case "PARENT" -> getParentDashboard(currentUser.getId());
            case "ADMIN" -> getAdminDashboard(currentUser.getId());
            case "STAFF" -> getStaffDashboard(currentUser.getId());
            default -> getBaseDashboardInfo(currentUser.getId());
        };
    }

    @Override
    public DashboardDto getBaseDashboardInfo(Long userId) {
        log.debug("Getting base dashboard info for user: {}", userId);
        
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return createBaseDashboard(user);
    }

    // Helper methods for creating dashboard components

    private DashboardDto createBaseDashboard(BaseUser user) {
        DashboardDto.UserInfo userInfo = new DashboardDto.UserInfo(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                null // avatarUrl - can be added later
        );

        List<DashboardDto.RecentActivity> recentActivities = createRecentActivities(user.getId());
        List<DashboardDto.Notification> notifications = createNotifications(user.getId());
        
        return new DashboardDto(
                userInfo,
                recentActivities,
                notifications,
                LocalDateTime.now().minusDays(1) // Mock last login
        );
    }

    private List<DashboardDto.RecentActivity> createRecentActivities(Long userId) {
        // Mock recent activities - can be enhanced with real data
        List<DashboardDto.RecentActivity> activities = new ArrayList<>();
        activities.add(new DashboardDto.RecentActivity(
                "LOGIN",
                "Logged into the system",
                LocalDateTime.now().minusHours(2),
                "/dashboard"
        ));
        return activities;
    }

    private List<DashboardDto.Notification> createNotifications(Long userId) {
        // Get real notifications from repository
        List<DashboardDto.Notification> notifications = new ArrayList<>();
        
        try {
            // Get recent unread notifications
            var dbNotificationsPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(0, 5));
            var dbNotifications = dbNotificationsPage.getContent().stream()
                    .filter(n -> !n.getReadStatus())
                    .limit(5)
                    .toList();
            
            for (var notification : dbNotifications) {
                notifications.add(new DashboardDto.Notification(
                        notification.getId(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getType().name(),
                        notification.getReadStatus(),
                        notification.getCreatedAt(),
                        notification.getActionUrl()
                ));
            }
        } catch (Exception e) {
            log.warn("Could not load notifications for user {}: {}", userId, e.getMessage());
        }
        
        return notifications;
    }

    private Map<String, Object> createStudentStats(Long studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalEnrollments = enrollmentRepository.countByStudentId(studentId);
            stats.put("totalEnrollments", totalEnrollments);
            stats.put("averageGrade", 85.5); // Mock data
            stats.put("completedCourses", 12);
            stats.put("totalAssignments", 45);
            stats.put("currentGPA", "3.7");
            stats.put("academicStanding", "Good Standing");
        } catch (Exception e) {
            log.warn("Error creating student stats: {}", e.getMessage());
            stats.put("totalEnrollments", 0);
        }
        
        return stats;
    }

    private List<Map<String, Object>> createRecentGrades(Long studentId) {
        List<Map<String, Object>> grades = new ArrayList<>();
        
        try {
            var recentGradesPage = gradeRepository.findByStudentIdOrderByGradedAtDesc(studentId, PageRequest.of(0, 5));
            var recentGrades = recentGradesPage.getContent();
            
            for (var grade : recentGrades) {
                Map<String, Object> gradeData = new HashMap<>();
                gradeData.put("courseName", grade.getEnrollment().getClassEntity().getName());
                gradeData.put("score", grade.getScore());
                gradeData.put("content", grade.getContent());
                gradeData.put("gradedAt", grade.getGradedAt());
                grades.add(gradeData);
            }
        } catch (Exception e) {
            log.warn("Error loading recent grades: {}", e.getMessage());
        }
        
        return grades;
    }

    private List<Map<String, Object>> createUpcomingEvents(Long studentId) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        // Mock upcoming events
        Map<String, Object> event = new HashMap<>();
        event.put("title", "Mathematics Exam");
        event.put("type", "EXAM");
        event.put("dateTime", LocalDateTime.now().plusDays(3));
        event.put("location", "Room 101");
        event.put("className", "Mathematics");
        events.add(event);
        
        return events;
    }

    private List<Map<String, Object>> createEnrolledClasses(Long studentId) {
        List<Map<String, Object>> classes = new ArrayList<>();
        
        try {
            var enrollments = enrollmentRepository.findByStudentId(studentId);
            
            for (var enrollment : enrollments) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("classId", enrollment.getClassEntity().getId());
                classData.put("className", enrollment.getClassEntity().getName());
                classData.put("teacherName", "Teacher Name"); // Would need to get from class
                classData.put("totalStudents", 25); // Mock data
                classData.put("schedule", "Mon, Wed, Fri");
                classes.add(classData);
            }
        } catch (Exception e) {
            log.warn("Error loading enrolled classes: {}", e.getMessage());
        }
        
        return classes;
    }

    private Map<String, Object> createTeacherStats(Long teacherId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalClasses = classRepository.countByTeacherId(teacherId);
            stats.put("totalClasses", totalClasses);
            stats.put("totalStudents", 120); // Mock data
            stats.put("totalCourses", 5);
            stats.put("pendingGrades", 15);
            stats.put("averageClassGrade", 82.3);
            stats.put("activeCourses", 3);
        } catch (Exception e) {
            log.warn("Error creating teacher stats: {}", e.getMessage());
            stats.put("totalClasses", 0);
        }
        
        return stats;
    }

    private List<Map<String, Object>> createTeacherClasses(Long teacherId) {
        List<Map<String, Object>> classes = new ArrayList<>();
        
        try {
            var teacherClasses = classRepository.findByTeacherId(teacherId);
            
            for (var classEntity : teacherClasses) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("classId", classEntity.getId());
                classData.put("className", classEntity.getName());
                classData.put("enrolledStudents", 25); // Would need to count enrollments
                classData.put("totalAssignments", 10);
                classData.put("pendingGrades", 5);
                classData.put("averageGrade", 85.0);
                classData.put("lastActivity", LocalDateTime.now().minusDays(1));
                classes.add(classData);
            }
        } catch (Exception e) {
            log.warn("Error loading teacher classes: {}", e.getMessage());
        }
        
        return classes;
    }

    private List<Map<String, Object>> createPendingTasks(Long teacherId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        
        // Mock pending tasks
        Map<String, Object> task = new HashMap<>();
        task.put("type", "GRADE_ASSIGNMENT");
        task.put("description", "Grade Mathematics Assignment #3");
        task.put("count", 15);
        task.put("priority", "HIGH");
        task.put("dueDate", LocalDateTime.now().plusDays(2));
        tasks.add(task);
        
        return tasks;
    }

    private List<Map<String, Object>> createStudentAlerts(Long teacherId) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // Mock student alerts
        Map<String, Object> alert = new HashMap<>();
        alert.put("studentId", 1L);
        alert.put("studentName", "John Doe");
        alert.put("alertType", "LOW_GRADE");
        alert.put("description", "Student has received a failing grade");
        alert.put("severity", "HIGH");
        alert.put("createdAt", LocalDateTime.now().minusDays(1));
        alerts.add(alert);
        
        return alerts;
    }

    private List<Map<String, Object>> createParentChildrenInfo(Long parentId) {
        List<Map<String, Object>> children = new ArrayList<>();
        
        try {
            var parentChildren = studentRepository.findByParentId(parentId);
            
            for (var child : parentChildren) {
                Map<String, Object> childData = new HashMap<>();
                childData.put("studentId", child.getId());
                childData.put("name", child.getFirstName() + " " + child.getLastName());
                childData.put("currentClass", "Grade 10A"); // Mock data
                childData.put("averageGrade", 88.5);
                childData.put("totalAbsences", 3);
                childData.put("academicStanding", "Good Standing");
                children.add(childData);
            }
        } catch (Exception e) {
            log.warn("Error loading parent children info: {}", e.getMessage());
        }
        
        return children;
    }

    private List<Map<String, Object>> createSchoolUpdates() {
        List<Map<String, Object>> updates = new ArrayList<>();
        
        // Mock school updates
        Map<String, Object> update = new HashMap<>();
        update.put("title", "Holiday Schedule");
        update.put("content", "Updated holiday schedule for the semester");
        update.put("type", "ANNOUNCEMENT");
        update.put("publishedAt", LocalDateTime.now().minusDays(2));
        update.put("importance", "MEDIUM");
        updates.add(update);
        
        return updates;
    }

    private List<Map<String, Object>> createParentUpcomingEvents(Long parentId) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        // Mock upcoming events
        Map<String, Object> event = new HashMap<>();
        event.put("title", "Parent-Teacher Conference");
        event.put("description", "Quarterly parent-teacher meetings");
        event.put("dateTime", LocalDateTime.now().plusDays(7));
        event.put("type", "MEETING");
        event.put("affectedStudents", List.of("John Doe"));
        events.add(event);
        
        return events;
    }

    private Map<String, Object> createSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalStudents = studentRepository.count();
            long totalTeachers = teacherRepository.count();
            long totalClasses = classRepository.count();
            long totalCourses = courseRepository.count();
            
            stats.put("totalStudents", totalStudents);
            stats.put("totalTeachers", totalTeachers);
            stats.put("totalParents", parentRepository.count());
            stats.put("totalClasses", totalClasses);
            stats.put("totalCourses", totalCourses);
            stats.put("activeEnrollments", enrollmentRepository.count());
            stats.put("systemHealth", 98.5);
            stats.put("serverStatus", "ONLINE");
        } catch (Exception e) {
            log.warn("Error creating system stats: {}", e.getMessage());
            stats.put("totalStudents", 0);
            stats.put("totalTeachers", 0);
            stats.put("totalClasses", 0);
            stats.put("totalCourses", 0);
        }
        
        return stats;
    }

    private List<Map<String, Object>> createSystemAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // Mock system alerts
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "INFO");
        alert.put("title", "System Maintenance");
        alert.put("description", "Scheduled maintenance this weekend");
        alert.put("severity", "LOW");
        alert.put("timestamp", LocalDateTime.now().minusHours(6));
        alert.put("isResolved", false);
        alert.put("actionRequired", "None");
        alerts.add(alert);
        
        return alerts;
    }

    private Map<String, Integer> createEnrollmentTrends() {
        Map<String, Integer> trends = new HashMap<>();
        trends.put("January", 150);
        trends.put("February", 155);
        trends.put("March", 160);
        trends.put("April", 158);
        trends.put("May", 162);
        return trends;
    }

    private Map<String, Double> createPerformanceMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("averageGPA", 3.2);
        metrics.put("attendanceRate", 92.5);
        metrics.put("graduationRate", 85.0);
        metrics.put("satisfactionScore", 4.2);
        return metrics;
    }

    private List<Map<String, Object>> createRecentSystemActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        Map<String, Object> activity = new HashMap<>();
        activity.put("activityType", "USER_REGISTRATION");
        activity.put("description", "New student registered");
        activity.put("performedBy", "System");
        activity.put("timestamp", LocalDateTime.now().minusHours(3));
        activity.put("entityType", "Student");
        activities.add(activity);
        
        return activities;
    }

    private List<Map<String, Object>> createStaffTasks(Long staffId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        
        // Mock staff tasks
        Map<String, Object> task = new HashMap<>();
        task.put("type", "MAINTENANCE");
        task.put("description", "Fix air conditioning in Room 201");
        task.put("priority", "HIGH");
        task.put("assignedAt", LocalDateTime.now().minusHours(2));
        task.put("dueDate", LocalDateTime.now().plusDays(1));
        task.put("status", "IN_PROGRESS");
        tasks.add(task);
        
        Map<String, Object> task2 = new HashMap<>();
        task2.put("type", "SECURITY");
        task2.put("description", "Check security cameras in Building B");
        task2.put("priority", "MEDIUM");
        task2.put("assignedAt", LocalDateTime.now().minusDays(1));
        task2.put("dueDate", LocalDateTime.now().plusDays(2));
        task2.put("status", "PENDING");
        tasks.add(task2);
        
        return tasks;
    }

    private List<Map<String, Object>> createMaintenanceAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // Mock maintenance alerts
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "MAINTENANCE");
        alert.put("title", "HVAC System Alert");
        alert.put("description", "Temperature sensor malfunction in Room 105");
        alert.put("severity", "MEDIUM");
        alert.put("location", "Room 105");
        alert.put("timestamp", LocalDateTime.now().minusHours(4));
        alert.put("isResolved", false);
        alerts.add(alert);
        
        Map<String, Object> alert2 = new HashMap<>();
        alert2.put("type", "SECURITY");
        alert2.put("title", "Door Access Issue");
        alert2.put("description", "Card reader not responding at Main Entrance");
        alert2.put("severity", "HIGH");
        alert2.put("location", "Main Entrance");
        alert2.put("timestamp", LocalDateTime.now().minusHours(1));
        alert2.put("isResolved", false);
        alerts.add(alert2);
        
        return alerts;
    }
} 