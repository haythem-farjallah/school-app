package com.example.school_management.feature.membership.repository;

import com.example.school_management.feature.membership.entity.SchoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.school_management.feature.membership.entity.MembershipRole;

import com.example.school_management.feature.auth.entity.BaseUser;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SchoolMembershipRepository extends JpaRepository<SchoolMembership, Long> {
    Optional<SchoolMembership> findByUserIdAndSchoolId(Long userId, Long schoolId);

    boolean existsByUserIdAndSchoolId(Long userId, Long schoolId);

    List<SchoolMembership> findAllByUserId(Long userId);

    List<SchoolMembership> findAllBySchoolId(Long schoolId);

    @Query("SELECT COUNT(DISTINCT m.user.id) FROM SchoolMembership m WHERE m.school.id = :schoolId AND :role MEMBER OF m.roles")
    long countUsersBySchoolIdAndRole(@Param("schoolId") Long schoolId, @Param("role") MembershipRole role);

    @Query("SELECT DISTINCT m.user FROM SchoolMembership m JOIN m.roles role WHERE m.school.id = :schoolId AND role IN :roles")
    List<BaseUser> findUsersBySchoolIdAndRoles(@Param("schoolId") Long schoolId,
                                            @Param("roles") Collection<MembershipRole> roles);

    @Query("""
        SELECT DISTINCT m.user FROM SchoolMembership m JOIN m.roles role
        WHERE m.school.id = :schoolId AND m.user.id IN :userIds AND role IN :roles
          AND m.user.role <> com.example.school_management.feature.auth.entity.UserRole.STAFF
        """)
    List<BaseUser> findUsersBySchoolIdAndIdsAndRoles(@Param("schoolId") Long schoolId,
                                                  @Param("userIds") Collection<Long> userIds,
                                                  @Param("roles") Collection<MembershipRole> roles);
}
