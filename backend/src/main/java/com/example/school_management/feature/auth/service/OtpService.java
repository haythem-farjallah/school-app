package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final BaseUserRepository userRepo;
    private final EmailService emailService;

    public void generateAndSendOtp(BaseUser u) {
        SecureRandom secureRandom = new SecureRandom();
        String code = RandomStringUtils.random(6,0,0,false,true,null,secureRandom);
        u.setOtpCode(code);
        u.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepo.save(u);
        log.info("OTP generated for {} → {}", u.getEmail(), code);
        emailService.sendTemplateEmail(
                u.getEmail(),
                "Your password-reset code",
                "otp-email",
                Map.of("otp", code)
        );
    }

    public void validateOtp(BaseUser u, String code) {
        if (!code.equals(u.getOtpCode()) ||
                u.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }
        u.setOtpCode(null);
        u.setOtpExpiry(null);
        userRepo.save(u);
    }
}
