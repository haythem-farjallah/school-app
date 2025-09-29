package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Status;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends BaseUserRepository<Student>, JpaSpecificationExecutor<Student> {
    long countByStatus(Status status);
    Optional<Student> findByEmailIgnoreCase(String email);
    
    // Find students by parent ID through the parent_students join table
    @Query(value = """
        SELECT u.*, s.grade_level, s.enrolled_at 
        FROM student s
        JOIN users u ON s.id = u.id
        JOIN parent_students ps ON s.id = ps.student_id
        WHERE ps.parent_id = :parentId
    """, nativeQuery = true)
    List<Student> findByParentId(@Param("parentId") Long parentId);
    
    // Find students by class IDs through enrollments
    @Query("""
        SELECT DISTINCT s FROM Student s 
        JOIN s.enrollments e 
        WHERE e.classEntity.id IN :classIds 
        AND e.status = 'ACTIVE'
    """)
    List<Student> findByClassIds(@Param("classIds") List<Long> classIds);
}
