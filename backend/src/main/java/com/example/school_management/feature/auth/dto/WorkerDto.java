package com.example.school_management.feature.auth.dto;

import java.time.LocalDateTime;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.UserRole;

public record WorkerDto(
        Long                 id,
        String               firstName,
        String               lastName,
        String               email,
        String               telephone,
        LocalDateTime        birthday,
        String               gender,
        String               address,
        UserRole role,
        Status               status,
        LocalDateTime        createdAt,
        LocalDateTime        updatedAt
) implements BaseUserDtoMarker { }