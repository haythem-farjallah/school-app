package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ParentCreateDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email    @NotBlank String email,
        @NotBlank String telephone,
        String    preferredContactMethod
) {}
