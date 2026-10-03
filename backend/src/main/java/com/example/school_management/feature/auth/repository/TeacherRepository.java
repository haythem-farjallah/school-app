package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Teacher;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface TeacherRepository extends BaseUserRepository<Teacher>, JpaSpecificationExecutor<Teacher> {
    Optional<Teacher> findByEmail(String email);

    @Query("""
        SELECT t FROM Teacher t WHERE t.id = :id AND EXISTS (
            SELECT m.id FROM SchoolMembership m JOIN m.roles role
            WHERE m.user.id = t.id AND m.school.id = :schoolId
              AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
        """)
    Optional<Teacher> findByIdAndSchoolId(@Param("id") Long id, @Param("schoolId") Long schoolId);

    @Query("""
        SELECT t FROM Teacher t WHERE EXISTS (
            SELECT m.id FROM SchoolMembership m JOIN m.roles role
            WHERE m.user.id = t.id AND m.school.id = :schoolId
              AND role = com.example.school_management.feature.membership.entity.MembershipRole.TEACHER)
        """)
    List<Teacher> findBySchoolId(@Param("schoolId") Long schoolId);
}
