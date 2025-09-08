package com.example.school_management.feature.communication.service;

import com.example.school_management.feature.communication.dto.PushNotificationRequest;
import com.example.school_management.feature.communication.dto.BulkPushNotificationRequest;
import com.example.school_management.feature.communication.dto.PushNotificationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PushNotificationService {

    /**
     * Send a single push notification
     */
    PushNotificationResponse sendPushNotification(PushNotificationRequest pushRequest);

    /**
     * Send push notification using template
     */
    PushNotificationResponse sendTemplatedPushNotification(String templateName, String recipientId, Map<String, Object> variables);

    /**
     * Send bulk push notifications
     */
    List<PushNotificationResponse> sendBulkPushNotifications(BulkPushNotificationRequest bulkRequest);

    /**
     * Send push notification to all users of a specific role
     */
    List<PushNotificationResponse> sendToRole(String role, PushNotificationRequest pushRequest);

    /**
     * Send push notification to a specific class
     */
    List<PushNotificationResponse> sendToClass(Long classId, PushNotificationRequest pushRequest);

    /**
     * Send push notification to all users
     */
    List<PushNotificationResponse> sendToAllUsers(PushNotificationRequest pushRequest);

    /**
     * Schedule push notification for later sending
     */
    PushNotificationResponse schedulePushNotification(PushNotificationRequest pushRequest, LocalDateTime scheduledAt);

    /**
     * Send real-time notification via WebSocket
     */
    void sendRealTimeNotification(String userId, Map<String, Object> payload);

    /**
     * Send real-time notification to multiple users
     */
    void sendRealTimeNotificationToUsers(List<String> userIds, Map<String, Object> payload);

    /**
     * Send real-time notification to all connected users
     */
    void broadcastRealTimeNotification(Map<String, Object> payload);

    /**
     * Register device token for push notifications
     */
    void registerDeviceToken(String userId, String deviceToken, String platform);

    /**
     * Unregister device token
     */
    void unregisterDeviceToken(String userId, String deviceToken);

    /**
     * Get registered device tokens for user
     */
    List<String> getUserDeviceTokens(String userId);

    /**
     * Send emergency alert to all users
     */
    List<PushNotificationResponse> sendEmergencyAlert(String title, String message, Map<String, Object> data);

    /**
     * Send grade notification
     */
    PushNotificationResponse sendGradeNotification(String studentId, String courseName, String grade);

    /**
     * Send assignment reminder
     */
    PushNotificationResponse sendAssignmentReminder(String studentId, String assignmentTitle, LocalDateTime dueDate);

    /**
     * Send attendance alert
     */
    PushNotificationResponse sendAttendanceAlert(String parentId, String studentName, String alertType);

    /**
     * Send announcement notification
     */
    List<PushNotificationResponse> sendAnnouncementNotification(String title, String content, List<String> targetUserIds);

    /**
     * Get push notification analytics
     */
    Map<String, Object> getPushNotificationAnalytics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get user notification preferences
     */
    Map<String, Object> getUserNotificationPreferences(String userId);

    /**
     * Update user notification preferences
     */
    void updateUserNotificationPreferences(String userId, Map<String, Object> preferences);

    /**
     * Check if user has enabled push notifications
     */
    boolean isPushNotificationEnabled(String userId);

    /**
     * Get notification delivery status
     */
    String getNotificationStatus(String notificationId);

    /**
     * Retry failed push notification
     */
    PushNotificationResponse retryFailedNotification(Long notificationId);

    /**
     * Get active WebSocket connections count
     */
    int getActiveConnectionsCount();

    /**
     * Get connected users
     */
    List<String> getConnectedUsers();

    /**
     * Disconnect user session
     */
    void disconnectUser(String userId);
}
