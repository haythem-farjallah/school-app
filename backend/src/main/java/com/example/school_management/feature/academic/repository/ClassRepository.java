package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> , JpaSpecificationExecutor<ClassEntity> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByName(String name);
    
    // Count classes by teacher ID (Many-to-Many relationship)
    @Query("SELECT COUNT(c) FROM ClassEntity c JOIN c.teachers t WHERE t.id = :teacherId")
    Long countByTeacherId(@Param("teacherId") Long teacherId);
    
    // Find classes by teacher ID (Many-to-Many relationship)
    @Query("SELECT c FROM ClassEntity c JOIN c.teachers t WHERE t.id = :teacherId ORDER BY c.name")
    List<ClassEntity> findByTeacherId(@Param("teacherId") Long teacherId);
}
