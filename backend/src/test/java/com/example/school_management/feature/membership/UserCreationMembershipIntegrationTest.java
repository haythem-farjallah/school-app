package com.example.school_management.feature.membership;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.dto.*;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.*;
import com.example.school_management.feature.communication.service.EmailService;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

@IntegrationTest
@Import(UserCreationMembershipIntegrationTest.MembershipObserver.class)
@Transactional
class UserCreationMembershipIntegrationTest {
    @Autowired AuthService auth;
    @Autowired TeacherService teachers;
    @Autowired StudentService students;
    @Autowired ParentService parents;
    @Autowired AdministrationService administrators;
    @Autowired StaffService staff;
    @Autowired AdminService studentWithParents;
    @Autowired UserRepository users;
    @Autowired SchoolMembershipRepository memberships;
    @MockitoSpyBean SchoolRepository schools;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired MembershipObserver observer;
    @MockitoBean EmailService emailService;

    @BeforeEach
    void clearObservedEvents() {
        observer.membershipPresentAtEvent.clear();
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, ADMIN", "TEACHER, TEACHER", "STUDENT, STUDENT", "PARENT, GUARDIAN"})
    void registrationPersistsMembershipAlongsideUser(UserRole legacy, MembershipRole canonical) {
        String email = email();
        auth.register(request(email, legacy));
        entityManager.flush();
        entityManager.clear();

        BaseUser user = users.findByEmail(email).orElseThrow();
        assertMembership(user, canonical);
        assertThat(user.isPasswordChangeRequired()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, ADMIN", "TEACHER, TEACHER", "STUDENT, STUDENT", "PARENT, GUARDIAN"})
    void crudCreationProvisionsMembershipBeforePublishingUserCreatedEvent(UserRole legacy, MembershipRole canonical) {
        BaseUser user = create(legacy, email());
        entityManager.flush();
        entityManager.clear();

        assertMembership(user, canonical);
        if (legacy != UserRole.ADMIN) {
            assertThat(observer.membershipPresentAtEvent).containsEntry(user.getId(), true);
        }
        assertThat(user.isPasswordChangeRequired()).isEqualTo(legacy != UserRole.ADMIN);
    }

    @Test
    void studentWithParentsProvisionsEveryNewAccount() {
        String studentEmail = email();
        String parentEmail = email();
        studentWithParents.createStudentWithParents(family(studentEmail, parentEmail));
        entityManager.flush();
        entityManager.clear();

        assertMembership(users.findByEmail(studentEmail).orElseThrow(), MembershipRole.STUDENT);
        assertMembership(users.findByEmail(parentEmail).orElseThrow(), MembershipRole.GUARDIAN);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void staffCreationWorksWithoutUnambiguousSchoolContext(int schoolCount) {
        if (schoolCount == 0) {
            // Migration fixtures reference the legacy School; simulate an empty resolver query.
            doReturn(new PageImpl<School>(List.of())).when(schools).findAll(any(Pageable.class));
        } else {
            addSchool();
        }
        String registeredEmail = email();
        auth.register(request(registeredEmail, UserRole.STAFF));
        BaseUser created = create(UserRole.STAFF, email());
        entityManager.flush();
        entityManager.clear();

        BaseUser registered = users.findByEmail(registeredEmail).orElseThrow();
        assertThat(memberships.findAllByUserId(registered.getId())).isEmpty();
        assertThat(memberships.findAllByUserId(created.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"register-ADMIN", "register-TEACHER", "register-STUDENT", "register-PARENT",
            "crud-ADMIN", "crud-TEACHER", "crud-STUDENT", "crud-PARENT", "family"})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void ambiguousSchoolRollsBackUserCreationWithoutPublishingEventsOrSendingEmail(String path) {
        School extra = addSchool();
        try {
            assertCreationRollsBack(path, "multiple Schools");
        } finally {
            schools.deleteById(extra.getId());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"register-ADMIN", "register-TEACHER", "register-STUDENT", "register-PARENT",
            "crud-ADMIN", "crud-TEACHER", "crud-STUDENT", "crud-PARENT", "family"})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void missingSchoolRollsBackUserCreationWithoutPublishingEventsOrSendingEmail(String path) {
        doReturn(new PageImpl<School>(List.of())).when(schools).findAll(any(Pageable.class));
        assertCreationRollsBack(path, "no Schools");
    }

    private void assertCreationRollsBack(String path, String reason) {
        String email = email();
        String parentEmail = email();
        long membershipCount = memberships.count();
        assertThatThrownBy(() -> {
            if (path.startsWith("register-")) {
                auth.register(request(email, UserRole.valueOf(path.substring(9))));
            } else if (path.startsWith("crud-")) {
                create(UserRole.valueOf(path.substring(5)), email);
            } else {
                studentWithParents.createStudentWithParents(family(email, parentEmail));
            }
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining(reason);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email IN (?, ?)", Long.class, email, parentEmail)).isZero();
        assertThat(memberships.count()).isEqualTo(membershipCount);
        assertThat(observer.membershipPresentAtEvent).isEmpty();
        verifyNoInteractions(emailService);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void successfulRegistrationCommitsUserAndMembershipTogether() {
        String email = email();
        auth.register(request(email, UserRole.TEACHER));
        BaseUser user = users.findByEmail(email).orElseThrow();
        try {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE user_id = ?", Long.class, user.getId())).isEqualTo(1);
            assertThat(jdbc.queryForList("SELECT r.role FROM school_membership_roles r JOIN school_memberships m ON m.id = r.membership_id WHERE m.user_id = ?", String.class, user.getId())).containsExactly("TEACHER");
        } finally {
            memberships.deleteAll(memberships.findAllByUserId(user.getId()));
            users.deleteById(user.getId());
        }
    }

    private BaseUser create(UserRole role, String email) {
        BaseUserCreateDto profile = profile(role, email);
        return switch (role) {
            case ADMIN -> administrators.create(new AdministrationCreateDto(profile, "Administration", "Principal", "Test-Pass-2026"));
            case TEACHER -> teachers.create(new TeacherCreateDto(profile, "M.Ed", "Mathematics", 20, null));
            case STUDENT -> students.create(new StudentCreateDto(profile, "HIGH", 2026));
            case PARENT -> parents.create(new ParentCreateDto(profile, "EMAIL", "GUARDIAN", List.of()));
            case STAFF -> staff.create(new StaffCreateDto(profile, null, "Operations"));
        };
    }

    private void assertMembership(BaseUser user, MembershipRole role) {
        var found = memberships.findAllByUserId(user.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getRoles()).containsExactly(role);
        assertThat(found.get(0).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    private School addSchool() {
        School school = new School();
        school.setName("Provisioning test school");
        return schools.saveAndFlush(school);
    }

    private RegisterRequest request(String email, UserRole role) {
        return new RegisterRequest("New", "Account", email, "Test-Pass-2026", role);
    }

    private BaseUserCreateDto profile(UserRole role, String email) {
        return new BaseUserCreateDto("New", "Account", email, null, null, null, null, role);
    }

    private CreateStudentWithParentsRequest family(String studentEmail, String parentEmail) {
        return new CreateStudentWithParentsRequest(
                new StudentDtoCreate("New", "Student", studentEmail, "12345678", LocalDateTime.of(2010, 1, 1, 0, 0), null, null, "HIGH", 2026),
                List.of(new ParentCreateDto(profile(UserRole.PARENT, parentEmail), "EMAIL", "GUARDIAN", List.of())));
    }

    private String email() {
        return "membership-" + UUID.randomUUID() + "@school.test";
    }

    static class MembershipObserver {
        private final SchoolMembershipRepository memberships;
        final Map<Long, Boolean> membershipPresentAtEvent = new HashMap<>();

        MembershipObserver(SchoolMembershipRepository memberships) {
            this.memberships = memberships;
        }

        @EventListener
        public void onUserCreated(UserCreatedEvent event) {
            membershipPresentAtEvent.put(event.user().getId(),
                    !memberships.findAllByUserId(event.user().getId()).isEmpty());
        }
    }
}
