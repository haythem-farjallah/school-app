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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@IntegrationTest
class ClassAcademicYearMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void v56UpgradeRemovesOnlyLegacyClassDependenciesAndEnforcesCanonicalOwnership() throws Exception {
        String url = newDatabaseUrl();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
        Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target("56").load().migrate();
        long year = jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) SELECT id, 'Configured', '2026-08-17', '2027-07-09' FROM schools RETURNING id", Long.class);
        long legacy = jdbc.queryForObject("INSERT INTO classes(name, academic_year) VALUES ('Legacy 7-A', 'unparseable demo year') RETURNING id", Long.class);
        long canonical = jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES ('7-A', ?) RETURNING id", Long.class, year);
        long student = jdbc.queryForObject("INSERT INTO users(role) VALUES ('STUDENT') RETURNING id", Long.class);
        jdbc.update("INSERT INTO student(id) VALUES (?)", student);
        long teacher = jdbc.queryForObject("SELECT min(id) FROM teacher", Long.class);
        long course = jdbc.queryForObject("SELECT min(id) FROM courses", Long.class);
        long period = jdbc.queryForObject("SELECT min(id) FROM periods", Long.class);
        long resource = jdbc.queryForObject("INSERT INTO learning_resources(title,url,type) VALUES ('Shared', 'https://school.test/resource', 'DOCUMENT') RETURNING id", Long.class);
        long timetable = jdbc.queryForObject("INSERT INTO timetables(name) VALUES ('Shared') RETURNING id", Long.class);
        long announcement = jdbc.queryForObject("INSERT INTO announcements(title,body) VALUES ('Shared','Body') RETURNING id", Long.class);
        for (long clazz : List.of(legacy, canonical)) {
            jdbc.update("INSERT INTO class_students(class_id,student_id) VALUES (?,?)", clazz, student);
            jdbc.update("INSERT INTO class_courses(class_id,course_id) VALUES (?,?)", clazz, course);
            jdbc.update("INSERT INTO class_teachers(class_id,teacher_id) VALUES (?,?)", clazz, teacher);
            jdbc.update("INSERT INTO teaching_assignments(class_id,course_id,teacher_id) VALUES (?,?,?)", clazz, course, teacher);
            long enrollment = jdbc.queryForObject("INSERT INTO enrollments(class_id,student_id) VALUES (?,?) RETURNING id", Long.class, clazz, student);
            jdbc.update("INSERT INTO grades(enrollment_id,assigned_by_id,score) VALUES (?,?,12)", enrollment, teacher);
            jdbc.update("INSERT INTO notes(class_id,course_id,teacher_id,student_id) VALUES (?,?,?,?)", clazz, course, teacher, student);
            long oldResource = jdbc.queryForObject("INSERT INTO resources(class_id,title) VALUES (?, 'Class resource') RETURNING id", Long.class, clazz);
            jdbc.update("INSERT INTO resource_allowed_roles(resource_id,role) VALUES (?, 'STUDENT')", oldResource);
            jdbc.update("INSERT INTO resource_comments(on_resource_id,commented_by_id,content) VALUES (?,?,'Comment')", oldResource, student);
            long slot = jdbc.queryForObject("INSERT INTO timetable_slots(for_class_id,period_id,day_of_week) VALUES (?,?,'MONDAY') RETURNING id", Long.class, clazz, period);
            jdbc.update("INSERT INTO attendance(class_id,timetable_slot_id,user_id,course_id) VALUES (?,?,?,?)", clazz, slot, student, course);
            jdbc.update("INSERT INTO learning_resource_classes(class_id,resource_id) VALUES (?,?)", clazz, resource);
            jdbc.update("INSERT INTO timetable_classes(class_id,timetable_id) VALUES (?,?)", clazz, timetable);
            jdbc.update("INSERT INTO announcement_target_classes(class_id,announcement_id) VALUES (?,?)", clazz, announcement);
        }
        var yearsBefore = jdbc.queryForList("SELECT * FROM academic_years ORDER BY id");
        var canonicalBefore = jdbc.queryForMap("SELECT * FROM classes WHERE id = ?", canonical);
        canonicalBefore.remove("academic_year");
        Map<String, List<Map<String, Object>>> preserved = new java.util.LinkedHashMap<>();
        for (String table : List.of("users", "student", "teacher", "courses", "rooms", "periods", "schools", "terms", "schedules", "schedule_courses", "learning_resources", "timetables", "announcements")) {
            preserved.put(table, jdbc.queryForList("SELECT * FROM " + table));
        }
        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("57");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM classes WHERE id = ?", Long.class, legacy)).isZero();
        assertThat(jdbc.queryForMap("SELECT * FROM classes WHERE id = ?", canonical)).isEqualTo(canonicalBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM classes WHERE academic_year_id IS NULL", Long.class)).isZero();
        for (String table : List.of("class_students", "class_courses", "class_teachers", "teaching_assignments", "enrollments", "notes", "resources", "learning_resource_classes", "timetable_classes", "announcement_target_classes")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE class_id = ?", Long.class, legacy)).as(table).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE class_id = ?", Long.class, canonical)).as(table).isEqualTo(1);
        }
        for (String table : List.of("grades", "resource_allowed_roles", "resource_comments")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).as(table).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM timetable_slots WHERE for_class_id = ?", Long.class, canonical)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM timetable_slots WHERE for_class_id = ?", Long.class, legacy)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance WHERE class_id IS NULL AND timetable_slot_id IS NULL", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance WHERE class_id = ? AND timetable_slot_id IS NOT NULL", Long.class, canonical)).isEqualTo(1);
        preserved.forEach((table, rows) -> assertThat(jdbc.queryForList("SELECT * FROM " + table)).as(table).containsExactlyInAnyOrderElementsOf(rows));
        assertThat(jdbc.queryForList("SELECT * FROM academic_years ORDER BY id")).isEqualTo(yearsBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'classes' AND column_name = 'academic_year'", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'classes' AND column_name = 'academic_year_id'", String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'classes'::regclass AND confrelid = 'academic_years'::regclass", String.class))
                .contains("FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT");
        assertThat(jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'classes'", String.class))
                .anyMatch(index -> index.contains("(academic_year_id)"))
                .anyMatch(index -> index.contains("UNIQUE INDEX") && index.contains("academic_year_id, lower((name)::text)"));
        assertSqlState(() -> jdbc.update("INSERT INTO classes(name) VALUES ('Unowned')"), "23502");
        assertSqlState(() -> jdbc.update("DELETE FROM academic_years WHERE id = ?", year), "23503");
        assertSqlState(() -> jdbc.update("INSERT INTO classes(name, academic_year_id) VALUES ('Invalid', ?)", Long.MAX_VALUE), "23503");
        assertSqlState(() -> jdbc.update("INSERT INTO classes(name, academic_year_id) VALUES ('7-a', ?)", year), "23505");
        assertSqlState(() -> jdbc.update("INSERT INTO classes(name, academic_year_id) VALUES ('7-A', ?)", year), "23505");
        long otherYear = jdbc.queryForObject("INSERT INTO academic_years(school_id,name,start_date,end_date) SELECT id,'Other','2027-08-17','2028-07-09' FROM schools RETURNING id", Long.class);
        jdbc.update("INSERT INTO classes(name,academic_year_id) VALUES ('7-A', ?)", otherYear);
        jdbc.update("INSERT INTO classes(name,academic_year_id) VALUES (NULL, ?), (NULL, ?)", year, year);
        jdbc.update("DELETE FROM classes WHERE academic_year_id = ?", otherYear);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM academic_years WHERE id = ?", Long.class, otherYear)).isEqualTo(1);
    }

    @Test
    void cleanMigrationValidatesFinalSchemaWithoutInventingYears() throws Exception {
        String url = newDatabaseUrl();
        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).load();
        flyway.migrate();
        flyway.validate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("57");
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM classes", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM academic_years", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'classes' AND column_name = 'academic_year_id'", String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'classes' AND column_name = 'academic_year'", Long.class)).isZero();
    }

    private String newDatabaseUrl() throws Exception {
        String database = "class_year_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }

    private void assertSqlState(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String state) {
        assertThatThrownBy(operation).rootCause().isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getSQLState()).isEqualTo(state));
    }
}
