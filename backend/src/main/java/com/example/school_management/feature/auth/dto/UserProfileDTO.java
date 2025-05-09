package com.example.school_management.feature.auth.dto;

import java.time.LocalDateTime;

import com.example.school_management.feature.auth.entity.UserRole;

public record UserProfileDTO(
    String firstName,
    String lastName,
    String email,
    String telephone,
    LocalDateTime birthday,
    String gender,
    String address,
    UserRole role
) {}