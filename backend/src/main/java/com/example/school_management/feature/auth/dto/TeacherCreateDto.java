package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO used by the admin API to create a {@code Teacher}.
 *
 *  • All “person” fields live in the nested {@link BaseUserCreateDto}.
 *  • Teacher-specific fields are added beside it.
 *  • Record = immutable → safe to expose in controller layer.
 */
public record TeacherCreateDto(

        @NotNull @Valid
        BaseUserCreateDto  profile,          // firstName, lastName, email, …

        @NotBlank
        String qualifications,              // e.g. “M.Ed TESOL”

        @NotBlank
        String subjectsTaught,              // comma-separated or use a List<> if you prefer

        @Positive
        Integer availableHours,             // weekly availability

        String schedulePreferences          // free-text (optional)
) implements BaseUserDtoMarker { }
