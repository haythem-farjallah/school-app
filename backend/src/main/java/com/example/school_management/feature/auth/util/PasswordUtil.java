package com.example.school_management.feature.auth.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordUtil {

    private static final int LENGTH = 8;
    private final SecureRandom rnd = new SecureRandom();

    public String generate() {
        return RandomStringUtils.random(
                LENGTH, 0, 0,
                true,
                true,
                null, rnd);
    }
}
