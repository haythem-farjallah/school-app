package com.example.school_management.feature.auth.controller;


import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.LoginRequest;
import com.example.school_management.commons.dtos.LoginResponse;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                new ApiSuccessResponse<>("success", authService.login(request))
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", null));
    }
}
