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
class TimetableSchoolMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void classOwnedTimetableGetsItsAcademicYearSchool() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz(db.jdbc, school));

        migrateAndAssertSchool(db, timetable, school);
    }

    @Test
    void roomOwnedTimetableGetsItsRoomSchool() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_rooms VALUES (?, ?)", timetable, room(db.jdbc, school));

        migrateAndAssertSchool(db, timetable, school);
    }

    @Test
    void slotOwnedTimetableGetsItsPeriodSchool() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        slot(db.jdbc, timetable, school, null, null, null);

        migrateAndAssertSchool(db, timetable, school);
    }

    @Test
    void agreeingSourcesPreserveEveryAssociationAndSlotReference() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        long clazz = clazz(db.jdbc, school);
        long room = room(db.jdbc, school);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz);
        db.jdbc.update("INSERT INTO timetable_rooms VALUES (?, ?)", timetable, room);
        long teacher = teacher(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_teachers VALUES (?, ?)", timetable, teacher);
        slot(db.jdbc, timetable, school, clazz, course(db.jdbc, school), room);
        var classes = db.jdbc.queryForList("SELECT * FROM timetable_classes");
        var rooms = db.jdbc.queryForList("SELECT * FROM timetable_rooms");
        var teachers = db.jdbc.queryForList("SELECT * FROM timetable_teachers");
        var slots = db.jdbc.queryForList("SELECT * FROM timetable_slots");

        migrateAndAssertSchool(db, timetable, school);

        assertThat(db.jdbc.queryForList("SELECT * FROM timetable_classes")).isEqualTo(classes);
        assertThat(db.jdbc.queryForList("SELECT * FROM timetable_rooms")).isEqualTo(rooms);
        assertThat(db.jdbc.queryForList("SELECT * FROM timetable_teachers")).isEqualTo(teachers);
        assertThat(db.jdbc.queryForList("SELECT * FROM timetable_slots")).isEqualTo(slots);
    }

    @Test
    void conflictingClassAndRoomSchoolsStopMigrationWithoutChangingRows() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz(db.jdbc, school(db.jdbc)));
        db.jdbc.update("INSERT INTO timetable_rooms VALUES (?, ?)", timetable, room(db.jdbc, school(db.jdbc)));

        assertMigrationFails(db, "timetable " + timetable);
    }

    @Test
    void conflictingPeriodAndSlotClassSchoolsStopMigration() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        long slot = slot(db.jdbc, timetable, school(db.jdbc), clazz(db.jdbc, school(db.jdbc)), null, null);

        assertMigrationFails(db, "slot " + slot);
    }

    @Test
    void conflictingPeriodAndSlotCourseSchoolsStopMigration() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        long slot = slot(db.jdbc, timetable, school(db.jdbc), null, course(db.jdbc, school(db.jdbc)), null);

        assertMigrationFails(db, "slot " + slot);
    }

    @Test
    void conflictingPeriodAndSlotRoomSchoolsStopMigration() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        long slot = slot(db.jdbc, timetable, school(db.jdbc), null, null, room(db.jdbc, school(db.jdbc)));

        assertMigrationFails(db, "slot " + slot);
    }

    @Test
    void conflictingTimetableClassAndSlotPeriodSchoolsStopMigration() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz(db.jdbc, school(db.jdbc)));
        slot(db.jdbc, timetable, school(db.jdbc), null, null, null);

        assertMigrationFails(db, "timetable " + timetable);
    }

    @Test
    void emptyTimetableUsesOnlySchoolWithoutReadingFreeTextLabels() throws Exception {
        Database db = databaseAt61();
        long school = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long timetable = timetable(db.jdbc);

        migrateAndAssertSchool(db, timetable, school);
    }

    @Test
    void emptyTimetableWithMultipleSchoolsStopsMigrationDespiteTeacherMembership() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, school, "TEACHER");
        db.jdbc.update("INSERT INTO timetable_teachers VALUES (?, ?)", timetable, teacher);

        assertMigrationFails(db, "timetable " + timetable);
    }

    @Test
    void multiSchoolTeacherIsCompatibleWhenOwnerMembershipContainsTeacherRole() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz(db.jdbc, school));
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, school, "TEACHER");
        membership(db.jdbc, teacher, school(db.jdbc), "TEACHER");
        db.jdbc.update("INSERT INTO timetable_teachers VALUES (?, ?)", timetable, teacher);

        migrateAndAssertSchool(db, timetable, school);
    }

    @Test
    void teacherWithOnlyForeignMembershipStopsMigration() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz(db.jdbc, school(db.jdbc)));
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, school(db.jdbc), "TEACHER");
        db.jdbc.update("INSERT INTO timetable_teachers VALUES (?, ?)", timetable, teacher);

        assertMigrationFails(db, "timetable " + timetable);
    }

    @Test
    void ownerMembershipWithoutTeacherRoleStopsMigration() throws Exception {
        Database db = databaseAt61();
        long school = school(db.jdbc);
        long timetable = timetable(db.jdbc);
        db.jdbc.update("INSERT INTO timetable_classes VALUES (?, ?)", timetable, clazz(db.jdbc, school));
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, school, "STUDENT");
        db.jdbc.update("INSERT INTO timetable_teachers VALUES (?, ?)", timetable, teacher);

        assertMigrationFails(db, "timetable " + timetable);
    }

    @Test
    void slotLinkedTeacherMustBeCompatibleWithPeriodSchoolWhenMembershipExists() throws Exception {
        Database db = databaseAt61();
        long timetable = timetable(db.jdbc);
        long slot = slot(db.jdbc, timetable, school(db.jdbc), null, null, null);
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, school(db.jdbc), "TEACHER");
        db.jdbc.update("UPDATE timetable_slots SET teacher_id = ? WHERE id = ?", teacher, slot);

        assertMigrationFails(db, "slot " + slot);
    }

    @Test
    void finalSchemaRequiresExistingSchoolAndDefinesScopedIndexes() throws Exception {
        Database db = databaseAt61();
        Flyway flyway = flyway(db.url, "62");
        flyway.migrate();
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("62");
        assertThat(db.jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'timetables' AND column_name = 'school_id'", String.class))
                .isEqualTo("NO");
        assertThat(db.jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'timetables'::regclass AND contype = 'f'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("FOREIGN KEY (school_id) REFERENCES schools(id)"));
        assertThat(db.jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'timetables'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("idx_timetables_school").contains("(school_id"));
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO timetables(name) VALUES ('Missing School')"))
                .rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO timetables(name, school_id) VALUES ('Foreign School', -1)"))
                .rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
    }

    private void migrateAndAssertSchool(Database db, long timetable, long school) {
        Flyway flyway = flyway(db.url, "62");
        flyway.migrate();
        flyway.validate();
        assertThat(db.jdbc.queryForObject("SELECT school_id FROM timetables WHERE id = ?", Long.class, timetable)).isEqualTo(school);
    }

    private void assertMigrationFails(Database db, String resource) {
        var before = db.jdbc.queryForList("SELECT * FROM timetables ORDER BY id");
        var slots = db.jdbc.queryForList("SELECT * FROM timetable_slots ORDER BY id");
        assertThatThrownBy(() -> flyway(db.url, "62").migrate())
                .isInstanceOf(FlywayException.class).hasMessageContaining("Timetable School ownership").hasMessageContaining(resource);
        assertThat(db.jdbc.queryForList("SELECT * FROM timetables ORDER BY id")).isEqualTo(before);
        assertThat(db.jdbc.queryForList("SELECT * FROM timetable_slots ORDER BY id")).isEqualTo(slots);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'timetables' AND column_name = 'school_id'", Long.class))
                .isZero();
    }

    private Database databaseAt61() throws Exception {
        String database = "timetable_school_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
        flyway(url, "61").migrate();
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

    private long room(JdbcTemplate jdbc, long school) {
        return jdbc.queryForObject("INSERT INTO rooms(name, room_type, school_id) VALUES ('Room A', 'CLASSROOM', ?) RETURNING id", Long.class, school);
    }

    private long slot(JdbcTemplate jdbc, long timetable, long school, Long clazz, Long course, Long room) {
        long period = jdbc.queryForObject("INSERT INTO periods(school_id, index_number, start_time, end_time) VALUES (?, 1, '08:00', '09:00') RETURNING id", Long.class, school);
        return jdbc.queryForObject("INSERT INTO timetable_slots(timetable_id, day_of_week, period_id, for_class_id, for_course_id, room_id) VALUES (?, 'MONDAY', ?, ?, ?, ?) RETURNING id",
                Long.class, timetable, period, clazz, course, room);
    }

    private long teacher(JdbcTemplate jdbc) {
        long user = jdbc.queryForObject("INSERT INTO users(role) VALUES ('TEACHER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO teacher(id) VALUES (?)", user);
        return user;
    }

    private void membership(JdbcTemplate jdbc, long teacher, long school, String role) {
        long membership = jdbc.queryForObject("INSERT INTO school_memberships(user_id, school_id, status, joined_at) VALUES (?, ?, 'INACTIVE', CURRENT_TIMESTAMP) RETURNING id",
                Long.class, teacher, school);
        jdbc.update("INSERT INTO school_membership_roles VALUES (?, ?)", membership, role);
    }

    private long timetable(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO timetables(name, academic_year, semester) VALUES ('Legacy Timetable', 'free text', 'free text') RETURNING id", Long.class);
    }

    private record Database(String url, JdbcTemplate jdbc) {}
}
