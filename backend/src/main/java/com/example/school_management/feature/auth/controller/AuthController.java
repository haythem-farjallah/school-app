package com.example.school_management.feature.auth.controller;


import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.LoginRequest;
import com.example.school_management.commons.dtos.LoginResponse;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.dto.ChangePasswordRequest;
import com.example.school_management.feature.auth.dto.ForgotPasswordRequest;
import com.example.school_management.feature.auth.dto.ResetPasswordRequest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.AuthService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.example.school_management.feature.auth.service.OtpService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
@SecurityRequirements({})
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final UserRepository userRepo;
    private final CustomUserDetailsService userDetailsService;


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

    /**
     * Step 1 of “forgot-password” flow:  generate & email OTP.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest rq) {
        BaseUser u = userDetailsService.findBaseUserByEmail(rq.getEmail());
        otpService.generateAndSendOtp(u);
        log.info("Forgot-password OTP sent to {}", rq.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * Step 2 of “forgot-password”: validate OTP, reset password.
     */
    @PostMapping("/reset-password")
    public  ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest rq) {
        BaseUser u = userDetailsService.findBaseUserByEmail(rq.getEmail());
        otpService.validateOtp(u, rq.getOtp());

        u.setPassword(passwordEncoder.encode(rq.getNewPassword()));
        u.setPasswordChangeRequired(false);
        userRepo.save(u);

        log.info("Password reset for {}", rq.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * “First-login” change-password endpoint (old→new).
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest rq) {
        BaseUser u = userDetailsService.findBaseUserByEmail(rq.getEmail());
        if (!passwordEncoder.matches(rq.getOldPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid current password");
        }

        u.setPassword(passwordEncoder.encode(rq.getNewPassword()));
        u.setPasswordChangeRequired(false);
        userRepo.save(u);

        log.info("First-login password changed for {}", rq.getEmail());
        return ResponseEntity.ok().build();

    }

    /**
     * DEBUG endpoint to test password verification
     */
    @PostMapping("/debug/test-password")
    public ResponseEntity<Map<String, Object>> debugTestPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        Map<String, Object> result = new HashMap<>();
        try {
            BaseUser user = userDetailsService.findBaseUserByEmail(email);
            
            // Get password directly from database to avoid @JsonIgnore issues
            String storedPassword = userRepo.findByEmailIgnoreCase(email)
                    .map(BaseUser::getPassword)
                    .orElse(null);
            
            boolean passwordMatches = storedPassword != null && passwordEncoder.matches(password, storedPassword);
            
            result.put("userFound", true);
            result.put("email", user.getEmail());
            result.put("role", user.getRole());
            result.put("status", user.getStatus());
            result.put("passwordChangeRequired", user.isPasswordChangeRequired());
            result.put("passwordMatches", passwordMatches);
            
            if (storedPassword != null) {
                result.put("passwordLength", storedPassword.length());
                result.put("passwordPrefix", storedPassword.substring(0, 10));
            } else {
                result.put("passwordLength", 0);
                result.put("passwordPrefix", "NULL");
            }
            result.put("inputPassword", password);
            
            // Test with common passwords
            Map<String, Boolean> commonPasswordTests = new HashMap<>();
            if (storedPassword != null) {
                commonPasswordTests.put("H7d1i8Af", passwordEncoder.matches("H7d1i8Af", storedPassword));
                commonPasswordTests.put("ChangeMe123", passwordEncoder.matches("ChangeMe123", storedPassword));
                commonPasswordTests.put("password", passwordEncoder.matches("password", storedPassword));
                commonPasswordTests.put("123456", passwordEncoder.matches("123456", storedPassword));
            }
            result.put("commonPasswordTests", commonPasswordTests);
            
        } catch (Exception e) {
            result.put("userFound", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * DEBUG endpoint to reset password to a known value
     */
    @PostMapping("/debug/reset-password")
    public ResponseEntity<Map<String, Object>> debugResetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        
        Map<String, Object> result = new HashMap<>();
        try {
            BaseUser user = userDetailsService.findBaseUserByEmail(email);
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            user.setPasswordChangeRequired(false);
            
            // Save the user
            userRepo.save(user);
            
            result.put("success", true);
            result.put("email", user.getEmail());
            result.put("newPassword", newPassword);
            result.put("message", "Password reset successfully");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

}
