package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParentUpdateDto implements BaseUserDtoMarker {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String telephone;
    private String preferredContactMethod;
} 