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
        Set<Long> publisherIds
) { } 