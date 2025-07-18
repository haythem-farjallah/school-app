package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class AdminFeedDto {
    Long id;
    AuditEventType eventType;
    NotificationType notificationType;
    String title;
    String description;
    String entityType;
    Long entityId;
    String entityName;
    String severity;
    boolean isRead;
    Long triggeredById;
    String triggeredByName;
    Long targetUserId;
    String targetUserName;
    LocalDateTime createdAt;
    String metadata;
} 