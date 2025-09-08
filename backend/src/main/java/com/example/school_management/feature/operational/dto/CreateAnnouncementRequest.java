package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.AnnouncementImportance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record CreateAnnouncementRequest(
        @NotBlank String title,
        @NotBlank String body,
        LocalDateTime startDate,
        LocalDateTime endDate,
        @NotNull Boolean isPublic,
        @NotNull AnnouncementImportance importance,
        Set<Long> publisherIds,
        // New fields for targeting
        String targetType, // "CLASSES", "ALL_STAFF", "ALL_TEACHERS", "ALL_STUDENTS", "WHOLE_SCHOOL", "SPECIFIC_USERS"
        Set<Long> targetClassIds, // For teachers sending to their classes
        Set<Long> targetUserIds, // For specific users
        Boolean sendNotifications // Whether to send real-time notifications
) { } 