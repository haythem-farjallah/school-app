package com.example.school_management.feature.communication.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.communication.service.SMSService;
import com.example.school_management.feature.communication.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Sends to caller-chosen recipients are administrative until communication authorization is redesigned.
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class CommunicationController {

    private final EmailService emailService;
    private final SMSService smsService;
    private final PushNotificationService pushNotificationService;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${app.websocket.enabled:true}")
    private boolean webSocketEnabled;

    // =====================================================
    // EMAIL ENDPOINTS
    // =====================================================

    @PostMapping("/email/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<EmailResponse>> sendEmail(@Valid @RequestBody EmailRequest emailRequest) {
        log.info("📧 API: Sending email to: {}", emailRequest.getRecipientEmail());
        EmailResponse response = emailService.sendEmail(emailRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/email/send-templated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<SMSResponse>> sendSMS(@Valid @RequestBody SMSRequest smsRequest) {
        log.info("📱 API: Sending SMS to: {}", maskPhoneNumber(smsRequest.getRecipientPhone()));
        SMSResponse response = smsService.sendSMS(smsRequest);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PostMapping("/sms/send-templated")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<String>> sendRealTimeNotification(
            @RequestParam String userId,
            @RequestBody Map<String, Object> payload) {
        log.info("📡 API: Sending real-time notification to user: {}", userId);
        pushNotificationService.sendRealTimeNotification(userId, payload);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Real-time notification dispatched"));
    }

    @PostMapping("/realtime/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<String>> broadcastRealTimeNotification(@RequestBody Map<String, Object> payload) {
        log.info("📡 API: Broadcasting real-time notification to all connected users");
        pushNotificationService.broadcastRealTimeNotification(payload);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Real-time notification broadcast dispatched"));
    }

    @GetMapping("/realtime/connections")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getWebSocketConnections() {
        log.info("🔌 API: Getting WebSocket connection information");
        Map<String, Object> connectionInfo = Map.of(
                "trackingAvailable", false
        );
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", connectionInfo));
    }

    // =====================================================
    // UTILITY ENDPOINTS
    // =====================================================

    @GetMapping("/health")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> getNotificationSystemHealth() {
        log.info("🏥 API: Getting notification system health");
        Map<String, Object> health = Map.of(
                "emailService", smtpHost == null || smtpHost.isBlank() ? "notConfigured" : "configured_unverified",
                "smsService", "notConfigured",
                "pushNotificationService", "notConfigured",
                "webSocketService", webSocketEnabled ? "enabled" : "disabled",
                "connectionTracking", "unavailable",
                "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", health));
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }
}