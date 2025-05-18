package com.example.school_management.feature.auth.dto;

public record AdministrationDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String telephone,
        String department,
        String jobTitle
) { }