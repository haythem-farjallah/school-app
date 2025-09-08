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
public class EmailResponse {

    private Long notificationId;

    private String messageId;

    private String recipientEmail;

    private String subject;

    private Notification.NotificationStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime scheduledAt;

    private String errorMessage;

    private Integer retryCount;

    private String externalId;

    private String provider;

    private Double estimatedCost;

    @Builder.Default
    private Boolean success = false;

    private String trackingUrl;

    private String unsubscribeUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Analytics fields
    private Boolean opened;
    
    private Boolean clicked;
    
    private Boolean bounced;
    
    private LocalDateTime openedAt;
    
    private LocalDateTime clickedAt;
    
    private LocalDateTime bouncedAt;

    // Delivery information
    private String deliveryStatus;
    
    private LocalDateTime deliveredAt;
    
    private String deliveryMessage;

    // Static factory methods for common responses
    public static EmailResponse success(Long notificationId, String messageId, String recipientEmail) {
        return EmailResponse.builder()
                .notificationId(notificationId)
                .messageId(messageId)
                .recipientEmail(recipientEmail)
                .status(Notification.NotificationStatus.SENT)
                .success(true)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public static EmailResponse failure(String recipientEmail, String errorMessage) {
        return EmailResponse.builder()
                .recipientEmail(recipientEmail)
                .status(Notification.NotificationStatus.FAILED)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static EmailResponse scheduled(Long notificationId, String recipientEmail, LocalDateTime scheduledAt) {
        return EmailResponse.builder()
                .notificationId(notificationId)
                .recipientEmail(recipientEmail)
                .status(Notification.NotificationStatus.PENDING)
                .success(true)
                .scheduledAt(scheduledAt)
                .build();
    }
}
