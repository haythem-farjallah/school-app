package com.example.school_management.feature.operational.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CreateResourceCommentRequest {
    @NotBlank
    String content;
    
    @NotNull
    Long resourceId;
} 