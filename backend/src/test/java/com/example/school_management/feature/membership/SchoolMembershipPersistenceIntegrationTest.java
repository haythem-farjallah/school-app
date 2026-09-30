package com.example.school_management.feature.membership;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class SchoolMembershipPersistenceIntegrationTest {
    @Autowired SchoolMembershipRepository memberships;
    @Autowired SchoolRepository schools;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired Validator validator;

    @Test
    void membershipRequiresAtLeastOneNonNullRole() {
        SchoolMembership membership = membership(user(), school());
        assertThat(validator.validate(membership))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("roles"));

        membership.getRoles().add(null);
        assertThat(validator.validate(membership))
                .anyMatch(violation -> violation.getPropertyPath().toString().startsWith("roles"));

        membership.setRoles(null);
        assertThat(validator.validate(membership))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("roles"));
    }

    @Test
    void oneMembershipPersistsMultipleRolesWithoutDuplicateRoleRows() {
        Teacher user = user();
        School school = school();
        SchoolMembership membership = membership(user, school, MembershipRole.TEACHER, MembershipRole.GUARDIAN);
        assertThat(membership.getRoles().add(MembershipRole.TEACHER)).isFalse();
        memberships.saveAndFlush(membership);
        entityManager.clear();

        SchoolMembership reloaded = memberships.findByUserIdAndSchoolId(user.getId(), school.getId()).orElseThrow();
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(MembershipRole.TEACHER, MembershipRole.GUARDIAN);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE user_id = ? AND school_id = ?", Long.class, user.getId(), school.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles WHERE membership_id = ?", Long.class, membership.getId())).isEqualTo(2);
    }

    @Test
    void independentSchoolMembershipsCoexistAndRepositoryLookupsStayScoped() {
        Teacher user = user();
        Teacher other = user();
        School first = school();
        School second = school();
        SchoolMembership teacher = memberships.save(membership(user, first, MembershipRole.TEACHER));
        SchoolMembership admin = memberships.save(membership(user, second, MembershipRole.ADMIN));
        SchoolMembership guardian = memberships.saveAndFlush(membership(other, first, MembershipRole.GUARDIAN));
        entityManager.clear();

        assertThat(memberships.findAllByUserId(user.getId())).containsExactlyInAnyOrder(teacher, admin);
        assertThat(memberships.findAllBySchoolId(first.getId())).containsExactlyInAnyOrder(teacher, guardian);
        assertThat(memberships.findByUserIdAndSchoolId(user.getId(), first.getId()).orElseThrow().getRoles()).containsExactly(MembershipRole.TEACHER);
        assertThat(memberships.findByUserIdAndSchoolId(user.getId(), second.getId()).orElseThrow().getRoles()).containsExactly(MembershipRole.ADMIN);
        assertThat(memberships.existsByUserIdAndSchoolId(user.getId(), first.getId())).isTrue();
        assertThat(memberships.existsByUserIdAndSchoolId(other.getId(), second.getId())).isFalse();
        assertThat(memberships.findByUserIdAndSchoolId(other.getId(), second.getId())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(MembershipStatus.class)
    void statusPersistsIndependentlyFromGlobalAccountAndJoinedAtSurvivesUpdate(MembershipStatus status) {
        Teacher user = user();
        SchoolMembership membership = membership(user, school(), MembershipRole.TEACHER);
        membership.setStatus(status);
        memberships.saveAndFlush(membership);
        entityManager.clear();
        membership = memberships.findById(membership.getId()).orElseThrow();
        var joinedAt = membership.getJoinedAt();
        assertThat(joinedAt).isNotNull();
        assertThat(membership.getStatus()).isEqualTo(status);
        assertThat(membership.getUser().getStatus()).isEqualTo(Status.ACTIVE);

        membership.setStatus(status == MembershipStatus.INACTIVE ? MembershipStatus.ACTIVE : MembershipStatus.INACTIVE);
        membership.getRoles().add(MembershipRole.GUARDIAN);
        entityManager.flush();
        entityManager.clear();

        SchoolMembership reloaded = memberships.findById(membership.getId()).orElseThrow();
        assertThat(reloaded.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(reloaded.getStatus()).isEqualTo(membership.getStatus());
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(MembershipRole.TEACHER, MembershipRole.GUARDIAN);
    }

    @Test
    void relationsAndRolesLoadLazilyAndJsonExcludesParentGraphs() {
        SchoolMembership membership = memberships.saveAndFlush(membership(user(), school(), MembershipRole.TEACHER));
        entityManager.clear();
        SchoolMembership reloaded = memberships.findById(membership.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(reloaded.getSchool())).isFalse();
        assertThat(Hibernate.isInitialized(reloaded.getUser())).isFalse();
        assertThat(Hibernate.isInitialized(reloaded.getRoles())).isFalse();
        var json = objectMapper.valueToTree(reloaded);
        assertThat(json.has("user")).isFalse();
        assertThat(json.has("school")).isFalse();
        assertThat(Hibernate.isInitialized(reloaded.getSchool())).isFalse();
        assertThat(Hibernate.isInitialized(reloaded.getUser())).isFalse();
    }

    @Test
    void identityIsStableAcrossMutableStateAndDetachedUninitializedProxy() {
        SchoolMembership membership = membership(user(), school(), MembershipRole.TEACHER);
        assertThat(membership).isEqualTo(membership).isNotEqualTo(new SchoolMembership()).isNotEqualTo(new School());
        int hash = membership.hashCode();
        Set<SchoolMembership> set = new HashSet<>();
        set.add(membership);
        memberships.saveAndFlush(membership);
        membership.setStatus(MembershipStatus.SUSPENDED);
        membership.getRoles().add(MembershipRole.GUARDIAN);
        entityManager.flush();
        entityManager.clear();
        SchoolMembership proxy = entityManager.getReference(SchoolMembership.class, membership.getId());
        entityManager.clear();

        assertThat(Hibernate.isInitialized(proxy)).isFalse();
        assertThat(membership).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(membership);
        assertThat(membership.hashCode()).isEqualTo(hash).isEqualTo(proxy.hashCode());
        assertThat(set).contains(membership, proxy);
        assertThat(Hibernate.isInitialized(proxy)).isFalse();
    }

    @Test
    void repositoryDeleteRemovesRolesAndPreservesUserAndSchool() {
        Teacher user = user();
        School school = school();
        SchoolMembership membership = memberships.saveAndFlush(membership(user, school, MembershipRole.TEACHER, MembershipRole.GUARDIAN));
        entityManager.clear();
        memberships.deleteById(membership.getId());
        memberships.flush();
        entityManager.clear();

        assertThat(memberships.findById(membership.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles WHERE membership_id = ?", Long.class, membership.getId())).isZero();
        assertThat(entityManager.find(Teacher.class, user.getId())).isNotNull();
        assertThat(schools.findById(school.getId())).isPresent();
    }

    private Teacher user() {
        Teacher user = new Teacher();
        user.setRole(UserRole.TEACHER);
        user.setStatus(Status.ACTIVE);
        entityManager.persist(user);
        return user;
    }

    private School school() {
        School school = new School();
        school.setName("Membership Test School");
        return schools.save(school);
    }

    private SchoolMembership membership(Teacher user, School school, MembershipRole... roles) {
        SchoolMembership membership = new SchoolMembership();
        membership.setUser(user);
        membership.setSchool(school);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRoles(new HashSet<>(Set.of(roles)));
        return membership;
    }
}
