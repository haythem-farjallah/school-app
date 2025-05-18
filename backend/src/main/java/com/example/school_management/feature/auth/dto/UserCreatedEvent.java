package com.example.school_management.feature.auth.dto;

import com.example.school_management.feature.auth.entity.BaseUser;

public record UserCreatedEvent(
        BaseUser user,
        String rawPassword
) {}