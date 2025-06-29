package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record StudentDtoCreate (
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email    @NotBlank String email,
    @NotBlank String telephone,
    @NotNull  LocalDateTime birthday,
    String    gender,
    String    address,
    @NotBlank String gradeLevel,
    @Min(1900) @Max(2100) Integer enrollmentYear
) implements BaseUserDtoMarker {}