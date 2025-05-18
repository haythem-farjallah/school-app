package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;

public record WorkerUpdateDto(
        @Valid BaseUserCreateDto profile
) implements BaseUserDtoMarker { }