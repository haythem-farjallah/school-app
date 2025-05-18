package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.Positive;

public record TeacherUpdateDto(

        /* ---------- Base-user changes ---------- */
        String telephone,
        String address,
        String image,

        /* ---------- Teacher-specific ---------- */
        String qualifications,
        String subjectsTaught,

        @Positive
        Integer availableHours,

        String schedulePreferences
) implements BaseUserDtoMarker { }