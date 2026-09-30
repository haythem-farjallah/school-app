package com.example.school_management.feature.membership.repository;

import com.example.school_management.feature.membership.entity.SchoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolMembershipRepository extends JpaRepository<SchoolMembership, Long> {
    Optional<SchoolMembership> findByUserIdAndSchoolId(Long userId, Long schoolId);

    boolean existsByUserIdAndSchoolId(Long userId, Long schoolId);

    List<SchoolMembership> findAllByUserId(Long userId);

    List<SchoolMembership> findAllBySchoolId(Long schoolId);
}
