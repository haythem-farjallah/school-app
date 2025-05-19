package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final EmailService   emailService;

    /* -------------------------------------------------------- *
     *  GENERATE & MAIL                                         *
     * -------------------------------------------------------- */
    @Transactional
    public void generateAndSendOtp(BaseUser user) {
        String code = RandomStringUtils.random(6, 0, 0,
                false, true, null, SECURE_RANDOM);

        user.setOtpCode(code);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepo.save(user);

        log.info("OTP generated for {} → {}", user.getEmail(), code);

        emailService.sendTemplateEmail(
                user.getEmail(),
                "Your password-reset code",
                "otp",                 // Thymeleaf template name
                Map.of("otp", code)
        );
    }

    /* -------------------------------------------------------- *
     *  VALIDATE                                                *
     * -------------------------------------------------------- */
    @Transactional
    public void validateOtp(BaseUser user, String code) {
        boolean invalid = !code.equals(user.getOtpCode()) ||
                user.getOtpExpiry() == null ||
                user.getOtpExpiry().isBefore(LocalDateTime.now());

        if (invalid) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepo.save(user);
    }
}