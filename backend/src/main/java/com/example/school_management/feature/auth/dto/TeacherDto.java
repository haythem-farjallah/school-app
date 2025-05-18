package com.example.school_management.feature.auth.dto;

import java.time.LocalDate;

public record TeacherDto(
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

        /* ------------ teacher-specific ------------ */
        String qualifications,
        String subjectsTaught,
        Integer availableHours,
        String schedulePreferences
) {}