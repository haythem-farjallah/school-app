package com.example.school_management.feature.communication.service.impl;

import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.repository.*;
import com.example.school_management.feature.communication.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final CommunicationNotificationRepository notificationRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.websocket.enabled:true}")
    private boolean webSocketEnabled;

    // In-memory storage for device tokens and user preferences
    // In production, these would be stored in database
    private final Map<String, List<String>> userDeviceTokens = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userPreferences = new ConcurrentHashMap<>();

    @Override
    public PushNotificationResponse sendPushNotification(PushNotificationRequest pushRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public PushNotificationResponse sendTemplatedPushNotification(String templateName, String recipientId, Map<String, Object> variables) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public List<PushNotificationResponse> sendBulkPushNotifications(BulkPushNotificationRequest bulkRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public List<PushNotificationResponse> sendToRole(String role, PushNotificationRequest pushRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PUSH_AUDIENCE_UNAVAILABLE");
    }

    @Override
    public List<PushNotificationResponse> sendToClass(Long classId, PushNotificationRequest pushRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PUSH_AUDIENCE_UNAVAILABLE");
    }

    @Override
    public List<PushNotificationResponse> sendToAllUsers(PushNotificationRequest pushRequest) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PUSH_AUDIENCE_UNAVAILABLE");
    }

    @Override
    public PushNotificationResponse schedulePushNotification(PushNotificationRequest pushRequest, LocalDateTime scheduledAt) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public void sendRealTimeNotification(String userId, Map<String, Object> payload) {
        requireWebSocket();
        try {
            messagingTemplate.convertAndSend("/user/" + userId + "/notifications", payload);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WEBSOCKET_DISPATCH_FAILED");
        }
    }

    @Override
    public void sendRealTimeNotificationToUsers(List<String> userIds, Map<String, Object> payload) {
        requireWebSocket();
        userIds.forEach(userId -> sendRealTimeNotification(userId, payload));
    }

    @Override
    public void broadcastRealTimeNotification(Map<String, Object> payload) {
        requireWebSocket();
        try {
            messagingTemplate.convertAndSend("/topic/notifications", payload);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WEBSOCKET_DISPATCH_FAILED");
        }
    }

    @Override
    public void unregisterDeviceToken(String userId, String deviceToken) {
        log.info("📱 Unregistering device token for user: {}", userId);
        
        List<String> tokens = userDeviceTokens.get(userId);
        if (tokens != null) {
            tokens.remove(deviceToken);
            if (tokens.isEmpty()) {
                userDeviceTokens.remove(userId);
            }
        }
        
        log.info("✅ Device token unregistered successfully for user: {}", userId);
    }

    @Override
    public List<String> getUserDeviceTokens(String userId) {
        return userDeviceTokens.getOrDefault(userId, new ArrayList<>());
    }

    @Override
    public List<PushNotificationResponse> sendEmergencyAlert(String title, String message, Map<String, Object> data) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PUSH_AUDIENCE_UNAVAILABLE");
    }

    @Override
    public PushNotificationResponse sendGradeNotification(String studentId, String courseName, String grade) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public PushNotificationResponse sendAssignmentReminder(String studentId, String assignmentTitle, LocalDateTime dueDate) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public PushNotificationResponse sendAttendanceAlert(String parentId, String studentName, String alertType) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public List<PushNotificationResponse> sendAnnouncementNotification(String title, String content, List<String> targetUserIds) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public Map<String, Object> getPushNotificationAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("📊 Generating push notification analytics from {} to {}", startDate, endDate);

        Map<String, Object> analytics = new HashMap<>();

        // Get basic stats
        List<Object[]> statusStats = notificationRepository.getNotificationStatusStats(startDate, endDate);
        Map<String, Long> statusMap = statusStats.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
        analytics.put("statusBreakdown", statusMap);

        // Get delivery stats
        List<Object[]> deliveryStats = communicationLogRepository.getDeliveryStatusStats(startDate, endDate);
        Map<String, Long> deliveryMap = deliveryStats.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
        analytics.put("deliveryBreakdown", deliveryMap);

        // Get engagement metrics
        Long openedCount = communicationLogRepository.getOpenedCount(startDate, endDate);
        Long clickedCount = communicationLogRepository.getClickedCount(startDate, endDate);
        analytics.put("openedCount", openedCount);
        analytics.put("clickedCount", clickedCount);

        analytics.put("connectionTrackingAvailable", false);

        return analytics;
    }

    @Override
    public Map<String, Object> getUserNotificationPreferences(String userId) {
        return userPreferences.getOrDefault(userId, getDefaultPreferences());
    }

    @Override
    public void updateUserNotificationPreferences(String userId, Map<String, Object> preferences) {
        userPreferences.put(userId, preferences);
        log.info("✅ Updated notification preferences for user: {}", userId);
    }

    @Override
    public boolean isPushNotificationEnabled(String userId) {
        Map<String, Object> preferences = getUserNotificationPreferences(userId);
        return (Boolean) preferences.getOrDefault("pushNotificationsEnabled", true);
    }

    @Override
    public String getNotificationStatus(String notificationId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public PushNotificationResponse retryFailedNotification(Long notificationId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "FCM_PROVIDER_NOT_CONFIGURED");
    }

    @Override
    public int getActiveConnectionsCount() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CONNECTION_TRACKING_UNAVAILABLE");
    }

    @Override
    public List<String> getConnectedUsers() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CONNECTION_TRACKING_UNAVAILABLE");
    }

    @Override
    public void disconnectUser(String userId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CONNECTION_TRACKING_UNAVAILABLE");
    }

    private void requireWebSocket() {
        if (!webSocketEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WEBSOCKET_DISABLED");
        }
    }

    private Map<String, Object> getDefaultPreferences() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("pushNotificationsEnabled", true);
        defaults.put("soundEnabled", true);
        defaults.put("vibrationEnabled", true);
        defaults.put("quietHoursEnabled", false);
        defaults.put("quietHoursStart", 22);
        defaults.put("quietHoursEnd", 7);
        return defaults;
    }
}
