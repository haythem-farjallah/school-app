package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Parent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParentRepository extends BaseUserRepository<Parent>  {

    @Query("""
        SELECT p FROM Parent p WHERE p.id = :id AND EXISTS (
            SELECT m.id FROM SchoolMembership m JOIN m.roles role
            WHERE m.user.id = p.id AND m.school.id = :schoolId
              AND role = com.example.school_management.feature.membership.entity.MembershipRole.GUARDIAN)
        """)
    Optional<Parent> findByIdAndSchoolId(@Param("id") Long id, @Param("schoolId") Long schoolId);

    @Query("""
        SELECT p FROM Parent p 
        JOIN p.children s 
        WHERE s.id = :studentId
    """)
    List<Parent> findByStudentId(@Param("studentId") Long studentId);
}
