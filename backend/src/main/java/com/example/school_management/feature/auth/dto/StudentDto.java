package com.example.school_management.feature.auth.dto;

import java.time.LocalDate;

public record StudentDto(
        /* ------------ identity ------------ */
        Long id,

        /* ------------ profile ------------ */
        String firstName,
        String lastName,
        String email,
        String telephone,
        LocalDate birthday,
        String gender,
        String address,

        /* ------------ student-specific ------------ */
        String gradeLevel,
        Integer enrollmentYear
) {}