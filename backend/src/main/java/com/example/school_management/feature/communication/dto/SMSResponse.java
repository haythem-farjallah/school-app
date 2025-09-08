package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSResponse {

    private Long notificationId;

    private String messageId;

    private String recipientPhone;

    private String message;

    private Notification.NotificationStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime scheduledAt;

    private String errorMessage;

    private String errorCode;

    private Integer retryCount;

    private String externalId; // Provider's message ID

    private String provider;

    private Double cost;

    private String currency;

    @Builder.Default
    private Boolean success = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // SMS-specific fields
    private Integer messageParts;

    private String encoding;

    private String direction; // "outbound", "inbound"

    private String smsType;

    // Delivery information
    private String deliveryStatus;

    private LocalDateTime deliveredAt;

    private String deliveryMessage;

    private String carrierName;

    private String countryCode;

    private String region;

    // Analytics fields
    private Boolean delivered;

    private Boolean failed;

    private Boolean undelivered;

    private LocalDateTime failedAt;

    private LocalDateTime undeliveredAt;

    // Provider-specific data
    private String providerResponse;

    private String accountSid; // For Twilio

    private String messagingServiceSid; // For Twilio

    // Callback information
    private String callbackUrl;

    private LocalDateTime lastCallbackAt;

    private String callbackStatus;

    // Static factory methods for common responses
    public static SMSResponse success(Long notificationId, String messageId, String recipientPhone) {
        return SMSResponse.builder()
                .notificationId(notificationId)
                .messageId(messageId)
                .recipientPhone(recipientPhone)
                .status(Notification.NotificationStatus.SENT)
                .success(true)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public static SMSResponse failure(String recipientPhone, String errorMessage, String errorCode) {
        return SMSResponse.builder()
                .recipientPhone(recipientPhone)
                .status(Notification.NotificationStatus.FAILED)
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static SMSResponse scheduled(Long notificationId, String recipientPhone, LocalDateTime scheduledAt) {
        return SMSResponse.builder()
                .notificationId(notificationId)
                .recipientPhone(recipientPhone)
                .status(Notification.NotificationStatus.PENDING)
                .success(true)
                .scheduledAt(scheduledAt)
                .build();
    }

    public static SMSResponse delivered(String messageId, String recipientPhone, LocalDateTime deliveredAt) {
        return SMSResponse.builder()
                .messageId(messageId)
                .recipientPhone(recipientPhone)
                .status(Notification.NotificationStatus.DELIVERED)
                .success(true)
                .delivered(true)
                .deliveredAt(deliveredAt)
                .deliveryStatus("delivered")
                .build();
    }

    // Helper methods
    public boolean isDelivered() {
        return Boolean.TRUE.equals(delivered) || 
               Notification.NotificationStatus.DELIVERED.equals(status);
    }

    public boolean isFailed() {
        return Boolean.TRUE.equals(failed) || 
               Notification.NotificationStatus.FAILED.equals(status);
    }

    public boolean isPending() {
        return Notification.NotificationStatus.PENDING.equals(status);
    }

    public boolean isSent() {
        return Notification.NotificationStatus.SENT.equals(status);
    }

    public Double getCostInUSD() {
        if (cost == null) return null;
        if ("USD".equals(currency)) return cost;
        // In a real implementation, you'd convert currencies
        return cost; // Placeholder
    }
}
