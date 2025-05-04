package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    String email;
    @NotBlank
    String oldPassword;
    @NotBlank
    String newPassword;
}
