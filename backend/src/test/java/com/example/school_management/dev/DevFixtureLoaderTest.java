package com.example.school_management.dev;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.entity.enums.Relation;
import com.example.school_management.feature.auth.repository.AdministrationRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.repository.SchoolRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class DevFixtureLoaderTest {

    @Autowired
    DevFixtureLoader loader;

    @Autowired
    AdministrationRepository administrationRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    ParentRepository parentRepository;

    @Autowired UserRepository users;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired SchoolRepository schools;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void createsOneAccountPerRoleAndLinksParentToStudent() {
        // ApplicationRunner executes the real loader when the fixtures context starts.
        assertFixturesAndMemberships();

        Parent parent = parentRepository.findByEmail(DevFixtureLoader.PARENT_EMAIL).orElseThrow();
        assertThat(parent.getRelation()).isEqualTo(Relation.GUARDIAN);
        assertThat(parent.getChildren())
                .extracting(Student::getEmail)
                .containsExactly(DevFixtureLoader.STUDENT_EMAIL);
        Student student = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parent_students WHERE parent_id = ? AND student_id = ?",
                Long.class, parent.getId(), student.getId())).isEqualTo(1);
    }

    @Test
    @Transactional
    void repairsExistingFixtureAccountsThatHaveNoMemberships() {
        var existing = fixtureUsers();
        var ids = existing.stream().map(BaseUser::getId).toList();
        removeFixtureMemberships(existing);
        entityManager.clear();

        loader.run(new DefaultApplicationArguments());
        entityManager.flush();
        entityManager.clear();

        assertThat(fixtureUsers()).extracting(BaseUser::getId).containsExactlyElementsOf(ids);
        assertFixturesAndMemberships();
    }

    @Test
    void repeatedReconciliationAcrossTransactionsDoesNotDuplicateAccountsMembershipsOrRoles() {
        long userCount = users.count();
        var ids = fixtureUsers().stream().map(BaseUser::getId).toList();

        loader.run(new DefaultApplicationArguments());
        long membershipCount = memberships.count();
        long roleCount = jdbc.queryForObject("SELECT count(*) FROM school_membership_roles", Long.class);

        loader.run(new DefaultApplicationArguments());

        assertThat(users.count()).isEqualTo(userCount);
        assertThat(fixtureUsers()).extracting(BaseUser::getId).containsExactlyElementsOf(ids);
        assertThat(memberships.count()).isEqualTo(membershipCount);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles", Long.class)).isEqualTo(roleCount);
        for (BaseUser user : fixtureUsers()) {
            assertThat(memberships.findAllByUserId(user.getId())).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles r JOIN school_memberships m ON m.id = r.membership_id WHERE m.user_id = ?",
                    Long.class, user.getId())).isEqualTo(1);
        }
    }

    @Test
    @Transactional
    void reconciliationPreservesExistingMembershipStatusAndAdditionalRoles() {
        var existing = fixtureUsers();
        removeFixtureMemberships(existing);
        BaseUser teacher = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        SchoolMembership membership = new SchoolMembership();
        membership.setUser(teacher);
        membership.setSchool(schools.findAll().get(0));
        membership.setStatus(MembershipStatus.SUSPENDED);
        membership.getRoles().add(MembershipRole.TEACHER);
        membership.getRoles().add(MembershipRole.GUARDIAN);
        memberships.saveAndFlush(membership);
        entityManager.clear();
        var joinedAt = memberships.findById(membership.getId()).orElseThrow().getJoinedAt();

        loader.run(new DefaultApplicationArguments());
        entityManager.flush();
        entityManager.clear();

        SchoolMembership reloaded = memberships.findById(membership.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(MembershipRole.TEACHER, MembershipRole.GUARDIAN);
        assertThat(reloaded.getJoinedAt()).isEqualTo(joinedAt);
        for (BaseUser user : fixtureUsers()) {
            assertThat(memberships.findAllByUserId(user.getId())).hasSize(1);
        }
    }

    private void assertFixturesAndMemberships() {
        assertThat(schools.count()).isEqualTo(1);
        assertFixture(administrationRepository.findByEmail(DevFixtureLoader.ADMIN_EMAIL).orElseThrow(),
                UserRole.ADMIN, MembershipRole.ADMIN, "Ada", "Admin");
        assertFixture(teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow(),
                UserRole.TEACHER, MembershipRole.TEACHER, "Theo", "Teacher");
        assertFixture(studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow(),
                UserRole.STUDENT, MembershipRole.STUDENT, "Sam", "Student");
        assertFixture(parentRepository.findByEmail(DevFixtureLoader.PARENT_EMAIL).orElseThrow(),
                UserRole.PARENT, MembershipRole.GUARDIAN, "Pat", "Parent");
    }

    private void assertFixture(BaseUser user, UserRole legacyRole, MembershipRole membershipRole,
                               String firstName, String lastName) {
        assertThat(user.getRole()).isEqualTo(legacyRole);
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.isPasswordChangeRequired()).isFalse();
        assertThat(user.getIsEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches(DevFixtureLoader.PASSWORD, user.getPassword())).isTrue();
        var found = memberships.findAllByUserId(user.getId());
        assertThat(found).hasSize(1);
        SchoolMembership membership = found.get(0);
        assertThat(membership.getSchool().getId()).isEqualTo(schools.findAll().get(0).getId());
        assertThat(membership.getRoles()).containsExactly(membershipRole);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles WHERE membership_id = ?",
                Long.class, membership.getId())).isEqualTo(1);
    }

    private List<BaseUser> fixtureUsers() {
        return List.of(
                users.findByEmail(DevFixtureLoader.ADMIN_EMAIL).orElseThrow(),
                users.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow(),
                users.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow(),
                users.findByEmail(DevFixtureLoader.PARENT_EMAIL).orElseThrow());
    }

    private void removeFixtureMemberships(List<BaseUser> fixtures) {
        for (BaseUser user : fixtures) {
            memberships.deleteAll(memberships.findAllByUserId(user.getId()));
        }
        memberships.flush();
        for (BaseUser user : fixtures) {
            assertThat(memberships.findAllByUserId(user.getId())).isEmpty();
        }
    }
}
