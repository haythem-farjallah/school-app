package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * DTO used by the admin API to create a {@code Student}.
 *
 *  • All "person" fields live in the nested {@link BaseUserCreateDto}.
 *  • Student-specific fields are added beside it.
 *  • Record = immutable → safe to expose in controller layer.
 */
public record StudentCreateDto(

        @NotNull @Valid
        BaseUserCreateDto profile,          // firstName, lastName, email, …

        @NotBlank
        String gradeLevel,                  // e.g. "5th", "Grade 10"

        @NotNull
        @Min(1900) @Max(2100)
        Integer enrollmentYear              // year student enrolled

) implements BaseUserDtoMarker { }