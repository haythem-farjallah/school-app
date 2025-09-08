package com.example.school_management.feature.operational.dto;

import com.example.school_management.commons.json.JsonResource;
import com.example.school_management.feature.operational.entity.enums.AnnouncementImportance;
import java.time.LocalDateTime;
import java.util.Set;

@JsonResource("announcement")
public record AnnouncementDto(
        Long id,
        String title,
        String body,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean isPublic,
        AnnouncementImportance importance,
        LocalDateTime createdAt,
        Long createdById,
        String createdByName,
        Set<Long> publisherIds,
        String targetType,
        Set<Long> targetClassIds,
        Set<String> targetClassNames
) { } 