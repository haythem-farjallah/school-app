package com.example.school_management.feature.membership;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class SchoolMembershipMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void backfillsEligibleLegacyAccountsIntoActualSchoolAndPreservesLegacyState() throws Exception {
        String url = databaseUrl();
        flyway(url, "57").migrate();
        JdbcTemplate jdbc = jdbc(url);
        long school = jdbc.queryForObject("INSERT INTO schools(name) VALUES ('Configured School') RETURNING id", Long.class);
        for (String table : new String[]{"courses", "rooms", "periods"}) {
            jdbc.update("UPDATE " + table + " SET school_id = ?", school);
        }
        jdbc.update("DELETE FROM schools WHERE id <> ?", school);
        assertThat(school).isNotEqualTo(1L);
        LocalDateTime created = LocalDateTime.of(2024, 3, 12, 9, 30);
        long admin = user(jdbc, "ADMIN", "ACTIVE", created);
        long teacher = user(jdbc, "TEACHER", "ACTIVE", created);
        long student = user(jdbc, "STUDENT", "SUSPENDED", created);
        long parent = user(jdbc, "PARENT", "ACTIVE", created);
        long staff = user(jdbc, "STAFF", "ACTIVE", created);
        long deleted = user(jdbc, "TEACHER", "DELETED", created);
        // The current schema requires created_at; simulate an older row missing it.
        jdbc.update("ALTER TABLE users ALTER COLUMN created_at DROP NOT NULL");
        long missingCreation = user(jdbc, "ADMIN", "ACTIVE", null);
        var before = jdbc.queryForList("SELECT * FROM users ORDER BY id");
        LocalDateTime migrationStart = jdbc.queryForObject("SELECT LOCALTIMESTAMP", LocalDateTime.class);

        Flyway flyway = flyway(url, "58");
        flyway.migrate();
        flyway.validate();

        assertThat(jdbc.queryForList("SELECT * FROM users ORDER BY id")).isEqualTo(before);
        assertRole(jdbc, admin, "ADMIN", "ACTIVE", school, created);
        assertRole(jdbc, teacher, "TEACHER", "ACTIVE", school, created);
        assertRole(jdbc, student, "STUDENT", "SUSPENDED", school, created);
        assertRole(jdbc, parent, "GUARDIAN", "ACTIVE", school, created);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE user_id IN (?, ?)",
                Long.class, staff, deleted)).isZero();
        assertThat(jdbc.queryForObject("SELECT joined_at FROM school_memberships WHERE user_id = ?",
                LocalDateTime.class, missingCreation)).isAfterOrEqualTo(migrationStart);
        assertThat(jdbc.queryForList("SELECT DISTINCT school_id FROM school_memberships", Long.class)).containsExactly(school);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships", Long.class))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM users WHERE role IN ('ADMIN','TEACHER','STUDENT','PARENT') AND status IN ('ACTIVE','SUSPENDED')", Long.class));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM (SELECT user_id, school_id FROM school_memberships GROUP BY user_id, school_id HAVING count(*) > 1) duplicates", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles", Long.class))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM school_memberships", Long.class));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships WHERE joined_at IS NULL", Long.class)).isZero();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("58");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void eligibleUsersWithAmbiguousOrMissingSchoolFailAndRollBack(int schoolCount) throws Exception {
        String url = databaseUrl();
        flyway(url, "57").migrate();
        JdbcTemplate jdbc = jdbc(url);
        if (schoolCount == 0) {
            jdbc.update("TRUNCATE schools CASCADE");
        } else {
            jdbc.update("INSERT INTO schools(name) VALUES ('Second School')");
        }
        var before = jdbc.queryForList("SELECT * FROM users ORDER BY id");
        assertThat(before).isNotEmpty();
        Flyway flyway = flyway(url, "58");

        assertThatThrownBy(flyway::migrate).rootCause().isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getMessage()).contains("Cannot backfill school memberships",
                        schoolCount == 0 ? "no schools" : "multiple schools"));

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("57");
        assertThat(jdbc.queryForList("SELECT * FROM users ORDER BY id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM schools", Long.class)).isEqualTo(schoolCount);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('school_memberships','school_membership_roles')", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void noEligibleAccountsRequireNoSchoolOwnershipGuess(int schoolCount) throws Exception {
        String url = databaseUrl();
        flyway(url, "57").migrate();
        JdbcTemplate jdbc = jdbc(url);
        jdbc.update("UPDATE users SET status = 'DELETED'");
        user(jdbc, "STAFF", "ACTIVE", LocalDateTime.of(2024, 3, 12, 9, 30));
        if (schoolCount == 0) {
            jdbc.update("TRUNCATE schools CASCADE");
        } else {
            jdbc.update("INSERT INTO schools(name) VALUES ('Second School')");
        }
        var before = jdbc.queryForList("SELECT * FROM users ORDER BY id");

        Flyway flyway = flyway(url, "58");
        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("58");
        assertThat(jdbc.queryForList("SELECT * FROM users ORDER BY id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM schools", Long.class)).isEqualTo(schoolCount);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_memberships", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM school_membership_roles", Long.class)).isZero();
    }

    @Test
    void fullCleanMigrationValidatesMembershipAndLegacyAuthSchema() throws Exception {
        String url = databaseUrl();
        Flyway flyway = flyway(url, null);
        flyway.migrate();
        flyway.validate();
        JdbcTemplate jdbc = jdbc(url);

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("64");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class))
                .contains("school_memberships", "school_membership_roles", "users", "role_permissions", "user_permissions", "parent", "staff");
        assertThat(jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid IN ('school_memberships'::regclass, 'school_membership_roles'::regclass)", String.class))
                .contains("uk_school_memberships_user_school", "fk_school_memberships_user", "fk_school_memberships_school",
                        "ck_school_memberships_status", "ck_school_membership_roles_role", "school_membership_roles_pkey", "fk_school_membership_roles_membership");
        assertThat(jdbc.queryForList("SELECT enumlabel FROM pg_enum JOIN pg_type ON pg_type.oid = enumtypid WHERE typname = 'user_role'", String.class))
                .contains("ADMIN", "TEACHER", "STUDENT", "PARENT", "STAFF");
    }

    private void assertRole(JdbcTemplate jdbc, long user, String role, String status, long school, LocalDateTime created) {
        var rows = jdbc.queryForList("SELECT m.school_id, m.status, m.joined_at, r.role FROM school_memberships m JOIN school_membership_roles r ON r.membership_id = m.id WHERE m.user_id = ?", user);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("school_id", school).containsEntry("status", status)
                .containsEntry("role", role).containsEntry("joined_at", java.sql.Timestamp.valueOf(created));
    }

    private long user(JdbcTemplate jdbc, String role, String status, LocalDateTime created) {
        return jdbc.queryForObject("INSERT INTO users(role, status, created_at) VALUES (?::user_role, ?::status, ?) RETURNING id", Long.class, role, status, created);
    }

    private Flyway flyway(String url, String target) {
        var configuration = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword());
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private JdbcTemplate jdbc(String url) {
        return new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
    }

    private String databaseUrl() throws Exception {
        String database = "membership_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }
}
