package com.example.school_management.commons.dtos;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}