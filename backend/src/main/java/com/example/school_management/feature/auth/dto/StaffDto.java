package com.example.school_management.feature.auth.dto;

import com.example.school_management.feature.auth.entity.enums.StaffType;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.Set;

@Value
@Builder
public class StaffDto implements BaseUserDtoMarker {
    Long id;
    String firstName;
    String lastName;
    String email;
    String telephone;
    LocalDateTime birthday;
    String gender;
    String address;
    String image;
    StaffType staffType;
    String department;
    Set<String> permissions;
    String role;
    Boolean isEmailVerified;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
} 