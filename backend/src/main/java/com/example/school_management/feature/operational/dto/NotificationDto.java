package com.example.school_management.feature.operational.dto;

import com.example.school_management.commons.json.JsonResource;
import com.example.school_management.feature.operational.entity.enums.NotificationType;

import java.time.LocalDateTime;

@JsonResource("notification")
public record NotificationDto(
        Long id,
        String title,
        String message,
        NotificationType type,
        String entityType,
        Long entityId,
        String actionUrl,
        Boolean readStatus,
        LocalDateTime createdAt,
        LocalDateTime readAt
) { } 