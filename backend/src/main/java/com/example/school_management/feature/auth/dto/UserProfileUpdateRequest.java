package com.example.school_management.feature.auth.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String telephone;
    private String address;
}
