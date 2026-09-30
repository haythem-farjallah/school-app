package com.example.school_management.feature.school;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class AcademicMasterMigrationIntegrationTest {
    @Autowired
    PostgreSQLContainer<?> postgres;

    private JdbcTemplate jdbc;
    private String jdbcUrl;

    @BeforeEach
    void migrateIsolatedTestDatabaseToAcceptedFoundation() throws Exception {
        String database = "masters_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        jdbcUrl = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
        jdbc = new JdbcTemplate(new DriverManagerDataSource(jdbcUrl, postgres.getUsername(), postgres.getPassword()));
        Flyway.configure().dataSource(jdbcUrl, postgres.getUsername(), postgres.getPassword()).target("54").load().migrate();
        assertThat(jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid IN ('courses'::regclass, 'periods'::regclass)", String.class))
                .contains("courses_code_key", "unique_period_index");
    }

    @Test
    void createsCompatibilitySchoolAndBackfillsWithoutChangingLegacyRows() {
        Map<String, List<Map<String, Object>>> before = snapshot();
        migrate();

        assertThat(jdbc.queryForList("SELECT name FROM schools", String.class)).containsExactly("Legacy School");
        Long selected = jdbc.queryForObject("SELECT id FROM schools", Long.class);
        assertPreservedAndOwned(before, selected);
    }

    @Test
    void reusesExistingSchoolWithoutAssumingItsDatabaseId() {
        Long discarded = school("Discarded Test School");
        jdbc.update("DELETE FROM schools WHERE id = ?", discarded);
        Long selected = school("Configured Test School");
        Map<String, List<Map<String, Object>>> before = snapshot();
        migrate();

        assertThat(selected).isGreaterThan(discarded);
        assertThat(jdbc.queryForList("SELECT name FROM schools", String.class)).containsExactly("Configured Test School");
        assertPreservedAndOwned(before, selected);
    }

    @Test
    void ambiguousOwnershipFailsAndRollsBackWithoutLosingLegacyData() {
        school("Test School A");
        school("Test School B");
        Map<String, List<Map<String, Object>>> before = snapshot();

        assertThatThrownBy(this::migrate).rootCause().isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getMessage()).contains("multiple schools", "unscoped academic masters"));

        assertThat(snapshot()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM schools", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name IN ('courses', 'rooms', 'periods') AND column_name = 'school_id'", Long.class)).isZero();
        assertThat(flyway().info().current().getVersion().getVersion()).isEqualTo("54");
    }

    @Test
    void multipleSchoolsWithNoLegacyMastersNeedNoOwnershipGuess() {
        school("Test School A");
        school("Test School B");
        jdbc.update("DELETE FROM class_courses");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM rooms");
        jdbc.update("DELETE FROM periods");

        migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM schools", Long.class)).isEqualTo(2);
        for (String table : new String[]{"courses", "rooms", "periods"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
        }
    }

    private Flyway flyway() {
        return Flyway.configure().dataSource(jdbcUrl, postgres.getUsername(), postgres.getPassword()).target("55").load();
    }

    private void migrate() {
        Flyway flyway = flyway();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("55");
    }

    private Long school(String name) {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES (?) RETURNING id", Long.class, name);
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        return Map.of("courses", rows("courses"), "rooms", rows("rooms"), "periods", rows("periods"));
    }

    private List<Map<String, Object>> rows(String table) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id");
        rows.forEach(row -> row.remove("school_id"));
        return rows;
    }

    private void assertPreservedAndOwned(Map<String, List<Map<String, Object>>> before, Long school) {
        assertThat(snapshot()).isEqualTo(before);
        for (String table : before.keySet()) {
            assertThat(jdbc.queryForList("SELECT DISTINCT school_id FROM " + table, Long.class)).containsExactly(school);
        }
    }
}
