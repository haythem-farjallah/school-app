package com.example.school_management.feature.operational.dto;

import lombok.Value;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Base dashboard DTO with common information
@Value
public class DashboardDto {
    UserInfo userInfo;
    List<RecentActivity> recentActivities;
    List<Notification> notifications;
    LocalDateTime lastLogin;
    
    @Value
    public static class UserInfo {
        Long id;
        String name;
        String email;
        String role;
        String avatarUrl;
    }
    
    @Value
    public static class RecentActivity {
        String type;
        String description;
        LocalDateTime timestamp;
        String actionUrl;
    }
    
    @Value 
    public static class Notification {
        Long id;
        String title;
        String message;
        String type;
        Boolean isRead;
        LocalDateTime createdAt;
        String actionUrl;
    }
}

// Student-specific dashboard
@Value 
class StudentDashboardDto {
    DashboardDto baseInfo;
    StudentStats stats;
    List<UpcomingEvent> upcomingEvents;
    List<RecentGrade> recentGrades;
    List<ClassInfo> enrolledClasses;
    AcademicProgress academicProgress;
    
    @Value
    public static class StudentStats {
        Integer totalEnrollments;
        Double averageGrade;
        Integer completedCourses;
        Integer totalAssignments;
        String currentGPA;
        String academicStanding;
    }
    
    @Value
    public static class UpcomingEvent {
        String title;
        String type; // EXAM, ASSIGNMENT, CLASS, etc.
        LocalDateTime dateTime;
        String location;
        String className;
    }
    
    @Value
    public static class RecentGrade {
        String courseName;
        String className;
        Double score;
        String letterGrade;
        LocalDateTime gradedAt;
        String content;
    }
    
    @Value
    public static class ClassInfo {
        Long classId;
        String className;
        String teacherName;
        Integer totalStudents;
        String schedule;
    }
    
    @Value
    public static class AcademicProgress {
        Integer totalCredits;
        Integer completedCredits;
        Double progressPercentage;
        String expectedGraduation;
        List<String> achievements;
    }
}

// Teacher-specific dashboard
@Value
class TeacherDashboardDto {
    DashboardDto baseInfo;
    TeacherStats stats;
    List<ClassOverview> classes;
    List<PendingTask> pendingTasks;
    List<StudentAlert> studentAlerts;
    
    @Value
    public static class TeacherStats {
        Integer totalClasses;
        Integer totalStudents;
        Integer totalCourses;
        Integer pendingGrades;
        Double averageClassGrade;
        Integer activeCourses;
    }
    
    @Value
    public static class ClassOverview {
        Long classId;
        String className;
        Integer enrolledStudents;
        Integer totalAssignments;
        Integer pendingGrades;
        Double averageGrade;
        LocalDateTime lastActivity;
    }
    
    @Value
    public static class PendingTask {
        String type; // GRADE_ASSIGNMENT, REVIEW_SUBMISSION, etc.
        String description;
        Integer count;
        String priority;
        LocalDateTime dueDate;
    }
    
    @Value
    public static class StudentAlert {
        Long studentId;
        String studentName;
        String alertType; // LOW_GRADE, ABSENCE, etc.
        String description;
        String severity;
        LocalDateTime createdAt;
    }
}

// Parent-specific dashboard  
@Value
class ParentDashboardDto {
    DashboardDto baseInfo;
    List<ChildInfo> children;
    List<SchoolUpdate> schoolUpdates;
    List<UpcomingEvent> upcomingEvents;
    
    @Value
    public static class ChildInfo {
        Long studentId;
        String name;
        String currentClass;
        Double averageGrade;
        Integer totalAbsences;
        String academicStanding;
        List<StudentGrade> recentGrades;
        List<TeacherNote> teacherNotes;
    }
    
    @Value
    public static class StudentGrade {
        String courseName;
        String className;
        Double score;
        String letterGrade;
        LocalDateTime gradedAt;
        String content;
    }
    
    @Value
    public static class TeacherNote {
        String teacherName;
        String subject;
        String note;
        LocalDateTime createdAt;
        String type; // POSITIVE, CONCERN, NEUTRAL
    }
    
    @Value
    public static class SchoolUpdate {
        String title;
        String content;
        String type; // ANNOUNCEMENT, POLICY, EVENT
        LocalDateTime publishedAt;
        String importance;
    }
    
    @Value
    public static class UpcomingEvent {
        String title;
        String description;
        LocalDateTime dateTime;
        String type;
        List<String> affectedStudents;
    }
}

// Admin-specific dashboard
@Value
class AdminDashboardDto {
    DashboardDto baseInfo;
    SystemStats systemStats;
    List<SystemAlert> systemAlerts;
    Map<String, Integer> enrollmentTrends;
    Map<String, Double> performanceMetrics;
    List<RecentSystemActivity> recentSystemActivities;
    
    @Value
    public static class SystemStats {
        Integer totalStudents;
        Integer totalTeachers;
        Integer totalParents;
        Integer totalClasses;
        Integer totalCourses;
        Integer activeEnrollments;
        Double systemHealth;
        String serverStatus;
    }
    
    @Value
    public static class SystemAlert {
        String type; // ERROR, WARNING, INFO
        String title;
        String description;
        String severity;
        LocalDateTime timestamp;
        Boolean isResolved;
        String actionRequired;
    }
    
    @Value
    public static class RecentSystemActivity {
        String activityType;
        String description;
        String performedBy;
        LocalDateTime timestamp;
        String entityType;
        Long entityId;
    }
} 