package com.example.school_management.feature.school.repository;

import com.example.school_management.feature.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
}
