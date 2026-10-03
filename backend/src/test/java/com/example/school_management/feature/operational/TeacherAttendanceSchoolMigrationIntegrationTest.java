package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class TeacherAttendanceSchoolMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void classDerivesOwnerFromAcademicYear() throws Exception {
        Database db = databaseAt62();
        long school = school(db.jdbc);
        long row = attendance(db.jdbc, teacher(db.jdbc), clazz(db.jdbc, school), null, null);
        migrateAndAssertOwner(db, row, school);
    }

    @Test
    void courseDerivesOwner() throws Exception {
        Database db = databaseAt62();
        long school = school(db.jdbc);
        long row = attendance(db.jdbc, teacher(db.jdbc), null, course(db.jdbc, school), null);
        migrateAndAssertOwner(db, row, school);
    }

    @Test
    void agreeingStructuralSourcesSupportMultiSchoolTeacherAndSubstitute() throws Exception {
        Database db = databaseAt62();
        long owner = school(db.jdbc);
        long other = school(db.jdbc);
        long teacher = teacher(db.jdbc);
        long substitute = teacher(db.jdbc);
        membership(db.jdbc, teacher, owner, "TEACHER");
        membership(db.jdbc, teacher, other, "TEACHER");
        membership(db.jdbc, substitute, owner, "TEACHER");
        membership(db.jdbc, substitute, other, "TEACHER");
        long row = attendance(db.jdbc, teacher, clazz(db.jdbc, owner), course(db.jdbc, owner), substitute);
        migrateAndAssertOwner(db, row, owner);
    }

    @Test
    void oneTeacherMembershipOwnsContextFreeHistory() throws Exception {
        Database db = databaseAt62();
        long owner = school(db.jdbc);
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, owner, "TEACHER");
        membership(db.jdbc, teacher, school(db.jdbc), "ADMIN");
        long row = attendance(db.jdbc, teacher, null, null, null);
        migrateAndAssertOwner(db, row, owner);
    }

    @Test
    void classCourseConflictFailsWithoutChangingRows() throws Exception {
        Database db = databaseAt62();
        long row = attendance(db.jdbc, teacher(db.jdbc), clazz(db.jdbc, school(db.jdbc)), course(db.jdbc, school(db.jdbc)), null);
        assertMigrationFails(db, row);
    }

    @Test
    void multipleTeacherMembershipsWithoutStructuralContextFail() throws Exception {
        Database db = databaseAt62();
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, school(db.jdbc), "TEACHER");
        membership(db.jdbc, teacher, school(db.jdbc), "TEACHER");
        assertMigrationFails(db, attendance(db.jdbc, teacher, null, null, null));
    }

    @Test
    void noMembershipHistoryUsesSingleSchoolLegacyFallback() throws Exception {
        Database db = databaseAt62();
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long row = attendance(db.jdbc, teacher(db.jdbc), null, null, null);
        migrateAndAssertOwner(db, row, owner);
    }

    @Test
    void noMembershipHistoryWithMultipleSchoolsFails() throws Exception {
        Database db = databaseAt62();
        school(db.jdbc);
        assertMigrationFails(db, attendance(db.jdbc, teacher(db.jdbc), null, null, null));
    }

    @Test
    void noMembershipHistoryWithZeroSchoolsFails() throws Exception {
        Database db = databaseAt62();
        db.jdbc.update("TRUNCATE schools CASCADE");
        assertMigrationFails(db, attendance(db.jdbc, teacher(db.jdbc), null, null, null));
    }

    @Test
    void membershipHistoryWithoutTeacherRoleCannotUseLegacyFallback() throws Exception {
        Database db = databaseAt62();
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, owner, "ADMIN");
        assertMigrationFails(db, attendance(db.jdbc, teacher, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "STUDENT"})
    void structuralOwnerRejectsIncompatibleTeacherMembership(String role) throws Exception {
        Database db = databaseAt62();
        long owner = school(db.jdbc);
        long teacher = teacher(db.jdbc);
        membership(db.jdbc, teacher, role.equals("TEACHER") ? school(db.jdbc) : owner, role);
        assertMigrationFails(db, attendance(db.jdbc, teacher, clazz(db.jdbc, owner), null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "STUDENT"})
    void substituteMembershipMustSupportOwnerTeacherRole(String role) throws Exception {
        Database db = databaseAt62();
        long owner = school(db.jdbc);
        long substitute = teacher(db.jdbc);
        membership(db.jdbc, substitute, role.equals("TEACHER") ? school(db.jdbc) : owner, role);
        assertMigrationFails(db, attendance(db.jdbc, teacher(db.jdbc), clazz(db.jdbc, owner), null, substitute));
    }

    @Test
    void substituteDoesNotProvideOwnershipEvidence() throws Exception {
        Database db = databaseAt62();
        long substitute = teacher(db.jdbc);
        membership(db.jdbc, substitute, school(db.jdbc), "TEACHER");
        assertMigrationFails(db, attendance(db.jdbc, teacher(db.jdbc), null, null, substitute));
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher_id", "class_id", "course_id", "substitute_teacher_id"})
    void missingCanonicalResourcesFailInsteadOfNullingIds(String column) throws Exception {
        Database db = databaseAt62();
        long owner = school(db.jdbc);
        long row = attendance(db.jdbc, teacher(db.jdbc), clazz(db.jdbc, owner), null, null);
        db.jdbc.update("UPDATE teacher_attendance SET " + column + " = -1 WHERE id = ?", row);
        assertMigrationFails(db, row);
    }

    @Test
    void baseAccountWithoutCanonicalTeacherCannotOwnAttendance() throws Exception {
        Database db = databaseAt62();
        long user = db.jdbc.queryForObject("INSERT INTO users(role) VALUES ('TEACHER') RETURNING id", Long.class);
        assertMigrationFails(db, attendance(db.jdbc, user, null, null, null));
    }

    @Test
    void schemaEnforcesSchoolAndScopedUniquenessAndIndexes() throws Exception {
        Database db = databaseAt62();
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long other = school(db.jdbc);
        long teacher = teacher(db.jdbc);
        long row = attendance(db.jdbc, teacher, clazz(db.jdbc, owner), null, null);
        migrateAndAssertOwner(db, row, owner);
        Flyway flyway = flyway(db.url, "63");
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("63");
        assertThat(db.jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'teacher_attendance' AND column_name = 'school_id'", String.class)).isEqualTo("NO");
        assertThat(db.jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'teacher_attendance'::regclass AND contype = 'f'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("FOREIGN KEY (school_id) REFERENCES schools(id)"));
        var indexes = db.jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'teacher_attendance'", String.class);
        assertThat(indexes).anySatisfy(definition -> assertThat(definition).contains("UNIQUE").contains("(school_id, teacher_id, date)"));
        assertThat(indexes).anySatisfy(definition -> assertThat(definition).contains("(school_id, date)"));
        assertThat(indexes).noneMatch(definition -> definition.contains("idx_teacher_attendance_unique "));
        assertThatThrownBy(() -> insertOwnedAttendance(db.jdbc, null, teacher)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> insertOwnedAttendance(db.jdbc, -1L, teacher)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
        assertThatThrownBy(() -> insertOwnedAttendance(db.jdbc, owner, teacher)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23505"));
        insertOwnedAttendance(db.jdbc, other, teacher);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM teacher_attendance WHERE teacher_id = ?", Long.class, teacher)).isEqualTo(2);
    }

    private void insertOwnedAttendance(JdbcTemplate jdbc, Long school, long teacher) {
        jdbc.update("INSERT INTO teacher_attendance(school_id, teacher_id, date, status, recorded_by_id, recorded_by_name) VALUES (?, ?, '2026-09-07', 'PRESENT', ?, 'Recorder')", school, teacher, teacher);
    }

    private void migrateAndAssertOwner(Database db, long row, long school) {
        var before = db.jdbc.queryForMap("SELECT * FROM teacher_attendance WHERE id = ?", row);
        flyway(db.url, "63").migrate();
        var after = db.jdbc.queryForMap("SELECT * FROM teacher_attendance WHERE id = ?", row);
        assertThat(after.remove("school_id")).isEqualTo(school);
        assertThat(after).isEqualTo(before);
    }

    private void assertMigrationFails(Database db, long row) {
        var before = db.jdbc.queryForList("SELECT * FROM teacher_attendance ORDER BY id");
        assertThatThrownBy(() -> flyway(db.url, "63").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("TeacherAttendance School ownership").hasMessageContaining("attendance " + row);
        assertThat(db.jdbc.queryForList("SELECT * FROM teacher_attendance ORDER BY id")).isEqualTo(before);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'teacher_attendance' AND column_name = 'school_id'", Long.class)).isZero();
    }

    private Database databaseAt62() throws Exception {
        String name = "teacher_attendance_school_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + name;
        flyway(url, "62").migrate();
        return new Database(url, new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword())));
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target(target).load();
    }

    private long school(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES (?) RETURNING id", Long.class, "School " + UUID.randomUUID());
    }

    private long teacher(JdbcTemplate jdbc) {
        long user = jdbc.queryForObject("INSERT INTO users(role) VALUES ('TEACHER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO teacher(id) VALUES (?)", user);
        return user;
    }

    private long clazz(JdbcTemplate jdbc, long school) {
        long year = jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, '2026-08-17', '2027-07-09') RETURNING id", Long.class, school, "Year " + UUID.randomUUID());
        return jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES ('Class A', ?) RETURNING id", Long.class, year);
    }

    private long course(JdbcTemplate jdbc, long school) {
        return jdbc.queryForObject("INSERT INTO courses(name, code, school_id) VALUES ('Math', 'MATH', ?) RETURNING id", Long.class, school);
    }

    private void membership(JdbcTemplate jdbc, long teacher, long school, String role) {
        long membership = jdbc.queryForObject("INSERT INTO school_memberships(user_id, school_id, status, joined_at) VALUES (?, ?, 'INACTIVE', CURRENT_TIMESTAMP) RETURNING id", Long.class, teacher, school);
        jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, ?)", membership, role);
    }

    private long attendance(JdbcTemplate jdbc, long teacher, Long clazz, Long course, Long substitute) {
        return jdbc.queryForObject("INSERT INTO teacher_attendance(teacher_id, class_id, course_id, substitute_teacher_id, date, status, recorded_by_id, recorded_by_name) VALUES (?, ?, ?, ?, '2026-09-07', 'PRESENT', ?, 'Recorder') RETURNING id", Long.class, teacher, clazz, course, substitute, teacher);
    }

    private record Database(String url, JdbcTemplate jdbc) {}
}
