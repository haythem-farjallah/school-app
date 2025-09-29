package com.example.school_management.feature.communication.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.communication.service.SMSService;
import com.example.school_management.feature.communication.service.PushNotificationService;
import com.example.school_management.feature.communication.entity.Notification;
import com.example.school_management.feature.communication.repository.CommunicationNotificationRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.operational.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class CommunicationController {

    private final EmailService emailService;
    private final SMSService smsService;
    private final PushNotificationService pushNotificationService;
    private final CommunicationNotificationRepository communicationNotificationRepository;
    private final BaseUserRepository<BaseUser> userRepository;

    // =====================================================
    // EMAIL ENDPOINTS
    // =====================================================

    @PostMapping("/email/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<EmailResponse>> sendEmail(@Valid @RequestBody EmailRequest emailRequest) {
        log.info("📧 API: Sending email to: {}", emailRequest.getRecipientEmail());
        EmailResponse response = emailService.sendEmail(emailRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/email/send-templated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<EmailResponse>> sendTemplatedEmail(
            @RequestParam String templateName,
            @RequestParam String recipientEmail,
            @RequestBody Map<String, Object> variables) {
        log.info("📧 API: Sending templated email '{}' to: {}", templateName, recipientEmail);
        EmailResponse response = emailService.sendTemplatedEmail(templateName, recipientEmail, variables);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/email/send-bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<EmailResponse>>> sendBulkEmails(@Valid @RequestBody BulkEmailRequest bulkEmailRequest) {
        log.info("📧 API: Sending bulk emails to {} recipients", bulkEmailRequest.getRecipients().size());
        List<EmailResponse> responses = emailService.sendBulkEmails(bulkEmailRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", responses));
    }

    @GetMapping("/email/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getEmailAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("📊 API: Getting email analytics from {} to {}", startDate, endDate);
        Map<String, Object> analytics = emailService.getEmailAnalytics(startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", analytics));
    }

    // =====================================================
    // SMS ENDPOINTS
    // =====================================================

    @PostMapping("/sms/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<SMSResponse>> sendSMS(@Valid @RequestBody SMSRequest smsRequest) {
        log.info("📱 API: Sending SMS to: {}", maskPhoneNumber(smsRequest.getRecipientPhone()));
        SMSResponse response = smsService.sendSMS(smsRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/sms/send-templated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<SMSResponse>> sendTemplatedSMS(
            @RequestParam String templateName,
            @RequestParam String recipientPhone,
            @RequestBody Map<String, Object> variables) {
        log.info("📱 API: Sending templated SMS '{}' to: {}", templateName, maskPhoneNumber(recipientPhone));
        SMSResponse response = smsService.sendTemplatedSMS(templateName, recipientPhone, variables);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/sms/send-bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<SMSResponse>>> sendBulkSMS(@Valid @RequestBody BulkSMSRequest bulkSMSRequest) {
        log.info("📱 API: Sending bulk SMS to {} recipients", bulkSMSRequest.getTotalRecipients());
        List<SMSResponse> responses = smsService.sendBulkSMS(bulkSMSRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", responses));
    }

    @PostMapping("/sms/send-otp")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiSuccessResponse<SMSResponse>> sendOTP(
            @RequestParam String recipientPhone,
            @RequestParam String otp,
            @RequestParam(defaultValue = "10") int expiryMinutes) {
        log.info("📱 API: Sending OTP to: {}", maskPhoneNumber(recipientPhone));
        SMSResponse response = smsService.sendOTP(recipientPhone, otp, expiryMinutes);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @GetMapping("/sms/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getSMSAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("📊 API: Getting SMS analytics from {} to {}", startDate, endDate);
        Map<String, Object> analytics = smsService.getSMSAnalytics(startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", analytics));
    }

    // =====================================================
    // PUSH NOTIFICATION ENDPOINTS
    // =====================================================

    @PostMapping("/push/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<PushNotificationResponse>> sendPushNotification(@Valid @RequestBody PushNotificationRequest pushRequest) {
        log.info("🔔 API: Sending push notification to: {}", pushRequest.getRecipientId());
        PushNotificationResponse response = pushNotificationService.sendPushNotification(pushRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/push/send-bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<PushNotificationResponse>>> sendBulkPushNotifications(@Valid @RequestBody BulkPushNotificationRequest bulkRequest) {
        log.info("🔔 API: Sending bulk push notifications to {} recipients", bulkRequest.getTotalRecipients());
        List<PushNotificationResponse> responses = pushNotificationService.sendBulkPushNotifications(bulkRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", responses));
    }

    @PostMapping("/push/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<PushNotificationResponse>>> broadcastPushNotification(@Valid @RequestBody PushNotificationRequest pushRequest) {
        log.info("📢 API: Broadcasting push notification to all users");
        List<PushNotificationResponse> responses = pushNotificationService.sendToAllUsers(pushRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", responses));
    }

    @PostMapping("/push/register-device")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiSuccessResponse<String>> registerDeviceToken(
            @RequestParam String userId,
            @RequestParam String deviceToken,
            @RequestParam String platform) {
        log.info("📱 API: Registering device token for user: {} on platform: {}", userId, platform);
        pushNotificationService.registerDeviceToken(userId, deviceToken, platform);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Device registered successfully"));
    }

    @GetMapping("/push/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getPushNotificationAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("📊 API: Getting push notification analytics from {} to {}", startDate, endDate);
        Map<String, Object> analytics = pushNotificationService.getPushNotificationAnalytics(startDate, endDate);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", analytics));
    }

    // =====================================================
    // USER PREFERENCES ENDPOINTS
    // =====================================================

    @GetMapping("/preferences/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or #userId == authentication.name")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getUserNotificationPreferences(@PathVariable String userId) {
        log.info("⚙️ API: Getting notification preferences for user: {}", userId);
        Map<String, Object> preferences = pushNotificationService.getUserNotificationPreferences(userId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", preferences));
    }

    @PutMapping("/preferences/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF') or #userId == authentication.name")
    public ResponseEntity<ApiSuccessResponse<String>> updateUserNotificationPreferences(
            @PathVariable String userId,
            @RequestBody Map<String, Object> preferences) {
        log.info("⚙️ API: Updating notification preferences for user: {}", userId);
        pushNotificationService.updateUserNotificationPreferences(userId, preferences);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Preferences updated successfully"));
    }

    // =====================================================
    // REAL-TIME WEBSOCKET ENDPOINTS
    // =====================================================

    @PostMapping("/realtime/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<String>> sendRealTimeNotification(
            @RequestParam String userId,
            @RequestBody Map<String, Object> payload) {
        log.info("📡 API: Sending real-time notification to user: {}", userId);
        pushNotificationService.sendRealTimeNotification(userId, payload);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Real-time notification sent"));
    }

    @PostMapping("/realtime/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<String>> broadcastRealTimeNotification(@RequestBody Map<String, Object> payload) {
        log.info("📡 API: Broadcasting real-time notification to all connected users");
        pushNotificationService.broadcastRealTimeNotification(payload);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Real-time notification broadcasted"));
    }

    @GetMapping("/realtime/connections")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getWebSocketConnections() {
        log.info("🔌 API: Getting WebSocket connection information");
        Map<String, Object> connectionInfo = Map.of(
                "activeConnections", pushNotificationService.getActiveConnectionsCount(),
                "connectedUsers", pushNotificationService.getConnectedUsers()
        );
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", connectionInfo));
    }

    // =====================================================
    // NOTIFICATION FETCH ENDPOINTS
    // =====================================================

    @GetMapping("/v1/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiSuccessResponse<List<NotificationDto>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean readStatus) {
        log.info("📋 API: Getting notifications for current user - page: {}, size: {}, readStatus: {}", page, size, readStatus);
        
        try {
            // Get current user from security context
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            
            // Find user by email
            BaseUser currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            log.info("🔍 Found current user: {} (ID: {})", currentUser.getEmail(), currentUser.getId());
            
            // Get notifications from communication table
            Notification.RecipientType recipientType = getRecipientType(currentUser);
            List<com.example.school_management.feature.communication.entity.Notification> notifications;
            
            if (readStatus != null) {
                if (readStatus) {
                    // Get read notifications (where readAt is NOT null) - we'll need to filter manually
                    List<com.example.school_management.feature.communication.entity.Notification> allNotifications = 
                        communicationNotificationRepository.findByRecipientIdAndRecipientType(currentUser.getId(), recipientType);
                    notifications = allNotifications.stream()
                        .filter(n -> n.getReadAt() != null)
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    notifications = communicationNotificationRepository.findUnreadByRecipient(
                        currentUser.getId(), recipientType);
                }
            } else {
                notifications = communicationNotificationRepository.findByRecipientIdAndRecipientType(
                    currentUser.getId(), recipientType);
            }
            
            log.info("📋 Found {} notifications for user {}", notifications.size(), currentUser.getEmail());
            
            // Convert to DTOs
            List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::convertToDto)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", notificationDtos));
            
        } catch (Exception e) {
            log.error("❌ Error getting notifications: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", List.of()));
        }
    }

    @GetMapping("/v1/notifications/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiSuccessResponse<Integer>> getUnreadNotificationCount() {
        log.info("📊 API: Getting unread notification count for current user");
        
        try {
            // Get current user from security context
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            
            // Find user by email
            BaseUser currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            
            // Get unread count
            Notification.RecipientType recipientType = getRecipientType(currentUser);
            List<com.example.school_management.feature.communication.entity.Notification> unreadNotifications = 
                communicationNotificationRepository.findUnreadByRecipient(currentUser.getId(), recipientType);
            
            int unreadCount = unreadNotifications.size();
            log.info("📊 Found {} unread notifications for user {}", unreadCount, currentUser.getEmail());
            
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", unreadCount));
            
        } catch (Exception e) {
            log.error("❌ Error getting unread count: {}", e.getMessage(), e);
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", 0));
        }
    }

    // =====================================================
    // UTILITY ENDPOINTS
    // =====================================================

    @GetMapping("/health")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getNotificationSystemHealth() {
        log.info("🏥 API: Getting notification system health");
        Map<String, Object> health = Map.of(
                "emailService", "healthy",
                "smsService", "healthy",
                "pushNotificationService", "healthy",
                "webSocketService", "healthy",
                "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", health));
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

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

    /**
     * Convert communication notification entity to DTO
     */
    private NotificationDto convertToDto(Notification notification) {
        // Map notification type
        com.example.school_management.feature.operational.entity.enums.NotificationType mappedType = null;
        if (notification.getNotificationType() != null) {
            mappedType = mapNotificationType(notification.getNotificationType());
        }
        
        // Extract metadata for entity info
        String entityType = "ATTENDANCE";
        Long entityId = null;
        String actionUrl = null;
        
        if (notification.getMetadata() != null) {
            try {
                // Simple parsing - could be improved with JSON library
                if (notification.getMetadata().contains("attendanceId")) {
                    entityType = "ATTENDANCE";
                    // Try to extract ID - simple approach
                    String metadata = notification.getMetadata();
                    if (metadata.contains("attendanceId\":")) {
                        int start = metadata.indexOf("attendanceId\":") + 14;
                        int end = metadata.indexOf(",", start);
                        if (end == -1) end = metadata.indexOf("}", start);
                        if (end != -1) {
                            try {
                                entityId = Long.parseLong(metadata.substring(start, end));
                                actionUrl = "/attendance/" + entityId;
                            } catch (NumberFormatException e) {
                                // Ignore parsing errors
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore metadata parsing errors
            }
        }
        
        return new NotificationDto(
            notification.getId(),
            notification.getTitle(),
            notification.getContent(),
            mappedType,
            entityType,
            entityId,
            actionUrl,
            notification.getReadAt() != null,
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }

    /**
     * Map communication notification type to operational notification type
     */
    private com.example.school_management.feature.operational.entity.enums.NotificationType mapNotificationType(
            Notification.NotificationType communicationType) {
        switch (communicationType) {
            case ATTENDANCE_ALERT:
                return com.example.school_management.feature.operational.entity.enums.NotificationType.ATTENDANCE_MARKED;
            case ANNOUNCEMENT:
                return com.example.school_management.feature.operational.entity.enums.NotificationType.ANNOUNCEMENT_PUBLISHED;
            case GRADE_PUBLISHED:
                return com.example.school_management.feature.operational.entity.enums.NotificationType.GRADE_ADDED;
            case ASSIGNMENT_DUE:
                return com.example.school_management.feature.operational.entity.enums.NotificationType.ASSIGNMENT_DUE;
            default:
                return com.example.school_management.feature.operational.entity.enums.NotificationType.GENERAL;
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }
}