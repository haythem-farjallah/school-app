package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentUpdateDto implements BaseUserDtoMarker {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotNull
    private Integer enrollmentYear;
    @NotBlank
    private String gradeLevel;
}
