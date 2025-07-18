package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.AnnouncementImportance;

import java.time.LocalDateTime;
import java.util.Set;

public record UpdateAnnouncementRequest(
        String title,
        String body,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean isPublic,
        AnnouncementImportance importance,
        Set<Long> publisherIds
) { } 