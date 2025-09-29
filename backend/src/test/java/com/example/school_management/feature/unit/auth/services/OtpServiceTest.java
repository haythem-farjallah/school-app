package com.example.school_management.feature.unit.auth.services;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {
    
    @Mock 
    private UserRepository userRepo;
    
    @Mock 
    private EmailService emailService;
    
    @InjectMocks 
    private OtpService otpService;

    private Student user;

    @BeforeEach
    void setUp() {
        user = new Student();
        user.setEmail("foo@bar.com");
    }

    @Test
    void generateAndSendOtp_savesOtpAndSendsEmail() {
        // when
        otpService.generateAndSendOtp(user);

        // then - OTP code and expiry should be set
        assertNotNull(user.getOtpCode());
        assertEquals(6, user.getOtpCode().length());
        assertTrue(user.getOtpExpiry().isAfter(LocalDateTime.now()));

        verify(userRepo).save(user);
        verify(emailService).sendTemplateEmail(
                eq("foo@bar.com"),
                eq("Your password-reset code"),
                eq("otp"),
                argThat(map -> map.get("otp").equals(user.getOtpCode()))
        );
    }

    @Test
    void validateOtp_withValidCode_clearsOtp() {
        // given
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        // when
        otpService.validateOtp(user, "123456");

        // then
        assertNull(user.getOtpCode());
        assertNull(user.getOtpExpiry());
        verify(userRepo).save(user);
    }

    @Test
    void validateOtp_withInvalidCode_throws() {
        // given
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        // when/then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                otpService.validateOtp(user, "000000")
        );
        assertEquals("Invalid or expired OTP", ex.getReason());
    }

    @Test
    void validateOtp_withExpiredCode_throws() {
        // given
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

        // when/then
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                otpService.validateOtp(user, "123456")
        );
        assertEquals("Invalid or expired OTP", ex.getReason());
    }

    @Test
    void validateOtp_withNullOtp_throws() {
        // given
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        // when/then
        assertThrows(ResponseStatusException.class, () ->
                otpService.validateOtp(user, "123456")
        );
    }
}