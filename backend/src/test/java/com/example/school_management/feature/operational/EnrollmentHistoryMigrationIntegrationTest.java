package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V59 turns Enrollment into immutable history. These tests run against real PostgreSQL databases created per test,
 * so each can stop Flyway at V58 or V59 and use independent connections.
 */
@IntegrationTest
class EnrollmentHistoryMigrationIntegrationTest {
    private static final String ONE_ACTIVE = "uk_enrollments_one_active_per_academic_year";

    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void v59UpgradeMapsLegacyStatusesCleansAmbiguousActiveRowsAndDropsTransfers() throws Exception {
        String url = newDatabaseUrl();
        JdbcTemplate jdbc = jdbc(url);
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("58").load().migrate();
        long school = school(jdbc);
        long year = year(jdbc, school, "2026");
        long otherYear = year(jdbc, school, "2027");

        long mapped = student(jdbc);
        long mappedActive = insert(jdbc, mapped, clazz(jdbc, year, "Active"), "ACTIVE");
        long mappedCompleted = insert(jdbc, mapped, clazz(jdbc, year, "Completed"), "COMPLETED");
        long mappedPending = insert(jdbc, mapped, clazz(jdbc, year, "Pending"), "PENDING");
        long mappedDropped = insert(jdbc, mapped, clazz(jdbc, year, "Dropped"), "DROPPED");
        long mappedSuspended = insert(jdbc, mapped, clazz(jdbc, year, "Suspended"), "SUSPENDED");

        long ambiguous = student(jdbc);
        long ambiguousOne = insert(jdbc, ambiguous, clazz(jdbc, year, "Ambiguous one"), "ACTIVE");
        long ambiguousTwo = insert(jdbc, ambiguous, clazz(jdbc, year, "Ambiguous two"), "ACTIVE");
        long otherYearActive = insert(jdbc, ambiguous, clazz(jdbc, otherYear, "Other year"), "ACTIVE");

        long withPending = student(jdbc);
        long keptActive = insert(jdbc, withPending, clazz(jdbc, year, "Kept active"), "ACTIVE");
        long becomesWithdrawn = insert(jdbc, withPending, clazz(jdbc, year, "Was pending"), "PENDING");

        jdbc.update("INSERT INTO transfers(student_id) VALUES (?)", mapped);

        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("59").load();
        flyway.migrate();
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("59");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(status(jdbc, mappedActive)).isEqualTo("ACTIVE");
        assertThat(status(jdbc, mappedCompleted)).isEqualTo("COMPLETED");
        assertThat(status(jdbc, mappedPending)).isEqualTo("WITHDRAWN");
        assertThat(status(jdbc, mappedDropped)).isEqualTo("WITHDRAWN");
        assertThat(status(jdbc, mappedSuspended)).isEqualTo("WITHDRAWN");

        assertThat(status(jdbc, ambiguousOne)).isEqualTo("WITHDRAWN");
        assertThat(status(jdbc, ambiguousTwo)).isEqualTo("WITHDRAWN");
        assertThat(status(jdbc, otherYearActive)).isEqualTo("ACTIVE");
        assertThat(status(jdbc, keptActive)).isEqualTo("ACTIVE");
        assertThat(status(jdbc, becomesWithdrawn)).isEqualTo("WITHDRAWN");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments", Long.class)).isEqualTo(10);

        assertThat(jdbc.queryForObject("SELECT to_regclass('transfers')", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_type WHERE typname = 'enrollment_status'", Long.class)).isZero();
    }

    @Test
    void cleanMigrationDefinesTheCanonicalStatusSchema() throws Exception {
        String url = newDatabaseUrl();
        JdbcTemplate jdbc = jdbc(url);
        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("59").load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("59");

        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_name = 'enrollments' AND column_name = 'status'", String.class))
                .isEqualTo("character varying");
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'enrollments' AND column_name = 'status'", String.class))
                .isEqualTo("NO");
        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name = 'enrollments'", String.class))
                .doesNotContain("academic_year_id", "school_id");
        assertThat(jdbc.queryForObject("SELECT to_regclass('transfers')", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT to_regclass('class_students')", String.class)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_type WHERE typname = 'enrollment_status'", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conrelid = 'enrollments'::regclass AND contype = 'u'", Long.class))
                .isZero();

        Fixture f = fixture(jdbc);
        long defaulted = jdbc.queryForObject("INSERT INTO enrollments(student_id, class_id) VALUES (?, ?) RETURNING id", Long.class, f.student, f.classes.get(0));
        assertThat(status(jdbc, defaulted)).isEqualTo("ACTIVE");

        for (String legacy : List.of("PENDING", "DROPPED", "SUSPENDED", "active", "")) {
            assertSqlState(() -> jdbc.update("INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, ?)",
                    f.student, f.classes.get(1), legacy), "23514");
        }
        assertSqlState(() -> jdbc.update("INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, NULL)",
                f.student, f.classes.get(1)), "23502");
        for (String supported : List.of("COMPLETED", "TRANSFERRED", "WITHDRAWN")) {
            jdbc.update("INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, ?)", f.student, f.classes.get(1), supported);
        }
    }

    @Test
    void aStudentMayHaveManyHistoricalEnrollmentsInTheSameClass() throws Exception {
        JdbcTemplate jdbc = migratedDatabase();
        Fixture f = fixture(jdbc);
        long a = f.classes.get(0);
        long b = f.classes.get(1);

        insert(jdbc, f.student, a, "TRANSFERRED");
        insert(jdbc, f.student, b, "TRANSFERRED");
        insert(jdbc, f.student, a, "ACTIVE");
        insert(jdbc, f.student, a, "WITHDRAWN");
        insert(jdbc, f.student, a, "COMPLETED");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments WHERE student_id = ? AND class_id = ?", Long.class, f.student, a))
                .isEqualTo(4);
    }

    @Test
    void aStudentHasOneActiveEnrollmentPerAcademicYear() throws Exception {
        JdbcTemplate jdbc = migratedDatabase();
        Fixture f = fixture(jdbc);
        long sameYearOne = f.classes.get(0);
        long sameYearTwo = f.classes.get(1);
        long otherYearClass = clazz(jdbc, f.otherYear, "Next year");

        long first = insert(jdbc, f.student, sameYearOne, "ACTIVE");
        assertSqlState(() -> insert(jdbc, f.student, sameYearTwo, "ACTIVE"), "23505", ONE_ACTIVE);
        assertSqlState(() -> insert(jdbc, f.student, sameYearOne, "ACTIVE"), "23505", ONE_ACTIVE);
        assertThat(activeCount(jdbc, f.student)).isEqualTo(1);

        insert(jdbc, f.student, otherYearClass, "ACTIVE");
        assertThat(activeCount(jdbc, f.student)).isEqualTo(2);

        jdbc.update("UPDATE enrollments SET status = 'WITHDRAWN' WHERE id = ?", first);
        insert(jdbc, f.student, sameYearTwo, "ACTIVE");
        assertThat(activeCount(jdbc, f.student)).isEqualTo(2);

        Fixture other = fixture(jdbc);
        insert(jdbc, other.student, other.classes.get(0), "ACTIVE");
        assertThat(activeCount(jdbc, other.student)).isEqualTo(1);
    }

    @Test
    void anActiveRowCanBeUpdatedAndEnded() throws Exception {
        JdbcTemplate jdbc = migratedDatabase();
        Fixture f = fixture(jdbc);
        long active = insert(jdbc, f.student, f.classes.get(0), "ACTIVE");

        jdbc.update("UPDATE enrollments SET final_grad = 14 WHERE id = ?", active);
        jdbc.update("UPDATE enrollments SET status = 'ACTIVE' WHERE id = ?", active);
        jdbc.update("UPDATE enrollments SET status = 'COMPLETED' WHERE id = ?", active);
        jdbc.update("UPDATE enrollments SET status = 'COMPLETED', final_grad = 15 WHERE id = ?", active);

        assertThat(status(jdbc, active)).isEqualTo("COMPLETED");
    }

    @Test
    void anEnrollmentCannotBeMovedToAnotherStudentOrClass() throws Exception {
        JdbcTemplate jdbc = migratedDatabase();
        Fixture f = fixture(jdbc);
        long otherStudent = student(jdbc);
        long active = insert(jdbc, f.student, f.classes.get(0), "ACTIVE");
        long terminal = insert(jdbc, f.student, f.classes.get(1), "WITHDRAWN");

        for (long id : List.of(active, terminal)) {
            assertSqlState(() -> jdbc.update("UPDATE enrollments SET class_id = ? WHERE id = ?", f.classes.get(2), id), "23514",
                    "ck_enrollments_immutable_relationships");
            assertSqlState(() -> jdbc.update("UPDATE enrollments SET student_id = ? WHERE id = ?", otherStudent, id), "23514",
                    "ck_enrollments_immutable_relationships");
        }
        assertThat(jdbc.queryForObject("SELECT class_id FROM enrollments WHERE id = ?", Long.class, active)).isEqualTo(f.classes.get(0));
        assertThat(jdbc.queryForObject("SELECT student_id FROM enrollments WHERE id = ?", Long.class, terminal)).isEqualTo(f.student);
    }

    @Test
    void aTerminalEnrollmentNeverChangesStatus() throws Exception {
        JdbcTemplate jdbc = migratedDatabase();
        Fixture f = fixture(jdbc);
        List<String> terminal = List.of("COMPLETED", "TRANSFERRED", "WITHDRAWN");

        for (String from : terminal) {
            long id = insert(jdbc, f.student, f.classes.get(terminal.indexOf(from)), from);
            for (String to : List.of("ACTIVE", "COMPLETED", "TRANSFERRED", "WITHDRAWN")) {
                if (to.equals(from)) {
                    jdbc.update("UPDATE enrollments SET status = ?, final_grad = 12 WHERE id = ?", to, id);
                } else {
                    assertSqlState(() -> jdbc.update("UPDATE enrollments SET status = ? WHERE id = ?", to, id), "23514",
                            "ck_enrollments_terminal_status");
                }
            }
            assertThat(status(jdbc, id)).isEqualTo(from);
        }
        assertThat(activeCount(jdbc, f.student)).isZero();
    }

    @Test
    void concurrentActiveEnrollmentsForTheSameStudentAndYearSerialize() throws Exception {
        String url = newDatabaseUrl();
        JdbcTemplate jdbc = jdbc(url);
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("59").load().migrate();
        Fixture f = fixture(jdbc);

        // Deterministic: the second writer must wait for the first transaction and then be rejected.
        try (Connection first = connection(url); Connection second = connection(url)) {
            insert(first, f.student, f.classes.get(0));
            CompletableFuture<Void> blocked = CompletableFuture.runAsync(() -> {
                try {
                    insert(second, f.student, f.classes.get(1));
                    second.commit();
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            assertThatThrownBy(() -> blocked.get(750, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            first.commit();
            assertThatThrownBy(() -> blocked.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .rootCause().isInstanceOfSatisfying(SQLException.class, e -> {
                        assertThat(e.getSQLState()).isEqualTo("23505");
                        assertThat(e.getMessage()).contains(ONE_ACTIVE);
                    });
            second.rollback();
        }
        assertThat(activeCount(jdbc, f.student)).isEqualTo(1);

        // Racing: many students, both writers released together; exactly one ACTIVE row survives for each.
        for (int round = 0; round < 15; round++) {
            long student = student(jdbc);
            CyclicBarrier start = new CyclicBarrier(2);
            List<CompletableFuture<Boolean>> writers = new ArrayList<>();
            for (long clazz : List.of(f.classes.get(0), f.classes.get(1))) {
                writers.add(CompletableFuture.supplyAsync(() -> {
                    try (Connection connection = connection(url)) {
                        start.await(10, TimeUnit.SECONDS);
                        insert(connection, student, clazz);
                        connection.commit();
                        return true;
                    } catch (SQLException e) {
                        assertThat(e.getSQLState()).isEqualTo("23505");
                        return false;
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }));
            }
            long succeeded = writers.stream().filter(w -> w.join()).count();
            assertThat(succeeded).as("round %d", round).isEqualTo(1);
            assertThat(activeCount(jdbc, student)).as("round %d", round).isEqualTo(1);
        }
    }

    @Test
    void concurrentTransfersAndEnrollmentsNeverLeaveTwoActiveRows() throws Exception {
        String url = newDatabaseUrl();
        JdbcTemplate jdbc = jdbc(url);
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("59").load().migrate();
        Fixture f = fixture(jdbc);
        long source = insert(jdbc, f.student, f.classes.get(0), "ACTIVE");

        // A transfer ends the old row and inserts the new one in one transaction; a rival insert must wait for it.
        try (Connection transfer = connection(url); Connection rival = connection(url)) {
            try (PreparedStatement end = transfer.prepareStatement("UPDATE enrollments SET status = 'TRANSFERRED' WHERE id = ?")) {
                end.setLong(1, source);
                end.executeUpdate();
            }
            insert(transfer, f.student, f.classes.get(1));
            CompletableFuture<Void> rivalInsert = CompletableFuture.runAsync(() -> {
                try {
                    insert(rival, f.student, f.classes.get(2));
                    rival.commit();
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            assertThatThrownBy(() -> rivalInsert.get(750, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            transfer.commit();
            assertThatThrownBy(() -> rivalInsert.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .rootCause().isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23505"));
            rival.rollback();
        }

        assertThat(status(jdbc, source)).isEqualTo("TRANSFERRED");
        assertThat(jdbc.queryForObject("SELECT class_id FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, f.student))
                .isEqualTo(f.classes.get(1));
    }

    // ---- fixtures ---------------------------------------------------------------------------------------

    private JdbcTemplate migratedDatabase() throws Exception {
        String url = newDatabaseUrl();
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("59").load().migrate();
        return jdbc(url);
    }

    private record Fixture(long student, long year, long otherYear, List<Long> classes) {}

    /** A student and three classes of one academic year, plus a second academic year. */
    private Fixture fixture(JdbcTemplate jdbc) {
        long school = school(jdbc);
        long year = year(jdbc, school, "Year " + UUID.randomUUID());
        long otherYear = year(jdbc, school, "Year " + UUID.randomUUID());
        return new Fixture(student(jdbc), year, otherYear,
                List.of(clazz(jdbc, year, "One"), clazz(jdbc, year, "Two"), clazz(jdbc, year, "Three")));
    }

    private long school(JdbcTemplate jdbc) {
        Long existing = jdbc.queryForObject("SELECT min(id) FROM schools", Long.class);
        return existing != null ? existing : jdbc.queryForObject("INSERT INTO schools(name) VALUES ('Test School') RETURNING id", Long.class);
    }

    private long year(JdbcTemplate jdbc, long school, String name) {
        return jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, '2026-08-17', '2027-07-09') RETURNING id",
                Long.class, school, name);
    }

    private long clazz(JdbcTemplate jdbc, long year, String name) {
        return jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES (?, ?) RETURNING id", Long.class, name + " " + UUID.randomUUID(), year);
    }

    private long student(JdbcTemplate jdbc) {
        long id = jdbc.queryForObject("INSERT INTO users(role) VALUES ('STUDENT') RETURNING id", Long.class);
        jdbc.update("INSERT INTO student(id) VALUES (?)", id);
        return id;
    }

    private long insert(JdbcTemplate jdbc, long student, long clazz, String status) {
        return jdbc.queryForObject("INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, ?) RETURNING id", Long.class, student, clazz, status);
    }

    private void insert(Connection connection, long student, long clazz) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO enrollments(student_id, class_id, status) VALUES (?, ?, 'ACTIVE')")) {
            statement.setLong(1, student);
            statement.setLong(2, clazz);
            statement.executeUpdate();
        }
    }

    private String status(JdbcTemplate jdbc, long enrollment) {
        return jdbc.queryForObject("SELECT status FROM enrollments WHERE id = ?", String.class, enrollment);
    }

    private long activeCount(JdbcTemplate jdbc, long student) {
        return jdbc.queryForObject("SELECT count(*) FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, student);
    }

    private String newDatabaseUrl() throws Exception {
        String database = "enrollment_history_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }

    private JdbcTemplate jdbc(String url) {
        return new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
    }

    private Connection connection(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
        connection.setAutoCommit(false);
        return connection;
    }

    private void assertSqlState(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String state, String... messageParts) {
        assertThatThrownBy(operation).rootCause().isInstanceOfSatisfying(SQLException.class, error -> {
            assertThat(error.getSQLState()).isEqualTo(state);
            for (String part : messageParts) {
                assertThat(error.getMessage()).contains(part);
            }
        });
    }
}
