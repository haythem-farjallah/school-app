package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "admin_feeds")
@EntityListeners(AuditingEntityListener.class)
public class AdminFeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", columnDefinition = "audit_event_type")
    @ColumnTransformer(write = "?::audit_event_type")
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", columnDefinition = "notification_type")
    @ColumnTransformer(write = "?::notification_type")
    private NotificationType notificationType;

    private String title;
    
    @Lob
    private String description;
    
    private String entityType; // e.g., "User", "Course", "Class", etc.
    private Long entityId; // ID of the related entity
    private String entityName; // Name/title of the related entity
    
    private String severity = "INFO"; // INFO, WARNING, ERROR, CRITICAL
    
    private boolean isRead = false;
    
    @ManyToOne
    @JoinColumn(name = "triggered_by_id")
    private BaseUser triggeredBy;
    
    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private BaseUser targetUser; // User affected by this event
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Additional metadata as JSON
    @Lob
    private String metadata; // JSON string for additional data
    
    // Helper methods
    public void markAsRead() {
        this.isRead = true;
    }
    
    public boolean isHighPriority() {
        return "ERROR".equals(severity) || "CRITICAL".equals(severity);
    }
    
    public boolean isRecent() {
        return createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }
} 