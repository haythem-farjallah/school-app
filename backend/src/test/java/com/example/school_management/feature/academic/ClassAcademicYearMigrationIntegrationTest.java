package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class ClassAcademicYearMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void v55UpgradePreservesLegacyRowsWithoutInventingYearsAndAddsNullableRestrictedOwnership() throws Exception {
        String database = "class_year_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("55").load().migrate();
        jdbc.update("INSERT INTO classes(name, academic_year) VALUES ('Legacy 7-A', '2024-2025'), ('Unlabeled', NULL)");
        var before = jdbc.queryForList("SELECT * FROM classes ORDER BY id");
        var yearsBefore = jdbc.queryForList("SELECT * FROM academic_years ORDER BY id");

        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("56");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        var after = jdbc.queryForList("SELECT * FROM classes ORDER BY id");
        for (var row : after) {
            assertThat(row).containsEntry("academic_year_id", null);
            row.remove("academic_year_id");
        }
        assertThat(after).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT * FROM academic_years ORDER BY id")).isEqualTo(yearsBefore).isEmpty();
        assertThat(jdbc.queryForObject("SELECT character_maximum_length FROM information_schema.columns WHERE table_name = 'classes' AND column_name = 'academic_year'", Integer.class)).isEqualTo(255);
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'classes' AND column_name = 'academic_year_id'", String.class)).isEqualTo("YES");
        assertThat(jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'classes'::regclass AND confrelid = 'academic_years'::regclass", String.class))
                .contains("FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT");
        assertThat(jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'classes'", String.class))
                .anyMatch(index -> index.contains("(academic_year_id)"));

        Long year = jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) SELECT id, 'Configured test year', '2026-08-17', '2027-07-09' FROM schools RETURNING id", Long.class);
        Long clazz = jdbc.queryForObject("INSERT INTO classes(name, academic_year, academic_year_id) VALUES ('Canonical', 'Configured test year', ?) RETURNING id", Long.class, year);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM academic_years WHERE id = ?", year)).rootCause()
                .isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO classes(name, academic_year_id) VALUES ('Invalid owner', ?)", Long.MAX_VALUE)).rootCause()
                .isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
        jdbc.update("DELETE FROM classes WHERE id = ?", clazz);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM academic_years WHERE id = ?", Long.class, year)).isEqualTo(1);
    }
}
