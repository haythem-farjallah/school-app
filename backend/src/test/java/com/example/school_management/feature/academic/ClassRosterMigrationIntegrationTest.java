package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V60 drops class_students, the duplicate roster. A legacy link is only redundant when an ACTIVE Enrollment for
 * the same student and class exists; anything else is inconsistent data that must stop the migration.
 */
@IntegrationTest
class ClassRosterMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void consistentLegacyLinksAreDroppedWithTheTableAndEnrollmentHistoryIsUntouched() throws Exception {
        String url = databaseAt59();
        JdbcTemplate jdbc = jdbc(url);
        long year = year(jdbc);
        long classA = clazz(jdbc, year, "A");
        long classB = clazz(jdbc, year, "B");
        long student = student(jdbc);
        long other = student(jdbc);
        jdbc.update("INSERT INTO class_students(class_id, student_id) VALUES (?, ?)", classA, student);
        long active = enrollment(jdbc, student, classA, "ACTIVE");
        long history = enrollment(jdbc, other, classB, "WITHDRAWN");
        var before = jdbc.queryForList("SELECT * FROM enrollments ORDER BY id");

        Flyway flyway = flyway(url);
        flyway.migrate();
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("60");
        assertThat(jdbc.queryForObject("SELECT to_regclass('class_students')", String.class)).isNull();
        assertThat(jdbc.queryForList("SELECT * FROM enrollments ORDER BY id")).isEqualTo(before);
        assertThat(active).isNotEqualTo(history);
    }

    @Test
    void aLegacyOnlyMembershipStopsTheMigration() throws Exception {
        String url = databaseAt59();
        JdbcTemplate jdbc = jdbc(url);
        long clazz = clazz(jdbc, year(jdbc), "A");
        long student = student(jdbc);
        jdbc.update("INSERT INTO class_students(class_id, student_id) VALUES (?, ?)", clazz, student);

        assertMigrationFails(url, jdbc, clazz, student);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments", Long.class)).isZero();
    }

    @Test
    void aHistoricalEnrollmentDoesNotJustifyALegacyMembership() throws Exception {
        for (String terminal : new String[] {"WITHDRAWN", "COMPLETED", "TRANSFERRED"}) {
            String url = databaseAt59();
            JdbcTemplate jdbc = jdbc(url);
            long clazz = clazz(jdbc, year(jdbc), "A");
            long student = student(jdbc);
            jdbc.update("INSERT INTO class_students(class_id, student_id) VALUES (?, ?)", clazz, student);
            long enrollment = enrollment(jdbc, student, clazz, terminal);

            assertMigrationFails(url, jdbc, clazz, student);
            assertThat(jdbc.queryForObject("SELECT status FROM enrollments WHERE id = ?", String.class, enrollment))
                    .as(terminal).isEqualTo(terminal);
        }
    }

    @Test
    void anActiveEnrollmentInAnotherClassDoesNotJustifyTheLink() throws Exception {
        String url = databaseAt59();
        JdbcTemplate jdbc = jdbc(url);
        long year = year(jdbc);
        long linked = clazz(jdbc, year, "A");
        long enrolledIn = clazz(jdbc, year, "B");
        long student = student(jdbc);
        jdbc.update("INSERT INTO class_students(class_id, student_id) VALUES (?, ?)", linked, student);
        enrollment(jdbc, student, enrolledIn, "ACTIVE");

        assertMigrationFails(url, jdbc, linked, student);
    }

    @Test
    void cleanMigrationHasNoRosterTableAndKeepsTheEnrollmentInvariants() throws Exception {
        String url = newDatabaseUrl();
        Flyway flyway = flyway(url);
        flyway.migrate();
        flyway.validate();
        JdbcTemplate jdbc = jdbc(url);

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("60");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(jdbc.queryForObject("SELECT to_regclass('class_students')", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname = 'trg_enrollments_history_invariants' AND NOT tgisinternal", Long.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'enrollments_status_check'", String.class))
                .contains("ACTIVE", "COMPLETED", "TRANSFERRED", "WITHDRAWN");
    }

    private void assertMigrationFails(String url, JdbcTemplate jdbc, long clazz, long student) {
        Flyway flyway = flyway(url);
        assertThatThrownBy(flyway::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("class_students")
                .hasMessageContaining("ACTIVE enrollment")
                .hasMessageContaining("class " + clazz + ", student " + student);
        assertThat(jdbc.queryForObject("SELECT to_regclass('class_students')", String.class)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_students WHERE class_id = ? AND student_id = ?", Long.class, clazz, student))
                .isEqualTo(1);
    }

    private String databaseAt59() throws Exception {
        String url = newDatabaseUrl();
        flyway(url, "59").migrate();
        return url;
    }

    private Flyway flyway(String url) {
        return flyway(url, "60");
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target(target).load();
    }

    private JdbcTemplate jdbc(String url) {
        return new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
    }

    private long year(JdbcTemplate jdbc) {
        Long existing = jdbc.queryForObject("SELECT min(id) FROM schools", Long.class);
        long school = existing != null ? existing : jdbc.queryForObject("INSERT INTO schools(name) VALUES ('Test School') RETURNING id", Long.class);
        return jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, '2026-08-17', '2027-07-09') RETURNING id",
                Long.class, school, "Year " + UUID.randomUUID());
    }

    private long clazz(JdbcTemplate jdbc, long year, String name) {
        return jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES (?, ?) RETURNING id", Long.class, name + " " + UUID.randomUUID(), year);
    }

    private long student(JdbcTemplate jdbc) {
        long id = jdbc.queryForObject("INSERT INTO users(role) VALUES ('STUDENT') RETURNING id", Long.class);
        jdbc.update("INSERT INTO student(id) VALUES (?)", id);
        return id;
    }

    private long enrollment(JdbcTemplate jdbc, long student, long clazz, String status) {
        return jdbc.queryForObject("INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, ?) RETURNING id", Long.class, student, clazz, status);
    }

    private String newDatabaseUrl() throws Exception {
        String database = "class_roster_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }
}
