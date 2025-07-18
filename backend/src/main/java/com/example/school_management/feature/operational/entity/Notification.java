package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user receiving the notification
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BaseUser user;

    private String title;
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "notification_type")
    @ColumnTransformer(write = "?::notification_type")
    private NotificationType type;

    // Link to related entity (optional)
    private String entityType; // e.g., "GRADE", "ANNOUNCEMENT", "RESOURCE"
    private Long entityId;
    private String actionUrl; // URL for the notification action

    private Boolean readStatus = false;
    private Boolean isRead = false;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime readAt;

    // Helper methods
    public void markAsRead() {
        this.readStatus = true;
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean isUnread() {
        return !this.isRead;
    }
}