package com.example.school_management.feature.communication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CommunicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private Notification.NotificationChannel channel;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LogStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(name = "provider")
    private String provider; // e.g., "Twilio", "SendGrid", "FCM"

    @Column(name = "cost")
    private Double cost;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional tracking data

    public enum LogStatus {
        QUEUED,
        SENT,
        DELIVERED,
        OPENED,
        CLICKED,
        BOUNCED,
        FAILED,
        UNSUBSCRIBED
    }
}
