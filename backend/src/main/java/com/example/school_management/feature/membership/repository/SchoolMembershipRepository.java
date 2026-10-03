package com.example.school_management.feature.membership.repository;

import com.example.school_management.feature.membership.entity.SchoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.school_management.feature.membership.entity.MembershipRole;

import java.util.List;
import java.util.Optional;

public interface SchoolMembershipRepository extends JpaRepository<SchoolMembership, Long> {
    Optional<SchoolMembership> findByUserIdAndSchoolId(Long userId, Long schoolId);

    boolean existsByUserIdAndSchoolId(Long userId, Long schoolId);

    List<SchoolMembership> findAllByUserId(Long userId);

    List<SchoolMembership> findAllBySchoolId(Long schoolId);

    @Query("SELECT COUNT(DISTINCT m.user.id) FROM SchoolMembership m WHERE m.school.id = :schoolId AND :role MEMBER OF m.roles")
    long countUsersBySchoolIdAndRole(@Param("schoolId") Long schoolId, @Param("role") MembershipRole role);
}
