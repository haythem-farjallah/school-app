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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class ObsoleteResourceSchemaMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void freshSchemaMigratesThroughV66WithCanonicalResourceForeignKeys() throws Exception {
        Database db = emptyDatabase();
        Flyway flyway = flyway(db.url, "latest");
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("66");
        assertCanonicalSchema(db.jdbc);
    }

    @Test
    void upgradeDropsOnlyObsoleteTablesAndPreservesCanonicalResourceAggregate() throws Exception {
        Database db = emptyDatabase();
        flyway(db.url, "65").migrate();
        assertThat(db.jdbc.queryForList("SELECT conrelid::regclass::text FROM pg_constraint WHERE contype = 'f' AND confrelid = 'resources'::regclass", String.class))
                .containsExactly("resource_allowed_roles");
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long canonical = db.jdbc.queryForObject("INSERT INTO learning_resources(title, url, type, school_id) VALUES ('Canonical', 'https://example.test/canonical', 'LINK', ?) RETURNING id", Long.class, owner);
        long author = db.jdbc.queryForObject("INSERT INTO users(role) VALUES ('TEACHER') RETURNING id", Long.class);
        db.jdbc.update("INSERT INTO teacher(id) VALUES (?)", author);
        db.jdbc.update("INSERT INTO learning_resource_teachers(resource_id, teacher_id) VALUES (?, ?)", canonical, author);
        long year = db.jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, 'Resource year', '2026-09-01', '2027-06-30') RETURNING id", Long.class, owner);
        long clazz = db.jdbc.queryForObject("INSERT INTO classes(name, academic_year_id) VALUES ('Resource class', ?) RETURNING id", Long.class, year);
        long course = db.jdbc.queryForObject("INSERT INTO courses(name, code, school_id) VALUES ('Math', 'RESOURCE', ?) RETURNING id", Long.class, owner);
        db.jdbc.update("INSERT INTO learning_resource_classes(resource_id, class_id) VALUES (?, ?)", canonical, clazz);
        db.jdbc.update("INSERT INTO learning_resource_courses(resource_id, course_id) VALUES (?, ?)", canonical, course);
        db.jdbc.update("INSERT INTO resource_comments(content, on_resource_id, commented_by_id) VALUES ('Canonical comment', ?, ?)", canonical, author);
        long obsolete = db.jdbc.queryForObject("INSERT INTO resources(title, url) VALUES ('Obsolete', 'https://example.test/obsolete') RETURNING id", Long.class);
        db.jdbc.update("INSERT INTO resource_allowed_roles(resource_id, role) VALUES (?, 'TEACHER')", obsolete);
        var resourceBefore = db.jdbc.queryForList("SELECT * FROM learning_resources ORDER BY id");
        var teachersBefore = db.jdbc.queryForList("SELECT * FROM learning_resource_teachers ORDER BY resource_id, teacher_id");
        var classesBefore = db.jdbc.queryForList("SELECT * FROM learning_resource_classes ORDER BY resource_id, class_id");
        var coursesBefore = db.jdbc.queryForList("SELECT * FROM learning_resource_courses ORDER BY resource_id, course_id");
        var commentsBefore = db.jdbc.queryForList("SELECT * FROM resource_comments ORDER BY id");

        flyway(db.url, "latest").migrate();

        assertCanonicalSchema(db.jdbc);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resources ORDER BY id")).isEqualTo(resourceBefore);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_teachers ORDER BY resource_id, teacher_id")).isEqualTo(teachersBefore);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_classes ORDER BY resource_id, class_id")).isEqualTo(classesBefore);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_courses ORDER BY resource_id, course_id")).isEqualTo(coursesBefore);
        assertThat(db.jdbc.queryForList("SELECT * FROM resource_comments ORDER BY id")).isEqualTo(commentsBefore);
    }

    @Test
    void unexpectedResourceDependencyFailsWithoutCascadingOrPartiallyDroppingTables() throws Exception {
        Database db = emptyDatabase();
        flyway(db.url, "65").migrate();
        db.jdbc.execute("CREATE TABLE unexpected_resource_dependency (resource_id BIGINT REFERENCES resources(id))");

        assertThatThrownBy(() -> flyway(db.url, "latest").migrate()).isInstanceOf(FlywayException.class);

        assertThat(db.jdbc.queryForObject("SELECT to_regclass('resources')::text", String.class)).isEqualTo("resources");
        assertThat(db.jdbc.queryForObject("SELECT to_regclass('resource_allowed_roles')::text", String.class)).isEqualTo("resource_allowed_roles");
        assertThat(db.jdbc.queryForObject("SELECT to_regclass('unexpected_resource_dependency')::text", String.class)).isEqualTo("unexpected_resource_dependency");
        assertThat(flyway(db.url, "65").info().current().getVersion().getVersion()).isEqualTo("65");
    }

    private void assertCanonicalSchema(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("SELECT to_regclass('resources')::text", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT to_regclass('resource_allowed_roles')::text", String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT to_regclass('learning_resources')::text", String.class)).isEqualTo("learning_resources");
        assertThat(jdbc.queryForObject("SELECT to_regclass('resource_comments')::text", String.class)).isEqualTo("resource_comments");
        assertThat(jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'fk_resource_comments_resource' AND conrelid = 'resource_comments'::regclass", String.class))
                .isEqualTo("FOREIGN KEY (on_resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE");
        assertThat(jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'fk_learning_resources_school' AND conrelid = 'learning_resources'::regclass", String.class))
                .isEqualTo("FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE RESTRICT");
    }

    private Database emptyDatabase() throws Exception {
        String name = "obsolete_resource_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + name;
        return new Database(url, new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword())));
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target(target).load();
    }

    private record Database(String url, JdbcTemplate jdbc) {}
}
