package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class AcademicPeriodSchemaIntegrationTest {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    @Test
    void compatibilitySchoolExistsAndAcademicPeriodsStartEmptyWhileFlywayValidates() {
        flyway.validate();
        assertThat(flyway.getConfiguration().isOutOfOrder()).isFalse();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(jdbc.queryForList("SELECT name FROM schools", String.class)).containsExactly("Legacy School");
        for (String table : new String[]{"academic_years", "terms"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
        }
    }

    @Test
    void schoolDisplayNamesAreNotGloballyUnique() {
        Long first = school();
        Long second = school();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void differentSchoolsCanReuseYearNamesAndEachHaveAnActiveYear() {
        Long first = year(school(), "2026-2027", true);
        Long second = year(school(), "2026-2027", true);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void oneSchoolCanHaveMultipleInactiveYears() {
        Long school = school();
        year(school, "2026-2027", false);
        year(school, "2027-2028", false);
        year(school, "2028-2029", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM academic_years WHERE school_id = ?",
                Long.class, school)).isEqualTo(3);
    }

    @Test
    void yearNamesAreUniqueWithinSchool() {
        Long school = school();
        year(school, "2026-2027", false);
        rejects(() -> year(school, "2026-2027", false), "23505");
    }

    @Test
    void schoolCannotHaveTwoActiveYears() {
        Long school = school();
        year(school, "2026-2027", true);
        rejects(() -> year(school, "2027-2028", true), "23505");
    }

    @Test
    void activatingAnotherYearIsRejected() {
        Long school = school();
        year(school, "2026-2027", true);
        Long inactive = year(school, "2027-2028", false);
        rejects(() -> jdbc.update("UPDATE academic_years SET active = true WHERE id = ?", inactive), "23505");
    }

    @Test
    void differentYearsCanReuseTermNamesAndSequenceNumbers() {
        Long school = school();
        Long first = term(year(school, "2026-2027", false), "Semester 1", 1);
        Long second = term(year(school, "2027-2028", false), "Semester 1", 1);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void termNamesAreUniqueWithinYear() {
        Long year = year(school(), "2026-2027", false);
        term(year, "Semester 1", 1);
        rejects(() -> term(year, "Semester 1", 2), "23505");
    }

    @Test
    void termSequencesAreUniqueWithinYear() {
        Long year = year(school(), "2026-2027", false);
        term(year, "Semester 1", 1);
        rejects(() -> term(year, "Semester 2", 1), "23505");
    }

    @ParameterizedTest
    @MethodSource("missingRequiredFields")
    void databaseRejectsNullRequiredFields(String sql) {
        Long school = school();
        Long year = year(school, "2026-2027", false);
        rejects(() -> jdbc.update(sql.replace("{school}", school.toString())
                .replace("{year}", year.toString())), "23502");
    }

    static Stream<String> missingRequiredFields() {
        return Stream.of(
                "INSERT INTO schools(name) VALUES (NULL)",
                "INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (NULL, '2027-2028', '2027-09-01', '2028-06-30')",
                "INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES ({school}, NULL, '2027-09-01', '2028-06-30')",
                "INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES ({school}, '2027-2028', NULL, '2028-06-30')",
                "INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES ({school}, '2027-2028', '2027-09-01', NULL)",
                "INSERT INTO academic_years(school_id, name, start_date, end_date, active) VALUES ({school}, '2027-2028', '2027-09-01', '2028-06-30', NULL)",
                "INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES (NULL, 'Semester 1', 1, '2026-09-01', '2027-01-31')",
                "INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES ({year}, NULL, 1, '2026-09-01', '2027-01-31')",
                "INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES ({year}, 'Semester 1', NULL, '2026-09-01', '2027-01-31')",
                "INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES ({year}, 'Semester 1', 1, NULL, '2027-01-31')",
                "INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES ({year}, 'Semester 1', 1, '2026-09-01', NULL)");
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void databaseRejectsEqualOrReversedDates(String table, LocalDate start, LocalDate end) {
        Long school = school();
        if (table.equals("academic_years")) {
            rejects(() -> jdbc.update("INSERT INTO academic_years(school_id, name, start_date, end_date) VALUES (?, ?, ?, ?)",
                    school, "2026-2027", start, end), "23514");
        } else {
            Long year = year(school, "2026-2027", false);
            rejects(() -> jdbc.update("INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES (?, ?, ?, ?, ?)",
                    year, "Semester 1", 1, start, end), "23514");
        }
    }

    static Stream<Arguments> invalidDates() {
        return Stream.of("academic_years", "terms").flatMap(table -> Stream.of(
                Arguments.of(table, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)),
                Arguments.of(table, LocalDate.of(2027, 6, 30), LocalDate.of(2026, 9, 1))));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void databaseRejectsNonPositiveTermSequences(int sequence) {
        Long year = year(school(), "2026-2027", false);
        rejects(() -> term(year, "Semester 1", sequence), "23514");
    }

    @Test
    void academicYearCannotReferenceMissingSchool() {
        rejects(() -> year(Long.MAX_VALUE, "2026-2027", false), "23503");
    }

    @Test
    void termCannotReferenceMissingYear() {
        rejects(() -> term(Long.MAX_VALUE, "Semester 1", 1), "23503");
    }

    @Test
    void deletingSchoolWithYearsIsRejected() {
        Long school = school();
        year(school, "2026-2027", false);
        rejects(() -> jdbc.update("DELETE FROM schools WHERE id = ?", school), "23503");
    }

    @Test
    void deletingYearWithTermsIsRejected() {
        Long year = year(school(), "2026-2027", false);
        term(year, "Semester 1", 1);
        rejects(() -> jdbc.update("DELETE FROM academic_years WHERE id = ?", year), "23503");
    }

    private Long school() {
        return jdbc.queryForObject("INSERT INTO schools(name) VALUES ('Test School') RETURNING id", Long.class);
    }

    private Long year(Long school, String name, boolean active) {
        return jdbc.queryForObject("INSERT INTO academic_years(school_id, name, start_date, end_date, active) VALUES (?, ?, '2026-09-01', '2027-06-30', ?) RETURNING id",
                Long.class, school, name, active);
    }

    private Long term(Long year, String name, int sequence) {
        return jdbc.queryForObject("INSERT INTO terms(academic_year_id, name, sequence_number, start_date, end_date) VALUES (?, ?, ?, '2026-09-01', '2027-01-31') RETURNING id",
                Long.class, year, name, sequence);
    }

    private void rejects(Runnable statement, String sqlState) {
        assertThatThrownBy(statement::run).rootCause()
                .isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo(sqlState));
    }
}
