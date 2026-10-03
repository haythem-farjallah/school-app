package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.service.EmailService;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import com.example.school_management.feature.membership.entity.*;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Intentionally no wrapping test transaction: assertions inspect committed state after HTTP failures.
@IntegrationTest
@RecordApplicationEvents
class ParentDirectoryAtomicityIntegrationTest {
    private static final String BASE = "/api/admin/parent-management";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ApplicationEvents events;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @MockitoBean EmailService emailService;
    @MockitoBean com.example.school_management.feature.communication.service.EmailService welcomeEmailService;

    private School school;
    private School foreignSchool;
    private Student oldChild;
    private Student validChild;
    private Student foreignChild;
    private Student wrongRoleChild;
    private Parent parent;
    private String createdEmail;
    private final List<Long> accountIds = new ArrayList<>();

    @BeforeEach
    void committedFixtures() {
        createdEmail = UUID.randomUUID() + "@parent-create.test";
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            school = school("Parent current");
            foreignSchool = school("Parent foreign");
            doReturn(school).when(currentSchool).resolve();
            oldChild = account(new Student(), UserRole.STUDENT);
            validChild = account(new Student(), UserRole.STUDENT);
            foreignChild = account(new Student(), UserRole.STUDENT);
            wrongRoleChild = account(new Student(), UserRole.STUDENT);
            parent = account(new Parent(), UserRole.PARENT);
            parent.setTelephone("original");
            parent.getChildren().add(oldChild);
            membership(oldChild, school, MembershipRole.STUDENT, MembershipStatus.INACTIVE);
            membership(validChild, school, MembershipRole.STUDENT, MembershipStatus.SUSPENDED);
            membership(foreignChild, foreignSchool, MembershipRole.STUDENT, MembershipStatus.ACTIVE);
            membership(wrongRoleChild, school, MembershipRole.GUARDIAN, MembershipStatus.ACTIVE);
            membership(parent, school, MembershipRole.GUARDIAN, MembershipStatus.ACTIVE);
        });
    }

    @AfterEach
    void cleanupOnlyTheseCommittedFixtures() {
        accountIds.addAll(jdbc.queryForList("SELECT id FROM users WHERE email = ?", Long.class, createdEmail));
        for (Long id : accountIds) {
            jdbc.update("DELETE FROM audit_events WHERE entity_id = ? AND entity_type = 'PARENT'", id);
            jdbc.update("DELETE FROM parent_students WHERE parent_id = ? OR student_id = ?", id, id);
            jdbc.update("DELETE FROM school_membership_roles WHERE membership_id IN (SELECT id FROM school_memberships WHERE user_id = ?)", id);
            jdbc.update("DELETE FROM school_memberships WHERE user_id = ?", id);
            jdbc.update("DELETE FROM parent WHERE id = ?", id);
            jdbc.update("DELETE FROM student WHERE id = ?", id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM schools WHERE id IN (?, ?)", school.getId(), foreignSchool.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign", "missing", "wrongRole"})
    void createWithOneInvalidChildLeavesNoParentOrMembership(String invalid) throws Exception {
        mvc.perform(post(BASE).with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "profile", Map.of("firstName", "New", "lastName", "Parent", "email", createdEmail),
                                "childrenEmails", List.of(validChild.getEmail(), invalidEmail(invalid))))))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Long.class, createdEmail)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships m JOIN users u ON u.id = m.user_id WHERE u.email = ?", Long.class, createdEmail)).isZero();
        assertThat(events.stream(UserCreatedEvent.class)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign", "missing", "wrongRole"})
    void patchWithInvalidChildPreservesProfileAndPreviousChildren(String invalid) throws Exception {
        mvc.perform(patch(BASE + "/" + parent.getId()).with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "telephone", "changed", "children", List.of(validChild.getEmail(), invalidEmail(invalid))))))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT telephone FROM users WHERE id = ?", String.class, parent.getId())).isEqualTo("original");
        assertThat(jdbc.queryForList("SELECT student_id FROM parent_students WHERE parent_id = ?", Long.class, parent.getId()))
                .containsExactly(oldChild.getId());
    }

    @Test
    void validChildrenWithInactiveMembershipsCanBeAssignedAndCreationProvisionsGuardianMembership() throws Exception {
        mvc.perform(post(BASE).with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "profile", Map.of("firstName", "New", "lastName", "Parent", "email", createdEmail),
                                "childrenEmails", List.of(oldChild.getEmail(), validChild.getEmail())))))
                .andExpect(status().isCreated());
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, createdEmail);
        assertThat(jdbc.queryForList("SELECT student_id FROM parent_students WHERE parent_id = ?", Long.class, id))
                .containsExactlyInAnyOrder(oldChild.getId(), validChild.getId());
        assertThat(jdbc.queryForList("SELECT r.role FROM school_memberships m JOIN school_membership_roles r ON r.membership_id = m.id WHERE m.user_id = ? AND m.school_id = ?",
                String.class, id, school.getId())).containsExactly("GUARDIAN");
        assertThat(events.stream(UserCreatedEvent.class)).hasSize(1);
        mvc.perform(patch(BASE + "/" + parent.getId()).with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("children", List.of(validChild.getEmail())))))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT student_id FROM parent_students WHERE parent_id = ?", Long.class, parent.getId()))
                .containsExactly(validChild.getId());
    }

    private String invalidEmail(String kind) {
        return switch (kind) {
            case "foreign" -> foreignChild.getEmail();
            case "wrongRole" -> wrongRoleChild.getEmail();
            default -> "missing@children.test";
        };
    }

    private School school(String name) {
        School value = new School();
        value.setName(name);
        em.persist(value);
        return value;
    }

    private <T extends BaseUser> T account(T value, UserRole role) {
        value.setRole(role);
        value.setFirstName("Parent fixture");
        value.setLastName("Person");
        value.setEmail(UUID.randomUUID() + "@children.test");
        value.setStatus(Status.ACTIVE);
        em.persist(value);
        accountIds.add(value.getId());
        return value;
    }

    private void membership(BaseUser account, School owner, MembershipRole role, MembershipStatus status) {
        SchoolMembership value = new SchoolMembership();
        value.setUser(account);
        value.setSchool(owner);
        value.setRoles(new HashSet<>(Set.of(role)));
        value.setStatus(status);
        em.persist(value);
    }
}
