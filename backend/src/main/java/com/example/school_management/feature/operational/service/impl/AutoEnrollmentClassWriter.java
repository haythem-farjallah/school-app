package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Generated Classes commit independently of the Student commands that later use them. */
@Service
@RequiredArgsConstructor
public class AutoEnrollmentClassWriter {
    private final ClassRepository classes;
    private final AcademicYearRepository years;
    private final CurrentSchoolResolver currentSchool;

    public record Result(Long id, String name, boolean created) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result create(Long yearId, GradeLevel grade, String section, String name) {
        var year = years.findById(yearId)
                .filter(y -> y.getSchool().getId().equals(currentSchool.resolve().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found in current school"));
        var existing = classes.findByAcademicYearIdAndNameIgnoreCase(yearId, name);
        if (existing.isPresent()) {
            return existingResult(existing.get(), grade, section);
        }
        ClassEntity created = new ClassEntity();
        created.setAcademicYear(year);
        created.setName(name);
        created.setGradeLevel(grade.name());
        created.setSection(section);
        created.setCapacity(30);
        created.setWeeklyHours(30);
        created = classes.saveAndFlush(created);
        return new Result(created.getId(), created.getName(), true);
    }

    /** A duplicate-name insert must have rolled back before this read starts. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Result> findExisting(Long yearId, GradeLevel grade, String section, String name) {
        return classes.findByAcademicYearIdAndNameIgnoreCase(yearId, name)
                .filter(c -> c.getAcademicYear().getSchool().getId().equals(currentSchool.resolve().getId()))
                .map(c -> existingResult(c, grade, section));
    }

    private Result existingResult(ClassEntity existing, GradeLevel grade, String section) {
        if (!grade.name().equals(existing.getGradeLevel()) || !section.equals(existing.getSection())) {
            throw new IllegalStateException("Generated class name is occupied by a different class");
        }
        return new Result(existing.getId(), existing.getName(), false);
    }
}
