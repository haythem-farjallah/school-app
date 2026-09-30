package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.Term;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.TermRepository;
import com.example.school_management.feature.academic.service.AcademicYearService;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.academic.service.TermService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@Transactional
class AcademicCalendarServiceIntegrationTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2027, 6, 30);
    @Autowired AcademicYearService yearService;
    @Autowired TermService termService;
    @Autowired CurrentAcademicYearResolver currentYear;
    @Autowired AcademicYearRepository years;
    @Autowired TermRepository terms;
    @Autowired SchoolRepository schools;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CurrentSchoolResolver currentSchool;
    private School school;
    private School otherSchool;

    @BeforeEach
    void setUp() {
        school = schools.findAll().get(0);
        otherSchool = new School();
        otherSchool.setName("Other school");
        schools.saveAndFlush(otherSchool);
        when(currentSchool.resolve()).thenReturn(school);
    }

    @Test
    void createAndUpdatePreserveCurrentSchoolAndAllowUnchangedName() {
        var created = createYear("Configured", false);
        var updated = yearService.update(created.id(), new UpdateAcademicYearRequest("Configured", START.minusDays(1), END));
        entityManager.flush();
        entityManager.clear();
        assertThat(updated.startDate()).isEqualTo(START.minusDays(1));
        assertThat(years.findById(created.id()).orElseThrow().getSchool().getId()).isEqualTo(school.getId());
        assertThat(yearService.get(created.id())).isEqualTo(updated);
    }

    @Test
    void duplicateYearNamesAreRejectedOnlyInsideCurrentSchool() {
        var first = createYear("Configured", false);
        var second = createYear("Second", false);
        assertThatThrownBy(() -> createYear("Configured", false)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> yearService.update(second.id(), new UpdateAcademicYearRequest("Configured", START, END)))
                .isInstanceOf(ConflictException.class);
        var other = otherYear("Configured", false);
        assertThat(years.findById(other.getId())).isPresent();
        assertThat(yearService.update(first.id(), new UpdateAcademicYearRequest("Configured", START, END)).name()).isEqualTo("Configured");
    }

    @Test
    void readsAndMutationsRejectOtherSchoolsYearAndListIsScopedAndSorted() {
        var earlier = createYear("Earlier", false);
        var later = yearService.create(new CreateAcademicYearRequest("Later", START.plusYears(1), END.plusYears(1), false));
        var other = otherYear("Other", true);
        assertThat(yearService.list()).extracting(AcademicYearDto::id).containsExactly(later.id(), earlier.id());
        assertThatThrownBy(() -> yearService.get(other.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> yearService.update(other.getId(), new UpdateAcademicYearRequest("Changed", START, END)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> yearService.activate(other.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(other.isActive()).isTrue();
    }

    @Test
    void activationSwitchesSafelyIsIdempotentAndDoesNotTouchOtherSchool() {
        var first = createYear("First", true);
        var second = createYear("Second", false);
        var other = otherYear("Other", true);
        assertThat(yearService.activate(second.id()).active()).isTrue();
        assertThat(yearService.activate(second.id()).active()).isTrue();
        entityManager.flush();
        entityManager.clear();
        assertThat(yearService.get(first.id()).active()).isFalse();
        assertThat(yearService.get(second.id()).active()).isTrue();
        assertThat(years.findBySchoolId(school.getId())).filteredOn(AcademicYear::isActive).hasSize(1);
        assertThat(years.findById(other.getId()).orElseThrow().isActive()).isTrue();
        assertThat(currentYear.resolve().getId()).isEqualTo(second.id());
        var third = createYear("Third", true);
        entityManager.flush();
        entityManager.clear();
        assertThat(yearService.get(second.id()).active()).isFalse();
        assertThat(currentYear.resolve().getId()).isEqualTo(third.id());
    }

    @Test
    void resolverFailsWithoutActiveYearEvenWhenOtherSchoolHasOne() {
        createYear("Inactive", false);
        otherYear("Other active", true);
        assertThatThrownBy(currentYear::resolve).isInstanceOf(IllegalStateException.class).hasMessageContaining("no active AcademicYear");
    }

    @ParameterizedTest
    @CsvSource({"2026-09-01,2026-09-01", "2027-06-30,2026-09-01"})
    void rejectsInvalidYearDateRange(LocalDate start, LocalDate end) {
        assertThatThrownBy(() -> yearService.create(new CreateAcademicYearRequest("Bad", start, end, false)))
                .isInstanceOf(ResponseStatusException.class);
        var year = createYear("Valid", false);
        assertThatThrownBy(() -> yearService.update(year.id(), new UpdateAcademicYearRequest("Bad", start, end)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void requiredYearFieldsAreValidatedByService() {
        for (var request : new CreateAcademicYearRequest[]{
                new CreateAcademicYearRequest(" ", START, END, false),
                new CreateAcademicYearRequest(null, START, END, false),
                new CreateAcademicYearRequest("Missing start", null, END, false),
                new CreateAcademicYearRequest("Missing end", START, null, false)}) {
            assertThatThrownBy(() -> yearService.create(request)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void termsAreScopedOrderedAndMayShareNamesAndSequencesAcrossYears() {
        var firstYear = createYear("First", false);
        var secondYear = createYear("Second", false);
        var later = termService.create(firstYear.id(), request("Later", 2, START, END));
        var first = termService.create(firstYear.id(), request("First", 1, START, END));
        var other = termService.create(secondYear.id(), request("First", 1, START, END));
        assertThat(termService.list(firstYear.id())).extracting(TermDto::id).containsExactly(first.id(), later.id());
        assertThat(termService.get(firstYear.id(), first.id())).isEqualTo(first);
        assertThatThrownBy(() -> termService.get(firstYear.id(), other.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> termService.update(firstYear.id(), other.id(), request("Changed", 3, START, END)))
                .isInstanceOf(ResourceNotFoundException.class);
        var updated = termService.update(firstYear.id(), first.id(), request("First", 1, START, END.minusDays(1)));
        entityManager.flush();
        entityManager.clear();
        assertThat(updated.academicYearId()).isEqualTo(firstYear.id());
        assertThat(terms.findById(first.id()).orElseThrow().getAcademicYear().getId()).isEqualTo(firstYear.id());
    }

    @Test
    void termDuplicatesAreRejectedOnCreateAndUpdateButOwnValuesAreAllowed() {
        var year = createYear("Year", false);
        var first = termService.create(year.id(), request("First", 1, START, END));
        var second = termService.create(year.id(), request("Second", 2, START, END));
        assertThatThrownBy(() -> termService.create(year.id(), request("First", 3, START, END))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> termService.create(year.id(), request("Third", 1, START, END))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> termService.update(year.id(), second.id(), request("First", 2, START, END))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> termService.update(year.id(), second.id(), request("Second", 1, START, END))).isInstanceOf(ConflictException.class);
        assertThat(termService.update(year.id(), first.id(), request("First", 1, START, END))).isEqualTo(first);
    }

    @ParameterizedTest
    @CsvSource({"0,2026-09-01,2027-06-30", "-1,2026-09-01,2027-06-30",
            "1,2026-09-01,2026-09-01", "1,2027-06-30,2026-09-01",
            "1,2026-08-31,2027-06-30", "1,2026-09-01,2027-07-01"})
    void invalidTermsAreRejectedOnCreateAndUpdate(int sequence, LocalDate start, LocalDate end) {
        var year = createYear("Year", false);
        var valid = termService.create(year.id(), request("Valid", 1, START, END));
        assertThatThrownBy(() -> termService.create(year.id(), request("Bad", sequence, start, end))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> termService.update(year.id(), valid.id(), request("Bad", sequence, start, end))).isInstanceOf(ResponseStatusException.class);
        assertThat(termService.get(year.id(), valid.id())).isEqualTo(valid);
    }

    @Test
    void requiredTermFieldsAreValidated() {
        var year = createYear("Year", false);
        for (var request : new TermRequest[]{request(" ", 1, START, END), request(null, 1, START, END),
                request("Missing sequence", null, START, END), request("Missing start", 1, null, END), request("Missing end", 1, START, null)}) {
            assertThatThrownBy(() -> termService.create(year.id(), request)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void allTermOperationsRejectOtherSchoolsYear() {
        var year = otherYear("Other", false);
        var term = new Term();
        term.setAcademicYear(year);
        term.setName("Foreign term");
        term.setSequenceNumber(1);
        term.setStartDate(START);
        term.setEndDate(END);
        terms.saveAndFlush(term);
        assertThatThrownBy(() -> termService.create(year.getId(), request("Term", 1, START, END))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> termService.list(year.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> termService.get(year.getId(), term.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> termService.update(year.getId(), term.getId(), request("Term", 1, START, END))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void yearUpdateCannotExcludeExistingTermsButCanExpandCalendar() {
        var year = createYear("Year", false);
        termService.create(year.id(), request("Term", 1, START, END));
        assertThatThrownBy(() -> yearService.update(year.id(), new UpdateAcademicYearRequest("Year", START.plusDays(1), END)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> yearService.update(year.id(), new UpdateAcademicYearRequest("Year", START, END.minusDays(1))))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(yearService.update(year.id(), new UpdateAcademicYearRequest("Year", START.minusDays(1), END.plusDays(1))).endDate())
                .isEqualTo(END.plusDays(1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void activationRollsBackAtomicallyWhenTransactionFails() {
        var tx = new TransactionTemplate(transactionManager);
        var first = createYear("Rollback first", true);
        var second = createYear("Rollback second", false);
        try {
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                yearService.activate(second.id());
                throw new IllegalStateException("Abort test transaction");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(yearService.get(first.id()).active()).isTrue();
            assertThat(yearService.get(second.id()).active()).isFalse();
            assertThat(currentYear.resolve().getId()).isEqualTo(first.id());
        } finally {
            tx.executeWithoutResult(status -> {
                years.deleteById(first.id());
                years.deleteById(second.id());
                schools.deleteById(otherSchool.getId());
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void activationPreservesConcurrentCalendarEditsAndTermContainment(boolean editTarget) throws Exception {
        var first = createYear("Concurrent first", true);
        var second = createYear("Concurrent second", false);
        long editedId = editTarget ? second.id() : first.id();
        var edited = new CountDownLatch(1);
        var commitEdit = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        var tx = new TransactionTemplate(transactionManager);
        try {
            var editing = workers.submit(() -> tx.executeWithoutResult(status -> {
                yearService.update(editedId, new UpdateAcademicYearRequest("Expanded", START.minusDays(1), END));
                termService.create(editedId, request("Boundary", 1, START.minusDays(1), END));
                edited.countDown();
                try {
                    if (!commitEdit.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to commit calendar edit");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }));
            assertThat(edited.await(10, TimeUnit.SECONDS)).isTrue();
            var activating = workers.submit(() -> yearService.activate(second.id()));
            // Observe the actual database lock wait before allowing the calendar edit to commit.
            await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock'",
                    Long.class) > 0);
            commitEdit.countDown();
            editing.get(10, TimeUnit.SECONDS);
            assertThat(activating.get(10, TimeUnit.SECONDS).active()).isTrue();
            var reloaded = yearService.get(editedId);
            assertThat(reloaded.name()).isEqualTo("Expanded");
            assertThat(reloaded.startDate()).isEqualTo(START.minusDays(1));
            assertThat(termService.list(editedId)).singleElement().satisfies(term ->
                    assertThat(term.startDate()).isEqualTo(START.minusDays(1)));
            assertThat(yearService.get(first.id()).active()).isFalse();
            assertThat(yearService.get(second.id()).active()).isTrue();
        } finally {
            commitEdit.countDown();
            workers.shutdown();
            assertThat(workers.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
            tx.executeWithoutResult(status -> {
                terms.deleteAll(terms.findByAcademicYearIdOrderBySequenceNumberAsc(editedId));
                terms.flush();
                years.deleteById(first.id());
                years.deleteById(second.id());
                schools.deleteById(otherSchool.getId());
            });
        }
    }

    private AcademicYearDto createYear(String name, boolean active) {
        return yearService.create(new CreateAcademicYearRequest(name, START, END, active));
    }

    private AcademicYear otherYear(String name, boolean active) {
        AcademicYear year = new AcademicYear();
        year.setSchool(otherSchool);
        year.setName(name);
        year.setStartDate(START);
        year.setEndDate(END);
        year.setActive(active);
        return years.saveAndFlush(year);
    }

    private TermRequest request(String name, Integer sequence, LocalDate start, LocalDate end) {
        return new TermRequest(name, sequence, start, end);
    }
}
