package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class ParentUpdateDto implements BaseUserDtoMarker {
    private String telephone;
    private String address;
    private String preferredContactMethod;
    private String relation;
    private List<String> children; // List of student emails to assign
} 