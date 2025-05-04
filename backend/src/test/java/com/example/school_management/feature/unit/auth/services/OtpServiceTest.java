package com.example.school_management.feature.unit.auth.services;
import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {
    @Mock private BaseUserRepository userRepo;
    @Mock private EmailService emailService;
    @InjectMocks private OtpService otpService;

    private Student user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new Student();
        user.setEmail("foo@bar.com");
    }

    @Test
    void generateAndSendOtp_savesOtpAndSendsEmail() {
        otpService.generateAndSendOtp(user);

        // OTP code and expiry should be set
        assertNotNull(user.getOtpCode());
        assertTrue(user.getOtpExpiry().isAfter(LocalDateTime.now()));

        verify(userRepo).save(user);
        verify(emailService).sendTemplateEmail(
                eq("foo@bar.com"),
                eq("Your password-reset code"),
                eq("otp-email"),
                argThat(map -> map.get("otp").equals(user.getOtpCode()))
        );
    }

    @Test
    void validateOtp_withValidCode_clearsOtp() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        otpService.validateOtp(user, "123456");

        assertNull(user.getOtpCode());
        assertNull(user.getOtpExpiry());
        verify(userRepo).save(user);
    }

    @Test
    void validateOtp_withInvalidCode_throws() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                otpService.validateOtp(user, "000000")
        );
        assertEquals("Invalid or expired OTP", ex.getReason());
    }

    @Test
    void validateOtp_withExpiredCode_throws() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                otpService.validateOtp(user, "123456")
        );
        assertEquals("Invalid or expired OTP", ex.getReason());
    }
}
