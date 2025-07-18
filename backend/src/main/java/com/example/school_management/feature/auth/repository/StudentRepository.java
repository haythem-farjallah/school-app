package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Status;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends BaseUserRepository<Student>, JpaSpecificationExecutor<Student> {
    long countByStatus(Status status);
    
    // Find students by parent ID through the parent_students join table
    @Query("SELECT s FROM Student s, Parent p WHERE s MEMBER OF p.children AND p.id = :parentId")
    List<Student> findByParentId(@Param("parentId") Long parentId);
}
