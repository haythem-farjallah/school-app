package com.example.school_management;

import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;

import java.time.LocalDate;
import java.util.UUID;

public final class AcademicYearTestFixtures {
    private AcademicYearTestFixtures() {}

    public static AcademicYear create(AcademicYearRepository years, CurrentSchoolResolver school) {
        AcademicYear year = new AcademicYear();
        year.setSchool(school.resolve());
        year.setName("Class fixture " + UUID.randomUUID());
        year.setStartDate(LocalDate.of(2026, 8, 17));
        year.setEndDate(LocalDate.of(2027, 7, 9));
        return years.save(year);
    }
}
