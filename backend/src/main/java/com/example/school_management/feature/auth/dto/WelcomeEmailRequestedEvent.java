package com.example.school_management.feature.auth.dto;

public record WelcomeEmailRequestedEvent(
        Long recipientId,
        String recipientEmail,
        String userName,
        String rawPassword
) {}
