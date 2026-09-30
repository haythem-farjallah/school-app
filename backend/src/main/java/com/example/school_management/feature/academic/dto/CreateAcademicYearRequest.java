package com.example.school_management.feature.academic.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAcademicYearRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        boolean active) {
    @AssertTrue(message = "Start date must be before end date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }
}
