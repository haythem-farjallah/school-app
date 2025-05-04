package com.example.school_management.feature.auth.dto;


import com.example.school_management.feature.auth.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private String profileTheme;
    private String profileLanguage;
}