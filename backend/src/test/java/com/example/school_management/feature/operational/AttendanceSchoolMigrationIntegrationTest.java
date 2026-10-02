package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
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
class AttendanceSchoolMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void classOwnedLegacyAttendanceGetsItsAcademicYearSchool() throws Exception {
        Database db = databaseAt60();
        long school = school(db.jdbc);
        long user = user(db.jdbc);
        membership(db.jdbc, user, school, "ACTIVE");
        membership(db.jdbc, user, school(db.jdbc), "ACTIVE");
        long attendance = attendance(db.jdbc, user, clazz(db.jdbc, school), null, null);

        migrateAndAssertSchool(db, attendance, school);
    }

    @Test
    void courseOwnedLegacyAttendanceGetsItsCourseSchool() throws Exception {
        Database db = databaseAt60();
        long school = school(db.jdbc);
        long attendance = attendance(db.jdbc, user(db.jdbc), null, course(db.jdbc, school), null);

        migrateAndAssertSchool(db, attendance, school);
    }

    @Test
    void slotOwnedLegacyAttendanceGetsItsPeriodSchoolAndPreservesRelationships() throws Exception {
        Database db = databaseAt60();
        long school = school(db.jdbc);
        long clazz = clazz(db.jdbc, school);
        long course = course(db.jdbc, school);
        long slot = slot(db.jdbc, school, clazz, course);
        long slotOnly = attendance(db.jdbc, user(db.jdbc), null, null, slot);
        long allContext = attendance(db.jdbc, user(db.jdbc), clazz, course, slot);
        var before = db.jdbc.queryForList("SELECT id, user_id, class_id, course_id, timetable_slot_id FROM attendance ORDER BY id");

        migrateAndAssertSchool(db, slotOnly, school);

        assertThat(owner(db.jdbc, allContext)).isEqualTo(school);
        assertThat(db.jdbc.queryForList("SELECT id, user_id, class_id, course_id, timetable_slot_id FROM attendance ORDER BY id"))
                .isEqualTo(before);
    }

    @Test
    void contextFreeLegacyAttendanceUsesExactlyOneMembershipRegardlessOfStatus() throws Exception {
        Database db = databaseAt60();
        long school = school(db.jdbc);
        for (String status : new String[] {"ACTIVE", "SUSPENDED", "INACTIVE"}) {
            long user = user(db.jdbc);
            membership(db.jdbc, user, school, status);
            attendance(db.jdbc, user, null, null, null);
        }

        flyway(db.url, "61").migrate();

        assertThat(db.jdbc.queryForList("SELECT school_id FROM attendance", Long.class)).containsOnly(school).hasSize(3);
    }

    @Test
    void conflictingClassAndCourseSchoolsStopTheMigrationWithoutChangingRows() throws Exception {
        Database db = databaseAt60();
        long a = school(db.jdbc);
        long b = school(db.jdbc);
        long attendance = attendance(db.jdbc, user(db.jdbc), clazz(db.jdbc, a), course(db.jdbc, b), null);

        assertMigrationFails(db, attendance);
    }

    @Test
    void conflictingSlotPeriodAndClassSchoolsStopTheMigration() throws Exception {
        Database db = databaseAt60();
        long a = school(db.jdbc);
        long b = school(db.jdbc);
        long attendance = attendance(db.jdbc, user(db.jdbc), null, null, slot(db.jdbc, a, clazz(db.jdbc, b), null));

        assertMigrationFails(db, attendance);
    }

    @Test
    void conflictingSlotPeriodAndCourseSchoolsStopTheMigration() throws Exception {
        Database db = databaseAt60();
        long a = school(db.jdbc);
        long b = school(db.jdbc);
        long attendance = attendance(db.jdbc, user(db.jdbc), null, null, slot(db.jdbc, a, null, course(db.jdbc, b)));

        assertMigrationFails(db, attendance);
    }

    @Test
    void conflictingDirectAndSlotSchoolsStopTheMigration() throws Exception {
        Database db = databaseAt60();
        long a = school(db.jdbc);
        long b = school(db.jdbc);
        long attendance = attendance(db.jdbc, user(db.jdbc), clazz(db.jdbc, a), null, slot(db.jdbc, b, null, null));

        assertMigrationFails(db, attendance);
    }

    @Test
    void multipleMembershipsCannotOwnContextFreeLegacyAttendance() throws Exception {
        Database db = databaseAt60();
        long user = user(db.jdbc);
        membership(db.jdbc, user, school(db.jdbc), "ACTIVE");
        membership(db.jdbc, user, school(db.jdbc), "INACTIVE");
        long attendance = attendance(db.jdbc, user, null, null, null);

        assertMigrationFails(db, attendance);
    }

    @Test
    void noMembershipCannotOwnContextFreeLegacyAttendance() throws Exception {
        Database db = databaseAt60();
        long attendance = attendance(db.jdbc, user(db.jdbc), null, null, null);

        assertMigrationFails(db, attendance);
    }

    @Test
    void freshSchemaSupportsUserOnlyAndClassOnlyAttendance() throws Exception {
        Database db = newDatabase();
        flyway(db.url, "61").migrate();
        long school = school(db.jdbc);
        long user = user(db.jdbc);
        long clazz = clazz(db.jdbc, school);
        db.jdbc.update("INSERT INTO attendance(school_id, user_id, date) VALUES (?, ?, '2026-09-21')", school, user);
        db.jdbc.update("INSERT INTO attendance(school_id, user_id, class_id, date) VALUES (?, ?, ?, '2026-09-22')", school, user, clazz);

        assertThat(db.jdbc.queryForList("SELECT school_id FROM attendance", Long.class)).containsExactly(school, school);
    }

    @Test
    void finalSchemaRequiresAnExistingSchoolAndDefinesScopedReadIndexes() throws Exception {
        Database db = newDatabase();
        Flyway flyway = flyway(db.url, "61");
        flyway.migrate();
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("61");
        assertThat(db.jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'attendance' AND column_name = 'school_id'", String.class))
                .isEqualTo("NO");
        assertThat(db.jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'attendance'::regclass AND contype = 'f'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("FOREIGN KEY (school_id) REFERENCES schools(id)"));
        assertThat(db.jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'attendance'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("(school_id, date)"))
                .anySatisfy(definition -> assertThat(definition).contains("(school_id, user_id, date)"))
                .anySatisfy(definition -> assertThat(definition).contains("(school_id, class_id, date)"))
                .anySatisfy(definition -> assertThat(definition).contains("(school_id, course_id, date)"))
                .anySatisfy(definition -> assertThat(definition).contains("(school_id, timetable_slot_id, date)"))
                .anySatisfy(definition -> assertThat(definition).contains("(school_id, user_type, date)"));
        long user = user(db.jdbc);
        long course = course(db.jdbc, school(db.jdbc));
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO attendance(user_id, course_id, date) VALUES (?, ?, '2026-09-21')", user, course))
                .rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO attendance(school_id, user_id, course_id, date) VALUES (-1, ?, ?, '2026-09-21')", user, course))
                .rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
    }

    private void migrateAndAssertSchool(Database db, long attendance, long school) {
        Flyway flyway = flyway(db.url, "61");
        flyway.migrate();
        flyway.validate();
        assertThat(owner(db.jdbc, attendance)).isEqualTo(school);
    }

    private long owner(JdbcTemplate jdbc, long attendance) {
        return jdbc.queryForObject("SELECT school_id FROM attendance WHERE id = ?", Long.class, attendance);
    }

    private void assertMigrationFails(Database db, long attendance) {
        var before = db.jdbc.queryForList("SELECT * FROM attendance ORDER BY id");
        assertThatThrownBy(() -> flyway(db.url, "61").migrate())
                .isInstanceOf(FlywayException.class).hasMessageContaining("Attendance School ownership")
                .hasMessageContaining("attendance " + attendance);
        assertThat(db.jdbc.queryForList("SELECT * FROM attendance ORDER BY id")).isEqualTo(before);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'attendance' AND column_name = 'school_id'", Long.class))
                .isZero();
    }

    private Database databaseAt60() throws Exception {
        Database db = newDatabase();
        flyway(db.url, "60").migrate();
        // Model the existing nullable Attendance.course relationship in legacy Hibernate-managed databases.
        db.jdbc.execute("ALTER TABLE attendance ALTER COLUMN course_id DROP NOT NULL");
        return db;
    }

    private Database newDatabase() throws Exception {
        String database = "attendance_school_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
        return new Database(url, jdbc);
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target(target).load();
    }

    private long school(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES (?) RETURNING id", Long.class, "School " + UUID.randomUUID());
    }

    private long clazz(JdbcTemplate jdbc, long school) {
        long year = jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, '2026-08-17', '2027-07-09') RETURNING id",
                Long.class, school, "Year " + UUID.randomUUID());
        return jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES ('Class A', ?) RETURNING id", Long.class, year);
    }

    private long course(JdbcTemplate jdbc, long school) {
        return jdbc.queryForObject("INSERT INTO courses(name, code, school_id) VALUES ('Math', 'MATH', ?) RETURNING id", Long.class, school);
    }

    private long slot(JdbcTemplate jdbc, long school, Long clazz, Long course) {
        long period = jdbc.queryForObject("INSERT INTO periods(school_id, index_number, start_time, end_time) VALUES (?, 1, '08:00', '09:00') RETURNING id", Long.class, school);
        return jdbc.queryForObject("INSERT INTO timetable_slots(day_of_week, period_id, for_class_id, for_course_id) VALUES ('MONDAY', ?, ?, ?) RETURNING id",
                Long.class, period, clazz, course);
    }

    private long user(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO users(role) VALUES ('STUDENT') RETURNING id", Long.class);
    }

    private void membership(JdbcTemplate jdbc, long user, long school, String status) {
        jdbc.update("INSERT INTO school_memberships(user_id, school_id, status, joined_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)", user, school, status);
    }

    private long attendance(JdbcTemplate jdbc, long user, Long clazz, Long course, Long slot) {
        return jdbc.queryForObject("INSERT INTO attendance(user_id, class_id, course_id, timetable_slot_id, date) VALUES (?, ?, ?, ?, '2026-09-21') RETURNING id",
                Long.class, user, clazz, course, slot);
    }

    private record Database(String url, JdbcTemplate jdbc) {}
}
