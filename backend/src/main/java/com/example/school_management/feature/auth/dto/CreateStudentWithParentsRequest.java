package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateStudentWithParentsRequest(
        @Valid @NotNull StudentDtoCreate student,
        @Valid @Size(max = 2) List<ParentCreateDto> parents
) {}