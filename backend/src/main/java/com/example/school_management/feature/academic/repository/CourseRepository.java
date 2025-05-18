package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    boolean existsByNameIgnoreCase(String name);
}