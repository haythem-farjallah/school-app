package com.example.school_management.commons.dtos;
import com.example.school_management.feature.auth.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserDto user;
}
