package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> , JpaSpecificationExecutor<ClassEntity> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByName(String name);
    
    // Count classes by teacher ID (through multiple relationships)
    @Query("""
        SELECT COUNT(DISTINCT c) FROM ClassEntity c 
        WHERE c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId
            UNION  
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
        )
        """)
    Long countByTeacherId(@Param("teacherId") Long teacherId);
    
    // Find classes by teacher ID (through multiple relationships - TeachingAssignment, TimetableSlot, or direct relationship)
    @Query("""
        SELECT DISTINCT c FROM ClassEntity c 
        LEFT JOIN FETCH c.assignedRoom
        WHERE c.id IN (
            SELECT DISTINCT ta.clazz.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId
            UNION
            SELECT DISTINCT ts.forClass.id FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId
            UNION  
            SELECT DISTINCT ct.id FROM ClassEntity ct JOIN ct.teachers t WHERE t.id = :teacherId
        )
        ORDER BY c.name
        """)
    List<ClassEntity> findByTeacherId(@Param("teacherId") Long teacherId);
}
