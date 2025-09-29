package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Parent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParentRepository extends BaseUserRepository<Parent>  {
    Optional<Parent> findByEmailIgnoreCase(String email);

    @Query("""
        SELECT p FROM Parent p 
        JOIN p.children s 
        WHERE s.id = :studentId
    """)
    List<Parent> findByStudentId(@Param("studentId") Long studentId);
}
