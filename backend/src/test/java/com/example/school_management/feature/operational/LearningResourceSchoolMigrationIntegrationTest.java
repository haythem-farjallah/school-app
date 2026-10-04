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
class LearningResourceSchoolMigrationIntegrationTest {
    @Autowired PostgreSQLContainer<?> postgres;

    @Test
    void classDerivesOwnerThroughAcademicYear() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void courseDerivesOwner() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetCourse(db.jdbc, resource, course(db.jdbc, owner));
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void agreeingClassAndCourseDeriveOwner() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        targetCourse(db.jdbc, resource, course(db.jdbc, owner));
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void multipleClassesInSameSchoolDeriveOwner() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void resourcesInDifferentSchoolsDeriveOwnershipIndependently() throws Exception {
        Database db = databaseAt63();
        long schoolA = school(db.jdbc);
        long schoolB = school(db.jdbc);
        long resourceA = resource(db.jdbc);
        long resourceB = resource(db.jdbc);
        targetClass(db.jdbc, resourceA, clazz(db.jdbc, schoolA));
        membership(db.jdbc, creator(db.jdbc, resourceB), schoolB, "TEACHER");
        migrateAndAssertOwner(db, resourceA, schoolA);
        assertThat(db.jdbc.queryForObject("SELECT school_id FROM learning_resources WHERE id = ?", Long.class, resourceB)).isEqualTo(schoolB);
    }

    @Test
    void conflictingClassAndCourseFailWithoutRewritingTargets() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, school(db.jdbc)));
        targetCourse(db.jdbc, resource, course(db.jdbc, school(db.jdbc)));
        assertMigrationFails(db, resource);
    }

    @Test
    void classesInDifferentSchoolsFail() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, school(db.jdbc)));
        targetClass(db.jdbc, resource, clazz(db.jdbc, school(db.jdbc)));
        assertMigrationFails(db, resource);
    }

    @Test
    void coursesInDifferentSchoolsFail() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        targetCourse(db.jdbc, resource, course(db.jdbc, school(db.jdbc)));
        targetCourse(db.jdbc, resource, course(db.jdbc, school(db.jdbc)));
        assertMigrationFails(db, resource);
    }

    @Test
    void structuralOwnerAcceptsCreatorWithOwnerTeacherMembership() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        long creator = creator(db.jdbc, resource);
        membership(db.jdbc, creator, owner, "TEACHER");
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void structuralOwnerAcceptsMultiSchoolCreatorWithOwnerTeacherMembership() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        long creator = creator(db.jdbc, resource);
        membership(db.jdbc, creator, owner, "TEACHER");
        membership(db.jdbc, creator, school(db.jdbc), "TEACHER");
        migrateAndAssertOwner(db, resource, owner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void structuralOwnerRejectsCreatorHistoryWithoutOwnerTeacherMembership(String role) throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        long creator = creator(db.jdbc, resource);
        membership(db.jdbc, creator, role.equals("TEACHER") ? school(db.jdbc) : owner, role);
        assertMigrationFails(db, resource);
    }

    @Test
    void structuralOwnerAcceptsLegacyCreatorWithoutMembershipHistory() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        targetClass(db.jdbc, resource, clazz(db.jdbc, owner));
        creator(db.jdbc, resource);
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void targetlessResourceUsesUniqueCreatorTeacherMembershipSchool() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        long creator = creator(db.jdbc, resource);
        membership(db.jdbc, creator, owner, "TEACHER");
        membership(db.jdbc, creator, school(db.jdbc), "ADMIN");
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void targetlessResourceRejectsMultiSchoolCreator() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        long creator = creator(db.jdbc, resource);
        membership(db.jdbc, creator, school(db.jdbc), "TEACHER");
        membership(db.jdbc, creator, school(db.jdbc), "TEACHER");
        assertMigrationFails(db, resource);
    }

    @Test
    void targetlessResourceRejectsCreatorsProducingDifferentSchools() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        membership(db.jdbc, creator(db.jdbc, resource), school(db.jdbc), "TEACHER");
        membership(db.jdbc, creator(db.jdbc, resource), school(db.jdbc), "TEACHER");
        assertMigrationFails(db, resource);
    }

    @Test
    void targetlessResourceAcceptsMultipleCreatorsProducingOneSchool() throws Exception {
        Database db = databaseAt63();
        long owner = school(db.jdbc);
        long resource = resource(db.jdbc);
        membership(db.jdbc, creator(db.jdbc, resource), owner, "TEACHER");
        membership(db.jdbc, creator(db.jdbc, resource), owner, "TEACHER");
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void noEvidenceUsesExactlyOneSchoolFallback() throws Exception {
        Database db = databaseAt63();
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long resource = resource(db.jdbc);
        creator(db.jdbc, resource);
        migrateAndAssertOwner(db, resource, owner);
    }

    @Test
    void noEvidenceWithMultipleSchoolsFails() throws Exception {
        Database db = databaseAt63();
        school(db.jdbc);
        assertMigrationFails(db, resource(db.jdbc));
    }

    @Test
    void noEvidenceWithZeroSchoolsFails() throws Exception {
        Database db = databaseAt63();
        db.jdbc.update("TRUNCATE schools CASCADE");
        assertMigrationFails(db, resource(db.jdbc));
    }

    @Test
    void finalSchemaEnforcesSchoolForeignKeyAndIndex() throws Exception {
        Database db = databaseAt63();
        long owner = db.jdbc.queryForObject("SELECT id FROM schools", Long.class);
        long resource = resource(db.jdbc);
        migrateAndAssertOwner(db, resource, owner);
        Flyway flyway = flyway(db.url, "64");
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("64");
        assertThat(db.jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'learning_resources' AND column_name = 'school_id'", String.class)).isEqualTo("NO");
        assertThat(db.jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'learning_resources'::regclass AND contype = 'f'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("FOREIGN KEY (school_id) REFERENCES schools(id)").contains("ON DELETE RESTRICT"));
        assertThat(db.jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename = 'learning_resources'", String.class))
                .anySatisfy(definition -> assertThat(definition).contains("idx_learning_resources_school").contains("(school_id)"));
        assertThatThrownBy(() -> insertOwnedResource(db.jdbc, null)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> insertOwnedResource(db.jdbc, -1L)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
        assertThatThrownBy(() -> db.jdbc.update("DELETE FROM schools WHERE id = ?", owner)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
    }

    @Test
    void matchingCommentsRetainRowsAndUseCanonicalLearningResourceForeignKey() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        legacyResource(db.jdbc, resource);
        long commenter = commenter(db.jdbc);
        long comment = comment(db.jdbc, resource, commenter);
        var commentsBefore = db.jdbc.queryForList("SELECT * FROM resource_comments ORDER BY id");
        var legacyBefore = db.jdbc.queryForList("SELECT * FROM resources ORDER BY id");

        flyway(db.url, "64").migrate();

        assertThat(db.jdbc.queryForList("SELECT * FROM resource_comments ORDER BY id")).isEqualTo(commentsBefore);
        assertThat(db.jdbc.queryForList("SELECT * FROM resources ORDER BY id")).isEqualTo(legacyBefore);
        assertThat(db.jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'fk_resource_comments_resource' AND conrelid = 'resource_comments'::regclass", String.class))
                .isEqualTo("FOREIGN KEY (on_resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE");
        assertThat(db.jdbc.queryForObject("SELECT convalidated FROM pg_constraint WHERE conname = 'fk_resource_comments_resource' AND conrelid = 'resource_comments'::regclass", Boolean.class)).isTrue();
        assertThat(db.jdbc.queryForObject("SELECT on_resource_id FROM resource_comments WHERE id = ?", Long.class, comment)).isEqualTo(resource);

        long owner = db.jdbc.queryForObject("SELECT school_id FROM learning_resources WHERE id = ?", Long.class, resource);
        long canonicalOnly = db.jdbc.queryForObject("INSERT INTO learning_resources(title, url, type, school_id) VALUES ('Canonical only', 'https://example.test/canonical', 'LINK', ?) RETURNING id", Long.class, owner);
        assertThat(comment(db.jdbc, canonicalOnly, commenter)).isPositive();
        legacyResource(db.jdbc, -10);
        assertThatThrownBy(() -> comment(db.jdbc, -10, commenter)).rootCause().isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
    }

    @Test
    void legacyOnlyFixtureCommentIsDeletedWhileCanonicalCommentIsPreserved() throws Exception {
        Database db = databaseAt63();
        long canonical = resource(db.jdbc);
        legacyResource(db.jdbc, canonical);
        long author = commenter(db.jdbc);
        long validComment = comment(db.jdbc, canonical, author);
        var validBefore = db.jdbc.queryForMap("SELECT * FROM resource_comments WHERE id = ?", validComment);
        legacyResource(db.jdbc, -10);
        long obsoleteComment = comment(db.jdbc, -10, author);
        var legacyBefore = db.jdbc.queryForList("SELECT * FROM resources ORDER BY id");

        flyway(db.url, "64").migrate();

        assertThat(db.jdbc.queryForMap("SELECT * FROM resource_comments WHERE id = ?", validComment)).isEqualTo(validBefore);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM resource_comments WHERE id = ?", Long.class, obsoleteComment)).isZero();
        assertThat(db.jdbc.queryForList("SELECT * FROM resources ORDER BY id")).isEqualTo(legacyBefore);
        assertThatThrownBy(() -> comment(db.jdbc, -10, author)).rootCause()
                .isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
    }

    @Test
    void fixtureCommentMissingFromBothResourceTablesIsDeleted() throws Exception {
        Database db = databaseAt63();
        resource(db.jdbc);
        db.jdbc.update("ALTER TABLE resource_comments DROP CONSTRAINT fk_resource_comments_resource");
        long comment = comment(db.jdbc, -10, commenter(db.jdbc));
        // A historical NOT VALID constraint models existing damage while still
        // enforcing the old relationship for any new inserts.
        db.jdbc.update("ALTER TABLE resource_comments ADD CONSTRAINT fk_resource_comments_resource FOREIGN KEY (on_resource_id) REFERENCES resources(id) NOT VALID");
        flyway(db.url, "64").migrate();

        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM resource_comments WHERE id = ?", Long.class, comment)).isZero();
        assertThat(db.jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'fk_resource_comments_resource' AND conrelid = 'resource_comments'::regclass", String.class))
                .isEqualTo("FOREIGN KEY (on_resource_id) REFERENCES learning_resources(id) ON DELETE CASCADE");
        assertThat(db.jdbc.queryForObject("SELECT convalidated FROM pg_constraint WHERE conname = 'fk_resource_comments_resource' AND conrelid = 'resource_comments'::regclass", Boolean.class)).isTrue();
    }

    @Test
    void deletingCanonicalResourceCascadesCommentsAndRetainsLegacyResources() throws Exception {
        Database db = databaseAt63();
        long resource = resource(db.jdbc);
        legacyResource(db.jdbc, resource);
        long comment = comment(db.jdbc, resource, commenter(db.jdbc));
        flyway(db.url, "64").migrate();

        db.jdbc.update("DELETE FROM learning_resources WHERE id = ?", resource);

        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM resource_comments WHERE id = ?", Long.class, comment)).isZero();
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM resources WHERE id = ?", Long.class, resource)).isEqualTo(1);
    }

    private long commenter(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO users(role) VALUES ('STUDENT') RETURNING id", Long.class);
    }

    private void legacyResource(JdbcTemplate jdbc, long id) {
        jdbc.update("INSERT INTO resources(id, title, url) VALUES (?, 'Legacy resource', 'https://example.test/legacy')", id);
    }

    private long comment(JdbcTemplate jdbc, long resource, long commenter) {
        return jdbc.queryForObject("INSERT INTO resource_comments(content, on_resource_id, commented_by_id) VALUES ('Comment', ?, ?) RETURNING id", Long.class, resource, commenter);
    }

    private void insertOwnedResource(JdbcTemplate jdbc, Long school) {
        jdbc.update("INSERT INTO learning_resources(title, url, type, school_id) VALUES ('Resource', 'https://example.test/resource', 'DOCUMENT', ?)", school);
    }

    private void migrateAndAssertOwner(Database db, long resource, long school) {
        var before = db.jdbc.queryForMap("SELECT * FROM learning_resources WHERE id = ?", resource);
        var classes = db.jdbc.queryForList("SELECT * FROM learning_resource_classes ORDER BY resource_id, class_id");
        var courses = db.jdbc.queryForList("SELECT * FROM learning_resource_courses ORDER BY resource_id, course_id");
        var teachers = db.jdbc.queryForList("SELECT * FROM learning_resource_teachers ORDER BY resource_id, teacher_id");
        var memberships = db.jdbc.queryForList("SELECT * FROM school_memberships ORDER BY id");
        var roles = db.jdbc.queryForList("SELECT * FROM school_membership_roles ORDER BY membership_id, role");
        flyway(db.url, "64").migrate();
        var after = db.jdbc.queryForMap("SELECT * FROM learning_resources WHERE id = ?", resource);
        assertThat(after.remove("school_id")).isEqualTo(school);
        assertThat(after).isEqualTo(before);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_classes ORDER BY resource_id, class_id")).isEqualTo(classes);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_courses ORDER BY resource_id, course_id")).isEqualTo(courses);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_teachers ORDER BY resource_id, teacher_id")).isEqualTo(teachers);
        assertThat(db.jdbc.queryForList("SELECT * FROM school_memberships ORDER BY id")).isEqualTo(memberships);
        assertThat(db.jdbc.queryForList("SELECT * FROM school_membership_roles ORDER BY membership_id, role")).isEqualTo(roles);
    }

    private void assertMigrationFails(Database db, long resource) {
        var before = db.jdbc.queryForList("SELECT * FROM learning_resources ORDER BY id");
        var classes = db.jdbc.queryForList("SELECT * FROM learning_resource_classes ORDER BY resource_id, class_id");
        var courses = db.jdbc.queryForList("SELECT * FROM learning_resource_courses ORDER BY resource_id, course_id");
        var teachers = db.jdbc.queryForList("SELECT * FROM learning_resource_teachers ORDER BY resource_id, teacher_id");
        var memberships = db.jdbc.queryForList("SELECT * FROM school_memberships ORDER BY id");
        var roles = db.jdbc.queryForList("SELECT * FROM school_membership_roles ORDER BY membership_id, role");
        assertThatThrownBy(() -> flyway(db.url, "64").migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("LearningResource School ownership").hasMessageContaining("resource " + resource);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resources ORDER BY id")).isEqualTo(before);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_classes ORDER BY resource_id, class_id")).isEqualTo(classes);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_courses ORDER BY resource_id, course_id")).isEqualTo(courses);
        assertThat(db.jdbc.queryForList("SELECT * FROM learning_resource_teachers ORDER BY resource_id, teacher_id")).isEqualTo(teachers);
        assertThat(db.jdbc.queryForList("SELECT * FROM school_memberships ORDER BY id")).isEqualTo(memberships);
        assertThat(db.jdbc.queryForList("SELECT * FROM school_membership_roles ORDER BY membership_id, role")).isEqualTo(roles);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_name = 'learning_resources' AND column_name = 'school_id'", Long.class)).isZero();
        assertThat(flyway(db.url, "63").info().current().getVersion().getVersion()).isEqualTo("63");
    }

    private Database databaseAt63() throws Exception {
        String name = "learning_resource_school_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        String url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + name;
        flyway(url, "63").migrate();
        return new Database(url, new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword())));
    }

    private Flyway flyway(String url, String target) {
        return Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword()).target(target).load();
    }

    private long school(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES (?) RETURNING id", Long.class, "School " + UUID.randomUUID());
    }

    private long creator(JdbcTemplate jdbc, long resource) {
        long teacher = jdbc.queryForObject("INSERT INTO users(role) VALUES ('TEACHER') RETURNING id", Long.class);
        jdbc.update("INSERT INTO teacher(id) VALUES (?)", teacher);
        jdbc.update("INSERT INTO learning_resource_teachers(resource_id, teacher_id) VALUES (?, ?)", resource, teacher);
        return teacher;
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

    private long resource(JdbcTemplate jdbc) {
        return jdbc.queryForObject("INSERT INTO learning_resources(title, url, type) VALUES ('Resource', 'https://example.test/resource', 'DOCUMENT') RETURNING id", Long.class);
    }

    private void targetClass(JdbcTemplate jdbc, long resource, long clazz) {
        jdbc.update("INSERT INTO learning_resource_classes(resource_id, class_id) VALUES (?, ?)", resource, clazz);
    }

    private void targetCourse(JdbcTemplate jdbc, long resource, long course) {
        jdbc.update("INSERT INTO learning_resource_courses(resource_id, course_id) VALUES (?, ?)", resource, course);
    }

    private record Database(String url, JdbcTemplate jdbc) {}
}
