package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    List<AcademicYear> findBySchoolId(Long schoolId);

    Optional<AcademicYear> findBySchoolIdAndActiveTrue(Long schoolId);
}
