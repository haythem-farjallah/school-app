package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentAcademicYearResolver {
    private final CurrentSchoolResolver currentSchool;
    private final AcademicYearRepository academicYears;

    @Transactional(readOnly = true)
    public AcademicYear resolve() {
        var candidates = academicYears.findAllBySchoolIdAndActiveTrue(currentSchool.resolve().getId());
        if (candidates.isEmpty()) {
            throw new ConfigurationException("Current School has no active AcademicYear configured");
        }
        if (candidates.size() != 1) {
            throw new ConfigurationException("Current School has multiple active AcademicYears");
        }
        return candidates.get(0);
    }

    public static final class ConfigurationException extends IllegalStateException {
        public ConfigurationException(String message) {
            super(message);
        }
    }
}
