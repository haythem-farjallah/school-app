package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushNotificationResponse {

    private Long notificationId;

    private String messageId;

    private String recipientId;

    private String title;

    private String body;

    private Notification.NotificationStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime scheduledAt;

    private String errorMessage;

    private String errorCode;

    private Integer retryCount;

    private String externalId; // FCM message ID

    private String provider;

    @Builder.Default
    private Boolean success = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Push notification specific fields
    private String platform; // "android", "ios", "web"

    private String deviceToken;

    private Integer multicastId; // For FCM multicast

    private Integer successCount;

    private Integer failureCount;

    private List<String> canonicalIds; // FCM canonical registration tokens

    private List<String> invalidTokens; // Tokens that should be removed

    // Delivery information
    private String deliveryStatus;

    private LocalDateTime deliveredAt;

    private LocalDateTime openedAt;

    private LocalDateTime clickedAt;

    // Analytics fields
    private Boolean delivered;

    private Boolean opened;

    private Boolean clicked;

    private Boolean dismissed;

    private String clickAction;

    private Map<String, Object> customData;

    // FCM specific response data
    private FcmResponse fcmResponse;

    // WebSocket delivery info
    private WebSocketDelivery webSocketDelivery;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FcmResponse {
        private String messageId;

        private String error;

        private String canonicalRegistrationId;

        private Integer multicastId;

        private Integer success;

        private Integer failure;

        private List<FcmResult> results;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class FcmResult {
            private String messageId;

            private String registrationId;

            private String error;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebSocketDelivery {
        private String sessionId;

        private Boolean connected;

        private LocalDateTime deliveredAt;

        private String deliveryStatus;

        private String errorMessage;
    }

    // Static factory methods for common responses
    public static PushNotificationResponse success(Long notificationId, String messageId, String recipientId) {
        return PushNotificationResponse.builder()
                .notificationId(notificationId)
                .messageId(messageId)
                .recipientId(recipientId)
                .status(Notification.NotificationStatus.SENT)
                .success(true)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public static PushNotificationResponse failure(String recipientId, String errorMessage, String errorCode) {
        return PushNotificationResponse.builder()
                .recipientId(recipientId)
                .status(Notification.NotificationStatus.FAILED)
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static PushNotificationResponse scheduled(Long notificationId, String recipientId, LocalDateTime scheduledAt) {
        return PushNotificationResponse.builder()
                .notificationId(notificationId)
                .recipientId(recipientId)
                .status(Notification.NotificationStatus.PENDING)
                .success(true)
                .scheduledAt(scheduledAt)
                .build();
    }

    public static PushNotificationResponse delivered(String messageId, String recipientId, LocalDateTime deliveredAt) {
        return PushNotificationResponse.builder()
                .messageId(messageId)
                .recipientId(recipientId)
                .status(Notification.NotificationStatus.DELIVERED)
                .success(true)
                .delivered(true)
                .deliveredAt(deliveredAt)
                .deliveryStatus("delivered")
                .build();
    }

    public static PushNotificationResponse webSocketDelivered(String recipientId, String sessionId) {
        return PushNotificationResponse.builder()
                .recipientId(recipientId)
                .status(Notification.NotificationStatus.DELIVERED)
                .success(true)
                .delivered(true)
                .deliveredAt(LocalDateTime.now())
                .webSocketDelivery(WebSocketDelivery.builder()
                        .sessionId(sessionId)
                        .connected(true)
                        .deliveredAt(LocalDateTime.now())
                        .deliveryStatus("delivered")
                        .build())
                .build();
    }

    // Helper methods
    public boolean isDelivered() {
        return Boolean.TRUE.equals(delivered) || 
               Notification.NotificationStatus.DELIVERED.equals(status);
    }

    public boolean isFailed() {
        return !success || Notification.NotificationStatus.FAILED.equals(status);
    }

    public boolean isPending() {
        return Notification.NotificationStatus.PENDING.equals(status);
    }

    public boolean isSent() {
        return Notification.NotificationStatus.SENT.equals(status);
    }

    public boolean isOpened() {
        return Boolean.TRUE.equals(opened) || openedAt != null;
    }

    public boolean isClicked() {
        return Boolean.TRUE.equals(clicked) || clickedAt != null;
    }

    public boolean wasDeliveredViaWebSocket() {
        return webSocketDelivery != null && Boolean.TRUE.equals(webSocketDelivery.getConnected());
    }

    public boolean wasDeliveredViaFCM() {
        return fcmResponse != null && fcmResponse.getMessageId() != null;
    }

    public boolean hasInvalidTokens() {
        return invalidTokens != null && !invalidTokens.isEmpty();
    }

    public boolean hasCanonicalIds() {
        return canonicalIds != null && !canonicalIds.isEmpty();
    }
}
