package com.example.school_management.feature.membership;

import com.example.school_management.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class SchoolMembershipSchemaIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void userSchoolPairIsUnique() {
        long user = user();
        long school = school();
        membership(user, school);
        assertSqlState(() -> membership(user, school), "23505");
    }

    @Test
    void differentUsersInSameSchoolAndSameUserInDifferentSchoolsAreAllowed() {
        long user = user();
        long school = school();
        long first = membership(user, school);
        long second = membership(user(), school);
        long third = membership(user, school());
        assertThat(first).isNotEqualTo(second).isNotEqualTo(third);
        assertThat(second).isNotEqualTo(third);
    }

    @Test
    void duplicateRoleIsRejected() {
        long membership = membership(user(), school());
        jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, 'TEACHER')", membership);
        assertSqlState(() -> jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, 'TEACHER')", membership), "23505");
    }

    @ParameterizedTest
    @CsvSource({"user_id,23502", "school_id,23502", "status,23502", "joined_at,23502"})
    void membershipRequiredColumnsCannotBeNull(String column, String state) {
        long membership = membership(user(), school());
        assertSqlState(() -> jdbc.update("UPDATE school_memberships SET " + column + " = NULL WHERE id = ?", membership), state);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user_id", "school_id"})
    void membershipCannotReferenceMissingParents(String column) {
        long membership = membership(user(), school());
        assertSqlState(() -> jdbc.update("UPDATE school_memberships SET " + column + " = ? WHERE id = ?", Long.MAX_VALUE, membership), "23503");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DELETED", "UNKNOWN", "active"})
    void unsupportedMembershipStatusesAreRejected(String status) {
        long membership = membership(user(), school());
        assertSqlState(() -> jdbc.update("UPDATE school_memberships SET status = ? WHERE id = ?", status, membership), "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {"STAFF", "PARENT", "UNKNOWN", "teacher"})
    void unsupportedMembershipRolesAreRejected(String role) {
        long membership = membership(user(), school());
        assertSqlState(() -> jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, ?)", membership, role), "23514");
    }

    @Test
    void roleRequiresExistingMembership() {
        assertSqlState(() -> jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, 'ADMIN')", Long.MAX_VALUE), "23503");
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership_id", "role"})
    void roleRowsRequireMembershipAndRole(String column) {
        long membership = membership(user(), school());
        jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, 'TEACHER')", membership);
        assertSqlState(() -> jdbc.update("UPDATE school_membership_roles SET " + column + " = NULL WHERE membership_id = ?", membership), "23502");
    }

    @Test
    void membershipDeletionCascadesOnlyToItsRoleRows() {
        long user = user();
        long school = school();
        long membership = membership(user, school);
        jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, 'TEACHER'), (?, 'GUARDIAN')", membership, membership);
        jdbc.update("DELETE FROM school_memberships WHERE id = ?", membership);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles WHERE membership_id = ?", Long.class, membership)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Long.class, user)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM schools WHERE id = ?", Long.class, school)).isEqualTo(1);
    }

    @Test
    void referencedUserCannotBeDeleted() {
        long user = user();
        membership(user, school());
        assertSqlState(() -> jdbc.update("DELETE FROM users WHERE id = ?", user), "23503");
    }

    @Test
    void referencedSchoolCannotBeDeleted() {
        long school = school();
        membership(user(), school);
        assertSqlState(() -> jdbc.update("DELETE FROM schools WHERE id = ?", school), "23503");
    }

    private long user() {
        return jdbc.queryForObject("INSERT INTO users(role) VALUES ('TEACHER') RETURNING id", Long.class);
    }

    private long school() {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES ('Membership Test School') RETURNING id", Long.class);
    }

    private long membership(long user, long school) {
        return jdbc.queryForObject("INSERT INTO school_memberships(user_id, school_id, status, joined_at) VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP) RETURNING id", Long.class, user, school);
    }

    private void assertSqlState(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String state) {
        assertThatThrownBy(operation).rootCause().isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getSQLState()).isEqualTo(state));
    }
}
