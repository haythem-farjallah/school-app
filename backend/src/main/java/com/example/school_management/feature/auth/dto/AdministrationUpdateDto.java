package com.example.school_management.feature.auth.dto;

public record AdministrationUpdateDto(
        String telephone,
        String address,
        String department,
        String jobTitle
) implements BaseUserDtoMarker { }