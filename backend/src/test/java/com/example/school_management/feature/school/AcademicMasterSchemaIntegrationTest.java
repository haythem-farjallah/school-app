package com.example.school_management.feature.school;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class AcademicMasterSchemaIntegrationTest {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    @Test
    void cleanMigrationCreatesOneCompatibilitySchoolAndScopesLegacyMasters() {
        flyway.validate();
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(jdbc.queryForList("SELECT name FROM schools", String.class)).containsExactly("Legacy School");
        for (String table : new String[]{"courses", "rooms", "periods"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isPositive();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE school_id IS NULL", Long.class)).isZero();
        }
        assertThat(jdbc.queryForList("SELECT conname FROM pg_constraint WHERE conrelid IN ('courses'::regclass, 'periods'::regclass)", String.class))
                .doesNotContain("courses_code_key", "unique_period_index");
    }

    @Test
    void sameCourseCodeCanExistInDifferentSchools() {
        insert("courses", school());
        insert("courses", school());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE code = 'MATH101'", Long.class)).isEqualTo(2);
    }

    @Test
    void duplicateCourseCodeInSameSchoolIsRejected() {
        Long school = school();
        insert("courses", school);
        rejects(() -> insert("courses", school), "23505");
    }

    @Test
    void samePeriodIndexCanExistInDifferentSchools() {
        insert("periods", school());
        insert("periods", school());
    }

    @Test
    void duplicatePeriodIndexInSameSchoolIsRejected() {
        Long school = school();
        insert("periods", school);
        rejects(() -> insert("periods", school), "23505");
    }

    @Test
    void roomNamesHaveNoNewUniquenessRule() {
        Long school = school();
        insert("rooms", school);
        insert("rooms", school);
        insert("rooms", school());
    }

    @ParameterizedTest
    @ValueSource(strings = {"courses", "rooms", "periods"})
    void schoolIsRequiredByDatabase(String table) {
        rejects(() -> insert(table, null), "23502");
    }

    @ParameterizedTest
    @ValueSource(strings = {"courses", "rooms", "periods"})
    void missingSchoolIsRejected(String table) {
        rejects(() -> insert(table, Long.MAX_VALUE), "23503");
    }

    @ParameterizedTest
    @ValueSource(strings = {"courses", "rooms", "periods"})
    void deletingReferencedSchoolIsRejected(String table) {
        Long school = school();
        insert(table, school);
        rejects(() -> jdbc.update("DELETE FROM schools WHERE id = ?", school), "23503");
    }

    private Long school() {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES ('Test School') RETURNING id", Long.class);
    }

    private void insert(String table, Long school) {
        switch (table) {
            case "courses" -> jdbc.update("INSERT INTO courses(school_id, name, code) VALUES (?, 'Mathematics', 'MATH101')", school);
            case "rooms" -> jdbc.update("INSERT INTO rooms(school_id, name, room_type) VALUES (?, 'Room A', 'CLASSROOM')", school);
            case "periods" -> jdbc.update("INSERT INTO periods(school_id, index_number, start_time, end_time) VALUES (?, 1, '08:00', '08:50')", school);
            default -> throw new IllegalArgumentException(table);
        }
    }

    private void rejects(Runnable statement, String sqlState) {
        assertThatThrownBy(statement::run).rootCause().isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getSQLState()).isEqualTo(sqlState));
    }
}
