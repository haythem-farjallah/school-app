package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdministrationCreateDto(
        @Valid BaseUserCreateDto profile,   // firstName, email, etc.

        @NotBlank String department,
        @NotBlank String jobTitle,

        String password
) implements BaseUserDtoMarker { }