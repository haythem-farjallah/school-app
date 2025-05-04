package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    String email;
    @NotBlank
    String otp;
    @NotBlank
    String newPassword;
}
