package com.example.school_management.feature.auth.dto;


import com.example.school_management.feature.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private String profileTheme;
    private String profileLanguage;
    private Set<String> permissions;
}