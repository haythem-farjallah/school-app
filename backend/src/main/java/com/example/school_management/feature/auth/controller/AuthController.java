package com.example.school_management.feature.auth.controller;


import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.LoginRequest;
import com.example.school_management.commons.dtos.LoginResponse;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.dto.ChangePasswordRequest;
import com.example.school_management.feature.auth.dto.ForgotPasswordRequest;
import com.example.school_management.feature.auth.dto.RefreshTokenRequest;
import com.example.school_management.feature.auth.dto.RefreshTokenResponse;
import com.example.school_management.feature.auth.dto.ResetPasswordRequest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.AuthService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.example.school_management.feature.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final PasswordResetService passwordResetService;
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

    /**
     * Issues a new access token. The refresh token in the body is the credential, so no
     * access token (expired or not) is required.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiSuccessResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
                new ApiSuccessResponse<>("success", authService.refresh(request.refreshToken()))
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
     * Step 1 of “forgot-password” flow: email a reset code. Always 200, so the response never
     * tells whether the email belongs to an account.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest rq) {
        passwordResetService.requestReset(rq.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * Step 2 of “forgot-password”: validate the code, reset the password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest rq) {
        passwordResetService.resetPassword(rq.getEmail(), rq.getOtp(), rq.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * “First-login” change-password endpoint (old→new). Changes only the caller's own password;
     * the email in the body must be the authenticated account's.
     */
    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails principal,
                                               @Valid @RequestBody ChangePasswordRequest rq) {
        if (!principal.getUsername().equals(rq.getEmail())) {
            throw new AccessDeniedException("You can only change your own password");
        }

        BaseUser u = userDetailsService.findBaseUserByEmail(principal.getUsername());
        if (!passwordEncoder.matches(rq.getOldPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid current password");
        }

        u.setPassword(passwordEncoder.encode(rq.getNewPassword()));
        u.setPasswordChangeRequired(false);
        userRepo.save(u);

        log.info("First-login password changed for {}", principal.getUsername());
        return ResponseEntity.ok().build();

    }
}
