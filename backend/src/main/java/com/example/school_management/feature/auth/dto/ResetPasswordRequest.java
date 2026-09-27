package com.example.school_management.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    @Email
    String email;
    @NotBlank
    @Pattern(regexp = "[0-9]{6}", message = "must be 6 digits")
    String otp;
    @NotBlank
    @Size(min = 8, max = 128)
    String newPassword;
}
