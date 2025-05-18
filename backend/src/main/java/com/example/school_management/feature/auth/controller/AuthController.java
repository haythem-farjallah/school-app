package com.example.school_management.feature.auth.controller;


import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.LoginRequest;
import com.example.school_management.commons.dtos.LoginResponse;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.dto.ChangePasswordRequest;
import com.example.school_management.feature.auth.dto.ForgotPasswordRequest;
import com.example.school_management.feature.auth.dto.ResetPasswordRequest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
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
    private final BaseUserRepository userRepo;
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
}
