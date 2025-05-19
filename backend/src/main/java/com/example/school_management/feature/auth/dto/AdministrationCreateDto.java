package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;


public record AdministrationCreateDto(
        @Valid BaseUserCreateDto profile,

         String department,
         String jobTitle,

        String password
) implements BaseUserDtoMarker { }