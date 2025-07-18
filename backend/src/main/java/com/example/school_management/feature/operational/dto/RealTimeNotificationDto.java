package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.Set;

@Value
@With
public class RealTimeNotificationDto {
    String id;
    String type; // ADMIN_FEED, USER_NOTIFICATION, SYSTEM_ALERT
    String title;
    String message;
    String priority; // HIGH, MEDIUM, LOW
    LocalDateTime timestamp;
    String performedBy;
    String entityType;
    Long entityId;
    Set<String> targetRoles; // ADMIN, TEACHER, PARENT, STUDENT
    Set<Long> targetUserIds; // Specific users if needed
    String actionUrl;
    Object additionalData;
    
    // Static factory methods for different notification types
    
    public static RealTimeNotificationDto adminFeed(
            AuditEventType eventType,
            String summary,
            String details,
            String performedBy,
            String entityType,
            Long entityId) {
        return new RealTimeNotificationDto(
            java.util.UUID.randomUUID().toString(),
            "ADMIN_FEED",
            summary,
            details,
            determinePriority(eventType),
            LocalDateTime.now(),
            performedBy,
            entityType,
            entityId,
            Set.of("ADMIN"),
            null,
            null,
            null
        );
    }
    
    public static RealTimeNotificationDto userNotification(
            String title,
            String message,
            String priority,
            Set<String> targetRoles,
            Set<Long> targetUserIds) {
        return new RealTimeNotificationDto(
            java.util.UUID.randomUUID().toString(),
            "USER_NOTIFICATION",
            title,
            message,
            priority,
            LocalDateTime.now(),
            "SYSTEM",
            null,
            null,
            targetRoles,
            targetUserIds,
            null,
            null
        );
    }
    
    public static RealTimeNotificationDto systemAlert(
            String title,
            String message,
            String priority) {
        return new RealTimeNotificationDto(
            java.util.UUID.randomUUID().toString(),
            "SYSTEM_ALERT",
            title,
            message,
            priority,
            LocalDateTime.now(),
            "SYSTEM",
            "SystemAlert",
            null,
            Set.of("ADMIN"),
            null,
            null,
            null
        );
    }
    
    public static RealTimeNotificationDto gradeNotification(
            String studentName,
            String courseName,
            String className,
            Double score,
            Long studentId,
            Long parentId) {
        String title = "New Grade Posted";
        String message = String.format("Grade posted for %s in %s (%s): %.2f", 
            studentName, courseName, className, score);
        
        return new RealTimeNotificationDto(
            java.util.UUID.randomUUID().toString(),
            "GRADE_NOTIFICATION",
            title,
            message,
            "MEDIUM",
            LocalDateTime.now(),
            "TEACHER",
            "Grade",
            null,
            Set.of("PARENT", "STUDENT"),
            Set.of(studentId, parentId),
            "/grades",
            null
        );
    }
    
    public static RealTimeNotificationDto enrollmentNotification(
            String studentName,
            String className,
            String action,
            Long studentId,
            Long parentId) {
        String title = String.format("Enrollment %s", action);
        String message = String.format("Student %s has been %s %s class %s", 
            studentName, action.toLowerCase(), 
            action.equals("ENROLLED") ? "in" : "from", className);
        
        return new RealTimeNotificationDto(
            java.util.UUID.randomUUID().toString(),
            "ENROLLMENT_NOTIFICATION",
            title,
            message,
            "HIGH",
            LocalDateTime.now(),
            "ADMIN",
            "Enrollment",
            null,
            Set.of("PARENT", "STUDENT"),
            Set.of(studentId, parentId),
            "/enrollments",
            null
        );
    }
    
    public static RealTimeNotificationDto announcementNotification(
            String title,
            String content,
            String importance,
            Set<String> targetRoles) {
        return new RealTimeNotificationDto(
            java.util.UUID.randomUUID().toString(),
            "ANNOUNCEMENT_NOTIFICATION",
            "New Announcement: " + title,
            content,
            mapImportanceToPriority(importance),
            LocalDateTime.now(),
            "ADMIN",
            "Announcement",
            null,
            targetRoles,
            null,
            "/announcements",
            null
        );
    }
    
    private static String determinePriority(AuditEventType eventType) {
        return switch (eventType) {
            case USER_DELETED, ENROLLMENT_DELETED, GRADE_DELETED -> "HIGH";
            case USER_CREATED, USER_UPDATED, ENROLLMENT_CREATED, GRADE_RECORDED -> "MEDIUM";
            default -> "LOW";
        };
    }
    
    private static String mapImportanceToPriority(String importance) {
        return switch (importance.toUpperCase()) {
            case "URGENT", "HIGH" -> "HIGH";
            case "MEDIUM", "NORMAL" -> "MEDIUM";
            default -> "LOW";
        };
    }
} 