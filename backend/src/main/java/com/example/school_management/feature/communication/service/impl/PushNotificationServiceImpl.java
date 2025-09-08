package com.example.school_management.feature.communication.service.impl;

import com.example.school_management.feature.communication.dto.*;
import com.example.school_management.feature.communication.entity.*;
import com.example.school_management.feature.communication.repository.*;
import com.example.school_management.feature.communication.service.PushNotificationService;
import com.example.school_management.feature.communication.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final CommunicationNotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final NotificationTemplateService templateService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.push.fcm.server-key:}")
    private String fcmServerKey;

    @Value("${app.push.fcm.sender-id:}")
    private String fcmSenderId;

    @Value("${app.push.async.enabled:true}")
    private boolean asyncEnabled;

    @Value("${app.websocket.enabled:true}")
    private boolean webSocketEnabled;

    // In-memory storage for device tokens and user preferences
    // In production, these would be stored in database
    private final Map<String, List<String>> userDeviceTokens = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userPreferences = new ConcurrentHashMap<>();
    private final Map<String, String> activeWebSocketSessions = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public PushNotificationResponse sendPushNotification(PushNotificationRequest pushRequest) {
        log.info("🔔 Sending push notification to: {}", pushRequest.getRecipientId());

        try {
            // Check user preferences
            if (!isPushNotificationEnabled(pushRequest.getRecipientId())) {
                return PushNotificationResponse.failure(pushRequest.getRecipientId(), 
                        "Push notifications disabled for user", "DISABLED");
            }

            // Create notification record
            Notification notification = createNotificationRecord(pushRequest);
            notification = notificationRepository.save(notification);

            // Try WebSocket delivery first (real-time)
            if (webSocketEnabled && isUserConnected(pushRequest.getRecipientId())) {
                PushNotificationResponse webSocketResponse = sendViaWebSocket(pushRequest, notification.getId());
                if (webSocketResponse.getSuccess()) {
                    updateNotificationStatus(notification, Notification.NotificationStatus.DELIVERED);
                    return webSocketResponse;
                }
            }

            // Fallback to FCM
            List<String> deviceTokens = getUserDeviceTokens(pushRequest.getRecipientId());
            if (deviceTokens.isEmpty()) {
                return PushNotificationResponse.failure(pushRequest.getRecipientId(), 
                        "No device tokens found", "NO_TOKENS");
            }

            // Send via FCM
            PushNotificationResponse fcmResponse = sendViaFCM(pushRequest, deviceTokens, notification.getId());
            
            // Update notification status
            if (fcmResponse.getSuccess()) {
                updateNotificationStatus(notification, Notification.NotificationStatus.SENT);
            } else {
                updateNotificationStatus(notification, Notification.NotificationStatus.FAILED);
            }

            // Create communication log
            createCommunicationLog(notification, fcmResponse);

            log.info("✅ Push notification sent successfully to: {}", pushRequest.getRecipientId());
            return fcmResponse;

        } catch (Exception e) {
            log.error("❌ Failed to send push notification to: {}", pushRequest.getRecipientId(), e);
            return PushNotificationResponse.failure(pushRequest.getRecipientId(), e.getMessage(), "SEND_ERROR");
        }
    }

    @Override
    @Transactional
    public PushNotificationResponse sendTemplatedPushNotification(String templateName, String recipientId, Map<String, Object> variables) {
        log.info("🔔 Sending templated push notification '{}' to: {}", templateName, recipientId);

        try {
            // Get template
            Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateNameAndTemplateTypeAndLanguage(
                    templateName, NotificationTemplate.TemplateType.PUSH_NOTIFICATION, "en"
            );

            if (templateOpt.isEmpty()) {
                return PushNotificationResponse.failure(recipientId, 
                        "Push notification template not found: " + templateName, "TEMPLATE_NOT_FOUND");
            }

            NotificationTemplate template = templateOpt.get();

            // Process template
            String processedTitle = templateService.processTemplate(template.getSubject(), variables);
            String processedBody = templateService.processTemplate(template.getContent(), variables);

            // Create push request
            PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                    .recipientId(recipientId)
                    .title(processedTitle)
                    .body(processedBody)
                    .templateName(templateName)
                    .templateVariables(variables)
                    .build();

            return sendPushNotification(pushRequest);

        } catch (Exception e) {
            log.error("❌ Failed to send templated push notification '{}' to: {}", templateName, recipientId, e);
            return PushNotificationResponse.failure(recipientId, e.getMessage(), "TEMPLATE_ERROR");
        }
    }

    @Override
    @Transactional
    public List<PushNotificationResponse> sendBulkPushNotifications(BulkPushNotificationRequest bulkRequest) {
        log.info("🔔 Sending bulk push notifications to {} recipients", bulkRequest.getTotalRecipients());

        List<PushNotificationResponse> responses = new ArrayList<>();
        List<BulkPushNotificationRequest.BulkPushRecipient> recipients = bulkRequest.getEnabledRecipients();

        // Process in batches
        int batchSize = bulkRequest.getBatchSize();
        for (int i = 0; i < recipients.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, recipients.size());
            List<BulkPushNotificationRequest.BulkPushRecipient> batch = recipients.subList(i, endIndex);

            // Process batch
            List<PushNotificationResponse> batchResponses = processPushBatch(batch, bulkRequest);
            responses.addAll(batchResponses);

            // Delay between batches
            if (endIndex < recipients.size() && bulkRequest.getDelayBetweenBatches() > 0) {
                try {
                    Thread.sleep(bulkRequest.getDelayBetweenBatches());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Bulk push notification processing interrupted");
                    break;
                }
            }
        }

        log.info("✅ Bulk push notifications completed. Sent: {}, Failed: {}", 
                responses.stream().mapToLong(r -> r.getSuccess() ? 1 : 0).sum(),
                responses.stream().mapToLong(r -> r.getSuccess() ? 0 : 1).sum());

        return responses;
    }

    @Override
    public List<PushNotificationResponse> sendToRole(String role, PushNotificationRequest pushRequest) {
        log.info("🔔 Sending push notification to all users with role: {}", role);
        
        // In a real implementation, this would query the user repository for users with the specified role
        List<String> userIds = getUserIdsByRole(role);
        
        return userIds.stream()
                .map(userId -> {
                    PushNotificationRequest roleRequest = PushNotificationRequest.builder()
                            .recipientId(userId)
                            .title(pushRequest.getTitle())
                            .body(pushRequest.getBody())
                            .icon(pushRequest.getIcon())
                            .image(pushRequest.getImage())
                            .sound(pushRequest.getSound())
                            .clickAction(pushRequest.getClickAction())
                            .priority(pushRequest.getPriority())
                            .data(pushRequest.getData())
                            .build();
                    return sendPushNotification(roleRequest);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PushNotificationResponse> sendToClass(Long classId, PushNotificationRequest pushRequest) {
        log.info("🔔 Sending push notification to all users in class: {}", classId);
        
        // In a real implementation, this would query for students and teachers in the class
        List<String> userIds = getUserIdsByClass(classId);
        
        return userIds.stream()
                .map(userId -> {
                    PushNotificationRequest classRequest = PushNotificationRequest.builder()
                            .recipientId(userId)
                            .title(pushRequest.getTitle())
                            .body(pushRequest.getBody())
                            .icon(pushRequest.getIcon())
                            .image(pushRequest.getImage())
                            .sound(pushRequest.getSound())
                            .clickAction(pushRequest.getClickAction())
                            .priority(pushRequest.getPriority())
                            .data(pushRequest.getData())
                            .build();
                    return sendPushNotification(classRequest);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PushNotificationResponse> sendToAllUsers(PushNotificationRequest pushRequest) {
        log.info("🔔 Broadcasting push notification to all users");
        
        // In a real implementation, this would query all active users
        List<String> allUserIds = getAllActiveUserIds();
        
        BulkPushNotificationRequest bulkRequest = BulkPushNotificationRequest.builder()
                .recipients(allUserIds.stream()
                        .map(userId -> BulkPushNotificationRequest.BulkPushRecipient.builder()
                                .recipientId(userId)
                                .build())
                        .collect(Collectors.toList()))
                .title(pushRequest.getTitle())
                .body(pushRequest.getBody())
                .icon(pushRequest.getIcon())
                .image(pushRequest.getImage())
                .sound(pushRequest.getSound())
                .clickAction(pushRequest.getClickAction())
                .priority(pushRequest.getPriority())
                .globalData(pushRequest.getData())
                .build();
        
        return sendBulkPushNotifications(bulkRequest);
    }

    @Override
    @Transactional
    public PushNotificationResponse schedulePushNotification(PushNotificationRequest pushRequest, LocalDateTime scheduledAt) {
        log.info("📅 Scheduling push notification to: {} for: {}", pushRequest.getRecipientId(), scheduledAt);

        try {
            // Create notification record with scheduled status
            Notification notification = createNotificationRecord(pushRequest);
            notification.setScheduledAt(scheduledAt);
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification = notificationRepository.save(notification);

            log.info("✅ Push notification scheduled successfully for: {}", pushRequest.getRecipientId());
            return PushNotificationResponse.scheduled(notification.getId(), pushRequest.getRecipientId(), scheduledAt);

        } catch (Exception e) {
            log.error("❌ Failed to schedule push notification to: {}", pushRequest.getRecipientId(), e);
            return PushNotificationResponse.failure(pushRequest.getRecipientId(), e.getMessage(), "SCHEDULE_ERROR");
        }
    }

    @Override
    public void sendRealTimeNotification(String userId, Map<String, Object> payload) {
        if (!webSocketEnabled) {
            log.warn("WebSocket is disabled, cannot send real-time notification");
            return;
        }

        try {
            String destination = "/user/" + userId + "/notifications";
            messagingTemplate.convertAndSend(destination, payload);
            log.info("📡 Real-time notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to send real-time notification to user: {}", userId, e);
        }
    }

    @Override
    public void sendRealTimeNotificationToUsers(List<String> userIds, Map<String, Object> payload) {
        userIds.forEach(userId -> sendRealTimeNotification(userId, payload));
    }

    @Override
    public void broadcastRealTimeNotification(Map<String, Object> payload) {
        if (!webSocketEnabled) {
            log.warn("WebSocket is disabled, cannot broadcast real-time notification");
            return;
        }

        try {
            messagingTemplate.convertAndSend("/topic/notifications", payload);
            log.info("📡 Real-time notification broadcasted to all connected users");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast real-time notification", e);
        }
    }

    @Override
    public void registerDeviceToken(String userId, String deviceToken, String platform) {
        log.info("📱 Registering device token for user: {} on platform: {}", userId, platform);
        
        userDeviceTokens.computeIfAbsent(userId, k -> new ArrayList<>()).add(deviceToken);
        
        // Remove duplicates
        List<String> tokens = userDeviceTokens.get(userId);
        userDeviceTokens.put(userId, tokens.stream().distinct().collect(Collectors.toList()));
        
        log.info("✅ Device token registered successfully for user: {}", userId);
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
        log.info("🚨 Sending emergency alert to all users");
        
        PushNotificationRequest emergencyRequest = PushNotificationRequest.builder()
                .title("🚨 " + title)
                .body(message)
                .priority(Notification.Priority.URGENT)
                .sound("emergency")
                .enableSound(true)
                .enableVibration(true)
                .enableLights(true)
                .color("#FF0000")
                .data(data)
                .build();
        
        return sendToAllUsers(emergencyRequest);
    }

    @Override
    public PushNotificationResponse sendGradeNotification(String studentId, String courseName, String grade) {
        Map<String, Object> variables = Map.of(
                "courseName", courseName,
                "grade", grade
        );
        return sendTemplatedPushNotification("grade-notification-push", studentId, variables);
    }

    @Override
    public PushNotificationResponse sendAssignmentReminder(String studentId, String assignmentTitle, LocalDateTime dueDate) {
        Map<String, Object> variables = Map.of(
                "assignmentTitle", assignmentTitle,
                "dueDate", dueDate.toString()
        );
        return sendTemplatedPushNotification("assignment-reminder-push", studentId, variables);
    }

    @Override
    public PushNotificationResponse sendAttendanceAlert(String parentId, String studentName, String alertType) {
        Map<String, Object> variables = Map.of(
                "studentName", studentName,
                "alertType", alertType
        );
        return sendTemplatedPushNotification("attendance-alert-push", parentId, variables);
    }

    @Override
    public List<PushNotificationResponse> sendAnnouncementNotification(String title, String content, List<String> targetUserIds) {
        BulkPushNotificationRequest bulkRequest = BulkPushNotificationRequest.builder()
                .recipients(targetUserIds.stream()
                        .map(userId -> BulkPushNotificationRequest.BulkPushRecipient.builder()
                                .recipientId(userId)
                                .build())
                        .collect(Collectors.toList()))
                .title("📢 " + title)
                .body(content)
                .icon("announcement")
                .category("announcement")
                .build();
        
        return sendBulkPushNotifications(bulkRequest);
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

        // WebSocket specific metrics
        analytics.put("activeWebSocketConnections", getActiveConnectionsCount());
        analytics.put("connectedUsers", getConnectedUsers().size());

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
        // In a real implementation, this would query FCM or other providers
        return "delivered";
    }

    @Override
    @Transactional
    public PushNotificationResponse retryFailedNotification(Long notificationId) {
        log.info("🔄 Retrying failed push notification: {}", notificationId);

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            return PushNotificationResponse.failure("", "Notification not found", "NOT_FOUND");
        }

        Notification notification = notificationOpt.get();
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            return PushNotificationResponse.failure("", "Maximum retry attempts exceeded", "MAX_RETRIES");
        }

        // Create push request from notification
        PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                .recipientId("") // Would need to extract from notification metadata
                .title(notification.getTitle())
                .body(notification.getContent())
                .build();

        // Increment retry count
        notification.setRetryCount(notification.getRetryCount() + 1);
        notificationRepository.save(notification);

        return sendPushNotification(pushRequest);
    }

    @Override
    public int getActiveConnectionsCount() {
        return activeWebSocketSessions.size();
    }

    @Override
    public List<String> getConnectedUsers() {
        return new ArrayList<>(activeWebSocketSessions.keySet());
    }

    @Override
    public void disconnectUser(String userId) {
        activeWebSocketSessions.remove(userId);
        log.info("🔌 User disconnected: {}", userId);
    }

    // Private helper methods

    private List<PushNotificationResponse> processPushBatch(List<BulkPushNotificationRequest.BulkPushRecipient> batch, 
                                                           BulkPushNotificationRequest bulkRequest) {
        if (asyncEnabled) {
            // Process batch asynchronously
            List<CompletableFuture<PushNotificationResponse>> futures = batch.stream()
                    .map(recipient -> CompletableFuture.supplyAsync(() -> processPushRecipient(recipient, bulkRequest)))
                    .collect(Collectors.toList());

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } else {
            // Process batch synchronously
            return batch.stream()
                    .map(recipient -> processPushRecipient(recipient, bulkRequest))
                    .collect(Collectors.toList());
        }
    }

    private PushNotificationResponse processPushRecipient(BulkPushNotificationRequest.BulkPushRecipient recipient, 
                                                         BulkPushNotificationRequest bulkRequest) {
        try {
            PushNotificationRequest pushRequest = PushNotificationRequest.builder()
                    .recipientId(recipient.getRecipientId())
                    .title(recipient.getCustomTitle() != null ? recipient.getCustomTitle() : bulkRequest.getTitle())
                    .body(recipient.getCustomBody() != null ? recipient.getCustomBody() : bulkRequest.getBody())
                    .icon(recipient.getCustomIcon() != null ? recipient.getCustomIcon() : bulkRequest.getIcon())
                    .image(recipient.getCustomImage() != null ? recipient.getCustomImage() : bulkRequest.getImage())
                    .sound(recipient.getCustomSound() != null ? recipient.getCustomSound() : bulkRequest.getSound())
                    .clickAction(recipient.getCustomClickAction() != null ? recipient.getCustomClickAction() : bulkRequest.getClickAction())
                    .priority(recipient.getCustomPriority() != null ? recipient.getCustomPriority() : bulkRequest.getPriority())
                    .data(mergeData(bulkRequest.getGlobalData(), recipient.getPersonalizedData()))
                    .templateName(bulkRequest.getTemplateName())
                    .templateVariables(mergeVariables(bulkRequest.getGlobalTemplateVariables(), recipient.getPersonalizedVariables()))
                    .scheduledAt(recipient.getCustomScheduledAt() != null ? recipient.getCustomScheduledAt() : bulkRequest.getScheduledAt())
                    .build();

            if (bulkRequest.getTemplateName() != null) {
                return sendTemplatedPushNotification(bulkRequest.getTemplateName(), recipient.getRecipientId(), 
                        pushRequest.getTemplateVariables());
            } else {
                return sendPushNotification(pushRequest);
            }
        } catch (Exception e) {
            log.error("❌ Failed to process push notification recipient: {}", recipient.getRecipientId(), e);
            return PushNotificationResponse.failure(recipient.getRecipientId(), e.getMessage(), "PROCESSING_ERROR");
        }
    }

    private Map<String, Object> mergeData(Map<String, Object> global, Map<String, Object> personal) {
        Map<String, Object> merged = new HashMap<>();
        if (global != null) merged.putAll(global);
        if (personal != null) merged.putAll(personal);
        return merged;
    }

    private Map<String, Object> mergeVariables(Map<String, Object> global, Map<String, Object> personal) {
        Map<String, Object> merged = new HashMap<>();
        if (global != null) merged.putAll(global);
        if (personal != null) merged.putAll(personal);
        return merged;
    }

    private Notification createNotificationRecord(PushNotificationRequest pushRequest) {
        return Notification.builder()
                .recipientType(Notification.RecipientType.STUDENT) // Default, should be determined from context
                .notificationType(Notification.NotificationType.CUSTOM)
                .channel(Notification.NotificationChannel.PUSH_NOTIFICATION)
                .title(pushRequest.getTitle())
                .content(pushRequest.getBody())
                .status(Notification.NotificationStatus.PENDING)
                .priority(pushRequest.getPriority())
                .scheduledAt(pushRequest.getScheduledAt())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    private PushNotificationResponse sendViaWebSocket(PushNotificationRequest pushRequest, Long notificationId) {
        try {
            Map<String, Object> payload = Map.of(
                    "id", notificationId,
                    "title", pushRequest.getTitle(),
                    "body", pushRequest.getBody(),
                    "icon", pushRequest.getIcon() != null ? pushRequest.getIcon() : "default",
                    "timestamp", LocalDateTime.now().toString(),
                    "data", pushRequest.getData() != null ? pushRequest.getData() : Map.of()
            );

            sendRealTimeNotification(pushRequest.getRecipientId(), payload);
            
            return PushNotificationResponse.webSocketDelivered(pushRequest.getRecipientId(), 
                    activeWebSocketSessions.get(pushRequest.getRecipientId()));
        } catch (Exception e) {
            log.error("❌ Failed to send via WebSocket to: {}", pushRequest.getRecipientId(), e);
            return PushNotificationResponse.failure(pushRequest.getRecipientId(), e.getMessage(), "WEBSOCKET_ERROR");
        }
    }

    private PushNotificationResponse sendViaFCM(PushNotificationRequest pushRequest, List<String> deviceTokens, Long notificationId) {
        // In a real implementation, this would use the FCM SDK
        log.info("📱 Simulating FCM push notification send to {} devices", deviceTokens.size());
        
        try {
            // Simulate API call delay
            Thread.sleep(100);
            
            String messageId = "fcm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            
            PushNotificationResponse response = PushNotificationResponse.success(notificationId, messageId, pushRequest.getRecipientId());
            response.setTitle(pushRequest.getTitle());
            response.setBody(pushRequest.getBody());
            response.setProvider("FCM");
            response.setSuccessCount(deviceTokens.size());
            response.setFailureCount(0);
            
            return response;
        } catch (Exception e) {
            return PushNotificationResponse.failure(pushRequest.getRecipientId(), e.getMessage(), "FCM_ERROR");
        }
    }

    private void updateNotificationStatus(Notification notification, Notification.NotificationStatus status) {
        notification.setStatus(status);
        if (status == Notification.NotificationStatus.SENT || status == Notification.NotificationStatus.DELIVERED) {
            notification.setSentAt(LocalDateTime.now());
        }
        notificationRepository.save(notification);
    }

    private void createCommunicationLog(Notification notification, PushNotificationResponse response) {
        CommunicationLog log = CommunicationLog.builder()
                .notificationId(notification.getId())
                .channel(Notification.NotificationChannel.PUSH_NOTIFICATION)
                .status(response.getSuccess() ? CommunicationLog.LogStatus.SENT : CommunicationLog.LogStatus.FAILED)
                .sentAt(LocalDateTime.now())
                .externalMessageId(response.getMessageId())
                .provider(response.getProvider())
                .errorMessage(response.getErrorMessage())
                .retryCount(0)
                .build();

        communicationLogRepository.save(log);
    }

    private boolean isUserConnected(String userId) {
        return activeWebSocketSessions.containsKey(userId);
    }

    private List<String> getUserIdsByRole(String role) {
        // Placeholder - in real implementation, query user repository
        return List.of("user1", "user2", "user3");
    }

    private List<String> getUserIdsByClass(Long classId) {
        // Placeholder - in real implementation, query enrollment repository
        return List.of("student1", "student2", "teacher1");
    }

    private List<String> getAllActiveUserIds() {
        // Placeholder - in real implementation, query user repository
        return List.of("user1", "user2", "user3", "user4", "user5");
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
