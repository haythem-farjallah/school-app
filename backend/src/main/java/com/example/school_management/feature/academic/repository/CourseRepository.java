package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);

    Optional<Course> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolIdAndNameIgnoreCaseAndIdNot(Long schoolId, String name, Long id);

    boolean existsBySchoolIdAndCode(Long schoolId, String code);
}
