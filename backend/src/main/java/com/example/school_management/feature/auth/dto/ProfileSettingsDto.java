package com.example.school_management.feature.auth.dto;


public record ProfileSettingsDto(
        String   language,
        String   theme,
        Boolean  notificationsEnabled,
        Boolean  darkMode
) {}