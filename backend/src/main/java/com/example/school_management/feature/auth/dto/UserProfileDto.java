package com.example.school_management.feature.auth.dto;

import com.example.school_management.feature.auth.entity.UserRole;

import java.util.Set;

/**
 * The signed-in user's own profile (GET/PATCH /api/me/profile).
 * Carries private contact fields, so it is returned only to the profile owner.
 */
public record UserProfileDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        String telephone,
        String address,
        String profileTheme,
        String profileLanguage,
        Set<String> permissions
) {
}
