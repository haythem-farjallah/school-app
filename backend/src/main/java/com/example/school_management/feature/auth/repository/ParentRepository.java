package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Parent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParentRepository extends BaseUserRepository<Parent>  {

    @Query("""
        SELECT p FROM Parent p 
        JOIN p.children s 
        WHERE s.id = :studentId
    """)
    List<Parent> findByStudentId(@Param("studentId") Long studentId);
}
