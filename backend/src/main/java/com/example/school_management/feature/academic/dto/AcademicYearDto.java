package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.academic.entity.AcademicYear;

import java.time.LocalDate;

public record AcademicYearDto(Long id, String name, LocalDate startDate, LocalDate endDate, boolean active) {
    public static AcademicYearDto from(AcademicYear year) {
        return new AcademicYearDto(year.getId(), year.getName(), year.getStartDate(), year.getEndDate(), year.isActive());
    }
}
