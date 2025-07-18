package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.RealTimeNotificationDto;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast admin feed notification to all admin users
     */
    public void broadcastAdminFeed(AuditEventType eventType, String summary, String details, 
                                  String performedBy, String entityType, Long entityId) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.adminFeed(
                eventType, summary, details, performedBy, entityType, entityId
            );
            
            // Send to admin-specific topic
            messagingTemplate.convertAndSend("/topic/admin-feeds", notification);
            log.debug("Broadcast admin feed: {} - {}", summary, details);
            
        } catch (Exception e) {
            log.error("Failed to broadcast admin feed notification", e);
        }
    }

    /**
     * Send notification to specific user roles
     */
    public void notifyUserRoles(String title, String message, String priority, Set<String> targetRoles) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.userNotification(
                title, message, priority, targetRoles, null
            );
            
            // Send to each target role
            for (String role : targetRoles) {
                String destination = "/topic/notifications/" + role.toLowerCase();
                messagingTemplate.convertAndSend(destination, notification);
                log.debug("Sent notification to role {}: {}", role, title);
            }
            
        } catch (Exception e) {
            log.error("Failed to send role-based notifications", e);
        }
    }

    /**
     * Send notification to specific users
     */
    public void notifySpecificUsers(String title, String message, String priority, Set<Long> userIds) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.userNotification(
                title, message, priority, null, userIds
            );
            
            // Send to each specific user
            for (Long userId : userIds) {
                String destination = "/queue/user/" + userId + "/notifications";
                messagingTemplate.convertAndSend(destination, notification);
                log.debug("Sent notification to user {}: {}", userId, title);
            }
            
        } catch (Exception e) {
            log.error("Failed to send user-specific notifications", e);
        }
    }

    /**
     * Broadcast system alert to admins
     */
    public void broadcastSystemAlert(String title, String message, String priority) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.systemAlert(
                title, message, priority
            );
            
            messagingTemplate.convertAndSend("/topic/system-alerts", notification);
            log.debug("Broadcast system alert: {}", title);
            
        } catch (Exception e) {
            log.error("Failed to broadcast system alert", e);
        }
    }

    /**
     * Send grade notification to student and parent
     */
    public void notifyGradePosted(String studentName, String courseName, String className, 
                                 Double score, Long studentId, Long parentId) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.gradeNotification(
                studentName, courseName, className, score, studentId, parentId
            );
            
            // Send to specific student and parent
            messagingTemplate.convertAndSend("/queue/user/" + studentId + "/notifications", notification);
            if (parentId != null) {
                messagingTemplate.convertAndSend("/queue/user/" + parentId + "/notifications", notification);
            }
            
            // Also send to parent/student general topics
            messagingTemplate.convertAndSend("/topic/notifications/student", notification);
            messagingTemplate.convertAndSend("/topic/notifications/parent", notification);
            
            log.debug("Sent grade notification for {} in {}", studentName, courseName);
            
        } catch (Exception e) {
            log.error("Failed to send grade notification", e);
        }
    }

    /**
     * Send enrollment notification to student and parent
     */
    public void notifyEnrollmentChange(String studentName, String className, String action, 
                                     Long studentId, Long parentId) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.enrollmentNotification(
                studentName, className, action, studentId, parentId
            );
            
            // Send to specific student and parent
            messagingTemplate.convertAndSend("/queue/user/" + studentId + "/notifications", notification);
            if (parentId != null) {
                messagingTemplate.convertAndSend("/queue/user/" + parentId + "/notifications", notification);
            }
            
            // Also send to parent/student general topics
            messagingTemplate.convertAndSend("/topic/notifications/student", notification);
            messagingTemplate.convertAndSend("/topic/notifications/parent", notification);
            
            log.debug("Sent enrollment notification: {} {} {}", studentName, action, className);
            
        } catch (Exception e) {
            log.error("Failed to send enrollment notification", e);
        }
    }

    /**
     * Broadcast announcement notification to target roles
     */
    public void notifyNewAnnouncement(String title, String content, String importance, Set<String> targetRoles) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.announcementNotification(
                title, content, importance, targetRoles
            );
            
            // Send to each target role
            for (String role : targetRoles) {
                String destination = "/topic/notifications/" + role.toLowerCase();
                messagingTemplate.convertAndSend(destination, notification);
                log.debug("Sent announcement notification to role {}: {}", role, title);
            }
            
            // Also send to general announcements topic
            messagingTemplate.convertAndSend("/topic/announcements", notification);
            
        } catch (Exception e) {
            log.error("Failed to send announcement notification", e);
        }
    }

    /**
     * Send general notification to all connected users
     */
    public void broadcastToAll(String title, String message, String priority) {
        try {
            Set<String> allRoles = Set.of("ADMIN", "TEACHER", "PARENT", "STUDENT");
            RealTimeNotificationDto notification = RealTimeNotificationDto.userNotification(
                title, message, priority, allRoles, null
            );
            
            messagingTemplate.convertAndSend("/topic/notifications/broadcast", notification);
            log.debug("Broadcast notification to all users: {}", title);
            
        } catch (Exception e) {
            log.error("Failed to broadcast notification to all users", e);
        }
    }

    /**
     * Send connection confirmation to user
     */
    public void sendConnectionConfirmation(Long userId, String userRole) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.userNotification(
                "Connected",
                "You are now connected to real-time notifications",
                "LOW",
                Set.of(userRole),
                Set.of(userId)
            );
            
            messagingTemplate.convertAndSend("/queue/user/" + userId + "/notifications", notification);
            log.debug("Sent connection confirmation to user {} with role {}", userId, userRole);
            
        } catch (Exception e) {
            log.error("Failed to send connection confirmation", e);
        }
    }
} 