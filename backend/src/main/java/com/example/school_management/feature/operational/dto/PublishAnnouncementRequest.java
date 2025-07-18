package com.example.school_management.feature.operational.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record PublishAnnouncementRequest(
        @NotEmpty Set<Long> userIds
) { } 