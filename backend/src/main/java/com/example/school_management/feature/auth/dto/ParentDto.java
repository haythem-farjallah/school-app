package com.example.school_management.feature.auth.dto;

import java.time.LocalDate;
import java.util.Set;

public record ParentDto(
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

        /* ------------ parent-specific ------------ */
        String preferredContactMethod,
        String relation,
        
        /* ------------ children ------------ */
        Set<StudentDto> children
) {} 