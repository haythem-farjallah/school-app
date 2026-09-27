package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Forgot-password flow: a six-digit code is emailed to an active account and exchanged for a
 * new password. Neither step tells the caller whether the email belongs to an account.
 *
 * A successful reset revokes every token issued to the account before it.
 *
 * Only a one-way hash of the code is stored. A code expires after {@link #OTP_LIFETIME}, is
 * discarded after {@link #MAX_FAILED_ATTEMPTS} wrong attempts, and works once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration OTP_LIFETIME = Duration.ofMinutes(10);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_FAILED_ATTEMPTS = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    /**
     * Step 1: email a reset code to the account if it is active. Completes normally for unknown,
     * inactive and unreachable accounts too, so the caller always gets the same answer.
     */
    public void requestReset(String email) {
        try {
            transactionTemplate.executeWithoutResult(status -> issueCode(email));
        } catch (RuntimeException ex) {
            // The transaction rolled back, so the account keeps no code the user never received.
            log.warn("Password-reset code not issued: {}", ex.getClass().getSimpleName());
        }
    }

    private void issueCode(String email) {
        BaseUser user = userRepo.findByEmailForUpdate(email)
                .filter(u -> u.getStatus() == Status.ACTIVE)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (user == null || issuedWithinCooldown(user, now)) {
            return;
        }

        String code = RandomStringUtils.random(6, 0, 0, false, true, null, SECURE_RANDOM);
        user.setOtpCode(passwordEncoder.encode(code));
        user.setOtpExpiry(now.plus(OTP_LIFETIME));
        user.setOtpFailedAttempts(0);
        // Write before sending, so a failing write cannot leave an emailed code that does not exist.
        userRepo.saveAndFlush(user);

        log.info("Password-reset code generated");

        emailService.sendTemplateEmail(
                user.getEmail(),
                "Your password-reset code",
                "otp",                 // Thymeleaf template name
                Map.of(
                    "otp", code,
                    "code", code,  // Template uses 'code' variable
                    "name", user.getFirstName() != null ? user.getFirstName() : "User"
                )
        );
    }

    /** A code's issue time is its expiry minus its lifetime, so no separate timestamp is stored. */
    private static boolean issuedWithinCooldown(BaseUser user, LocalDateTime now) {
        return user.getOtpCode() != null
                && user.getOtpExpiry() != null
                && user.getOtpExpiry().minus(OTP_LIFETIME).plus(RESEND_COOLDOWN).isAfter(now);
    }

    /**
     * Step 2: set a new password with the emailed code. Unknown and inactive accounts and wrong,
     * expired or discarded codes all fail with the same 401. Wrong attempts are counted even
     * though the request fails, hence no rollback for that rejection.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void resetPassword(String email, String code, String newPassword) {
        BaseUser user = userRepo.findByEmailForUpdate(email)
                .filter(u -> u.getStatus() == Status.ACTIVE)
                .orElseThrow(PasswordResetService::invalidCode);
        if (!consumeCode(user, code)) {
            throw invalidCode();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        // Every access and refresh token issued before the reset stops working.
        user.setTokenVersion(user.getTokenVersion() + 1);
        log.info("Password reset completed");
    }

    /** Returns whether the code is the account's current code; consumes or counts it either way. */
    private boolean consumeCode(BaseUser user, String code) {
        if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
            return false;
        }
        if (!user.getOtpExpiry().isAfter(LocalDateTime.now())) {
            clearCode(user);
            return false;
        }
        if (passwordEncoder.matches(code, user.getOtpCode())) {
            clearCode(user);
            return true;
        }

        int failedAttempts = user.getOtpFailedAttempts() + 1;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            clearCode(user);
        } else {
            user.setOtpFailedAttempts(failedAttempts);
        }
        return false;
    }

    private static void clearCode(BaseUser user) {
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpFailedAttempts(0);
    }

    private static ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
    }
}
