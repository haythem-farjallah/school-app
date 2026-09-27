package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.RealTimeNotificationDto;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Pushes notifications from the server to connected browsers. Notifications about one person go to
 * that person's own queue only; role topics carry only content meant for everyone in the role.
 * Logs name destinations and ids, never notification content.
 */
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
            log.debug("Admin feed notification sent");

        } catch (Exception e) {
            log.error("Realtime notification delivery failed: {}", e.getClass().getSimpleName());
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
                sendToUser(userId, notification);
                log.debug("Notification sent to user id={}", userId);
            }

        } catch (Exception e) {
            log.error("Realtime notification delivery failed: {}", e.getClass().getSimpleName());
        }
    }

    /**
     * Send an enrollment notification to the student, and to the parent when one is given
     */
    public void notifyEnrollmentChange(String studentName, String className, String action,
                                     Long studentId, Long parentId) {
        try {
            RealTimeNotificationDto notification = RealTimeNotificationDto.enrollmentNotification(
                studentName, className, action, studentId, parentId
            );

            sendToUser(studentId, notification);
            log.debug("Enrollment notification sent to student id={}", studentId);
            if (parentId != null) {
                sendToUser(parentId, notification);
                log.debug("Enrollment notification sent to parent id={}", parentId);
            }

        } catch (Exception e) {
            log.error("Realtime notification delivery failed: {}", e.getClass().getSimpleName());
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
                log.debug("Notification sent to role {}", role);
            }

        } catch (Exception e) {
            log.error("Realtime notification delivery failed: {}", e.getClass().getSimpleName());
        }
    }

    private void sendToUser(Long userId, RealTimeNotificationDto notification) {
        messagingTemplate.convertAndSend("/queue/user/" + userId + "/notifications", notification);
    }
}
