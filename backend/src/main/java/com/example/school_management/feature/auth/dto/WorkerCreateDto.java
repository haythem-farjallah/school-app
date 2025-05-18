package com.example.school_management.feature.auth.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record WorkerCreateDto(
        @Valid @NotNull BaseUserCreateDto profile
) implements BaseUserDtoMarker { }