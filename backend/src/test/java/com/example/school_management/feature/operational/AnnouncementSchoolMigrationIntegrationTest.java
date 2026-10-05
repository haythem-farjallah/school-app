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

import static org.assertj.core.api.Assertions.*;

@IntegrationTest
class AnnouncementSchoolMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void oneClassDeterminesOwner() throws Exception {
        Database db = database();
        long owner = school(db.jdbc);
        long announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        succeeds(db, announcement, owner);
    }

    @Test
    void agreeingClassesDetermineOwner() throws Exception {
        Database db = database();
        long owner = school(db.jdbc);
        long announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        succeeds(db, announcement, owner);
    }

    @Test
    void differentAnnouncementsHaveIndependentOwners() throws Exception {
        Database db = database();
        long a = school(db.jdbc), b = school(db.jdbc);
        long first = announcement(db.jdbc), second = announcement(db.jdbc);
        target(db.jdbc, first, clazz(db.jdbc, a));
        target(db.jdbc, second, clazz(db.jdbc, b));
        succeeds(db, first, a);
        assertThat(db.jdbc.queryForObject("SELECT school_id FROM announcements WHERE id = ?", Long.class, second)).isEqualTo(b);
    }

    @Test
    void conflictingClassesRollBackSchemaAndData() throws Exception {
        Database db = database();
        long announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, school(db.jdbc)));
        target(db.jdbc, announcement, clazz(db.jdbc, school(db.jdbc)));
        fails(db, announcement);
    }

    @Test
    void brokenClassOwnershipFails() throws Exception {
        Database db = database();
        long announcement = announcement(db.jdbc);
        db.jdbc.update("ALTER TABLE announcement_target_classes DROP CONSTRAINT fk_announcement_target_classes_class");
        target(db.jdbc, announcement, -1);
        fails(db, announcement);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void structuralOwnerAcceptsMatchingCreatorRole(String role) throws Exception {
        Database db = database();
        long owner = school(db.jdbc), announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        membership(db.jdbc, creator(db.jdbc, announcement, role), owner, role);
        succeeds(db, announcement, owner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void structuralOwnerAcceptsMultiSchoolCreator(String role) throws Exception {
        Database db = database();
        long owner = school(db.jdbc), announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        long creator = creator(db.jdbc, announcement, role);
        membership(db.jdbc, creator, owner, role);
        membership(db.jdbc, creator, school(db.jdbc), role);
        succeeds(db, announcement, owner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void structuralOwnerRejectsWrongSchoolCreatorHistory(String role) throws Exception {
        Database db = database();
        long announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, school(db.jdbc)));
        membership(db.jdbc, creator(db.jdbc, announcement, role), school(db.jdbc), role);
        fails(db, announcement);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void structuralOwnerRejectsWrongMembershipRole(String role) throws Exception {
        Database db = database();
        long owner = school(db.jdbc), announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        membership(db.jdbc, creator(db.jdbc, announcement, role), owner, "STUDENT");
        fails(db, announcement);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN", "STAFF"})
    void structuralOwnerAcceptsLegacyCreatorWithoutMembershipHistory(String role) throws Exception {
        Database db = database();
        long owner = school(db.jdbc), announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        creator(db.jdbc, announcement, role);
        succeeds(db, announcement, owner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void targetlessUsesUniqueApplicableCreatorMembership(String role) throws Exception {
        Database db = database();
        long owner = school(db.jdbc), announcement = announcement(db.jdbc);
        long creator = creator(db.jdbc, announcement, role);
        membership(db.jdbc, creator, owner, role);
        membership(db.jdbc, creator, school(db.jdbc), "STUDENT");
        succeeds(db, announcement, owner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void targetlessRejectsAmbiguousCreatorSchools(String role) throws Exception {
        Database db = database();
        long announcement = announcement(db.jdbc), creator = creator(db.jdbc, announcement, role);
        membership(db.jdbc, creator, school(db.jdbc), role);
        membership(db.jdbc, creator, school(db.jdbc), role);
        fails(db, announcement);
    }

    @Test
    void staffCreatorIsNotOwnershipEvidence() throws Exception {
        Database db = database();
        long announcement = announcement(db.jdbc);
        membership(db.jdbc, creator(db.jdbc, announcement, "STAFF"), school(db.jdbc), "ADMIN");
        fails(db, announcement);
    }

    @Test
    void staffPublisherIsNotOwnershipEvidence() throws Exception {
        Database db = database();
        long announcement = announcement(db.jdbc);
        long staff = db.jdbc.queryForObject("INSERT INTO users(role) VALUES ('STAFF') RETURNING id", Long.class);
        db.jdbc.update("INSERT INTO staff(id) VALUES (?)", staff);
        membership(db.jdbc, staff, school(db.jdbc), "ADMIN");
        db.jdbc.update("INSERT INTO staff_announcements(staff_id, announcement_id) VALUES (?, ?)", staff, announcement);
        fails(db, announcement);
    }

    @Test
    void singleSchoolFallbackSucceedsAndPreservesStaffLinks() throws Exception {
        Database db = database();
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long announcement = announcement(db.jdbc), staff = creator(db.jdbc, announcement, "STAFF");
        db.jdbc.update("INSERT INTO staff(id) VALUES (?)", staff);
        db.jdbc.update("INSERT INTO staff_announcements(staff_id, announcement_id) VALUES (?, ?)", staff, announcement);
        succeeds(db, announcement, owner);
    }

    @Test
    void multipleSchoolsWithoutEvidenceFail() throws Exception {
        Database db = database();
        school(db.jdbc);
        fails(db, announcement(db.jdbc));
    }

    @Test
    void zeroSchoolsWithoutEvidenceFail() throws Exception {
        Database db = database();
        db.jdbc.update("TRUNCATE schools CASCADE");
        fails(db, announcement(db.jdbc));
    }

    @Test
    void finalSchemaEnforcesOwnerAndRestrictsSchoolDeletion() throws Exception {
        Database db = database();
        long owner = school(db.jdbc), announcement = announcement(db.jdbc);
        target(db.jdbc, announcement, clazz(db.jdbc, owner));
        succeeds(db, announcement, owner);
        assertThat(db.jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'announcements' AND column_name = 'school_id'", String.class)).isEqualTo("NO");
        assertThat(db.jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'announcements'::regclass AND contype = 'f'", String.class))
                .anySatisfy(def -> assertThat(def).contains("FOREIGN KEY (school_id) REFERENCES schools(id)", "ON DELETE RESTRICT"));
        assertThat(db.jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'announcements'", String.class))
                .anySatisfy(def -> assertThat(def).contains("idx_announcements_school", "(school_id)"));
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO announcements(title, body) VALUES ('Invalid', 'Body')"))
                .rootCause().isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO announcements(title, body, school_id) VALUES ('Invalid', 'Body', -1)"))
                .rootCause().isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23503"));
        // Remove structural references so the Announcement FK alone proves restriction.
        db.jdbc.update("DELETE FROM announcement_target_classes WHERE announcement_id = ?", announcement);
        db.jdbc.update("DELETE FROM classes WHERE academic_year_id IN (SELECT id FROM academic_years WHERE school_id = ?)", owner);
        db.jdbc.update("DELETE FROM academic_years WHERE school_id = ?", owner);
        assertThatThrownBy(() -> db.jdbc.update("DELETE FROM schools WHERE id = ?", owner))
                .rootCause().isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23503"));
    }

    private void succeeds(Database db, long announcement, long owner) {
        var before = db.jdbc.queryForMap("SELECT * FROM announcements WHERE id = ?", announcement);
        var targets = db.jdbc.queryForList("SELECT * FROM announcement_target_classes ORDER BY announcement_id, class_id");
        var publishers = db.jdbc.queryForList("SELECT * FROM staff_announcements ORDER BY announcement_id, staff_id");
        flyway(db.url, "65").migrate();
        var after = db.jdbc.queryForMap("SELECT * FROM announcements WHERE id = ?", announcement);
        assertThat(after.remove("school_id")).isEqualTo(owner);
        assertThat(after).isEqualTo(before);
        assertThat(db.jdbc.queryForList("SELECT * FROM announcement_target_classes ORDER BY announcement_id, class_id")).isEqualTo(targets);
        assertThat(db.jdbc.queryForList("SELECT * FROM staff_announcements ORDER BY announcement_id, staff_id")).isEqualTo(publishers);
        flyway(db.url, "65").validate();
    }

    private void fails(Database db, long announcement) {
        var before = db.jdbc.queryForList("SELECT * FROM announcements ORDER BY id");
        var targets = db.jdbc.queryForList("SELECT * FROM announcement_target_classes ORDER BY announcement_id, class_id");
        var publishers = db.jdbc.queryForList("SELECT * FROM staff_announcements ORDER BY announcement_id, staff_id");
        assertThatThrownBy(() -> flyway(db.url, "65").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("Announcement School ownership").hasMessageContaining("announcement " + announcement);
        assertThat(db.jdbc.queryForList("SELECT * FROM announcements ORDER BY id")).isEqualTo(before);
        assertThat(db.jdbc.queryForList("SELECT * FROM announcement_target_classes ORDER BY announcement_id, class_id")).isEqualTo(targets);
        assertThat(db.jdbc.queryForList("SELECT * FROM staff_announcements ORDER BY announcement_id, staff_id")).isEqualTo(publishers);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'announcements' AND column_name = 'school_id'", Long.class)).isZero();
        assertThat(flyway(db.url, "64").info().current().getVersion().getVersion()).isEqualTo("64");
    }

    private Database database() throws Exception {
        String name = "announcement_school_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + name;
        flyway(url, "64").migrate();
        return new Database(url, new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword())));
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target(target).load();
    }

    private long school(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES (?) RETURNING id", Long.class, "School " + UUID.randomUUID());
    }

    private long announcement(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO announcements(title, body) VALUES ('Announcement', 'Body') RETURNING id", Long.class);
    }

    private long creator(JdbcTemplate jdbc, long announcement, String role) {
        long id = jdbc.queryForObject("INSERT INTO users(role) VALUES (?::user_role) RETURNING id", Long.class, role);
        jdbc.update("UPDATE announcements SET created_by_id = ? WHERE id = ?", id, announcement);
        return id;
    }

    private long clazz(JdbcTemplate jdbc, long school) {
        long year = jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, '2026-08-17', '2027-07-09') RETURNING id", Long.class, school, "Year " + UUID.randomUUID());
        return jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES ('Class', ?) RETURNING id", Long.class, year);
    }

    private void target(JdbcTemplate jdbc, long announcement, long clazz) {
        jdbc.update("INSERT INTO announcement_target_classes(announcement_id, class_id) VALUES (?, ?)", announcement, clazz);
    }

    private void membership(JdbcTemplate jdbc, long user, long school, String role) {
        long id = jdbc.queryForObject("INSERT INTO school_memberships(user_id, school_id, status, joined_at) VALUES (?, ?, 'INACTIVE', CURRENT_TIMESTAMP) RETURNING id", Long.class, user, school);
        jdbc.update("INSERT INTO school_membership_roles(membership_id, role) VALUES (?, ?)", id, role);
    }

    private record Database(String url, JdbcTemplate jdbc) {}
}
