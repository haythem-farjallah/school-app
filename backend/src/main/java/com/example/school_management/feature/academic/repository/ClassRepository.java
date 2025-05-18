package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> , JpaSpecificationExecutor<ClassEntity> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByName(String name);
}
