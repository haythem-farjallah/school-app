package com.example.school_management.feature.membership;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Administration;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.membership.service.SchoolMembershipProvisioningService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class SchoolMembershipProvisioningIntegrationTest {
    @Autowired SchoolMembershipProvisioningService provisioner;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired SchoolRepository schools;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @ParameterizedTest
    @CsvSource({"ADMIN, ADMIN", "TEACHER, TEACHER", "STUDENT, STUDENT", "PARENT, GUARDIAN"})
    void repeatedProvisioningPersistsOneMembershipAndOneMappedRole(UserRole legacy, MembershipRole canonical) {
        BaseUser user = user(legacy, Status.ACTIVE);

        provisioner.provisionFor(user);
        provisioner.provisionFor(user);
        entityManager.flush();
        entityManager.clear();

        SchoolMembership membership = memberships.findAllByUserId(user.getId()).get(0);
        assertThat(membership.getSchool().getId()).isEqualTo(schools.findAll().get(0).getId());
        assertThat(membership.getRoles()).containsExactly(canonical);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getJoinedAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE user_id = ?", Long.class, user.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles WHERE membership_id = ?", Long.class, membership.getId())).isEqualTo(1);
    }

    @Test
    void suspendedUserStartsWithSuspendedMembership() {
        BaseUser user = user(UserRole.TEACHER, Status.SUSPENDED);
        provisioner.provisionFor(user);
        entityManager.flush();
        entityManager.clear();

        assertThat(memberships.findAllByUserId(user.getId()).get(0).getStatus()).isEqualTo(MembershipStatus.SUSPENDED);
    }

    @ParameterizedTest
    @EnumSource(MembershipStatus.class)
    void existingMembershipRetainsStatusAndRolesWhenAnotherRoleIsAdded(MembershipStatus status) {
        BaseUser user = user(UserRole.TEACHER, Status.ACTIVE);
        SchoolMembership existing = new SchoolMembership();
        existing.setUser(user);
        existing.setSchool(schools.findAll().get(0));
        existing.setStatus(status);
        existing.getRoles().add(MembershipRole.GUARDIAN);
        memberships.saveAndFlush(existing);
        entityManager.clear();
        existing = memberships.findById(existing.getId()).orElseThrow();
        var joinedAt = existing.getJoinedAt();

        provisioner.provisionFor(user);
        provisioner.provisionFor(user);
        entityManager.flush();
        entityManager.clear();

        SchoolMembership reloaded = memberships.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(MembershipRole.GUARDIAN, MembershipRole.TEACHER);
        assertThat(reloaded.getStatus()).isEqualTo(status);
        assertThat(reloaded.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles WHERE membership_id = ?", Long.class, existing.getId())).isEqualTo(2);
    }

    @Test
    void deletedAccountLeavesExistingMembershipUntouched() {
        BaseUser user = user(UserRole.TEACHER, Status.ACTIVE);
        provisioner.provisionFor(user);
        entityManager.flush();
        Long membershipId = memberships.findAllByUserId(user.getId()).get(0).getId();

        user.setStatus(Status.DELETED);
        provisioner.provisionFor(user);
        entityManager.flush();
        entityManager.clear();

        SchoolMembership membership = memberships.findById(membershipId).orElseThrow();
        assertThat(membership.getRoles()).containsExactly(MembershipRole.TEACHER);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void changingGlobalAccountStatusDoesNotResynchronizeExistingMembership() {
        BaseUser user = user(UserRole.TEACHER, Status.ACTIVE);
        provisioner.provisionFor(user);

        user.setStatus(Status.SUSPENDED);
        provisioner.provisionFor(user);
        entityManager.flush();
        entityManager.clear();

        assertThat(memberships.findAllByUserId(user.getId()).get(0).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void eligibleUserFailsWhenMultipleSchoolsExist() {
        School extra = new School();
        extra.setName("Second school");
        schools.save(extra);
        BaseUser user = user(UserRole.TEACHER, Status.ACTIVE);

        assertThatThrownBy(() -> provisioner.provisionFor(user))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("multiple Schools");
        assertThat(memberships.findAllByUserId(user.getId())).isEmpty();
    }

    private BaseUser user(UserRole role, Status status) {
        BaseUser user = switch (role) {
            case ADMIN -> new Administration();
            case TEACHER -> new Teacher();
            case STUDENT -> new Student();
            case PARENT -> new Parent();
            case STAFF -> throw new IllegalArgumentException("Use a canonical role in this fixture");
        };
        user.setRole(role);
        user.setStatus(status);
        entityManager.persist(user);
        return user;
    }
}
