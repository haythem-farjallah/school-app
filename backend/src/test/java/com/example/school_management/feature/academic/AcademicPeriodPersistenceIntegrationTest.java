package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Term;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.TermRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class AcademicPeriodPersistenceIntegrationTest {
    @Autowired
    SchoolRepository schools;

    @Autowired
    AcademicYearRepository years;

    @Autowired
    TermRepository terms;

    @Autowired
    EntityManager entityManager;

    @Autowired
    Validator validator;

    @Test
    void schoolsWithSameDisplayNamePersistIndependently() {
        School first = schools.saveAndFlush(school());
        School second = schools.saveAndFlush(school());
        entityManager.clear();

        assertThat(schools.findById(first.getId()).orElseThrow().getName()).isEqualTo("Test School");
        assertThat(schools.findById(second.getId()).orElseThrow().getName()).isEqualTo("Test School");
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void yearLookupsStayWithinSchoolAndFindOnlyItsActiveYear() {
        School first = schools.save(school());
        School second = schools.save(school());
        AcademicYear active = years.save(year(first, "2026-2027", true));
        AcademicYear inactive = years.save(year(first, "2027-2028", false));
        AcademicYear other = years.saveAndFlush(year(second, "2026-2027", true));
        entityManager.clear();

        assertThat(years.findBySchoolId(first.getId())).containsExactlyInAnyOrder(active, inactive);
        assertThat(years.findBySchoolId(second.getId())).containsExactly(other);
        assertThat(years.findBySchoolIdAndActiveTrue(first.getId())).contains(active);
        assertThat(years.findBySchoolIdAndActiveTrue(second.getId())).contains(other);
        assertThat(years.findBySchoolIdAndActiveTrue(schools.save(school()).getId())).isEmpty();
        assertThat(years.findById(active.getId()).orElseThrow().getSchool().getId()).isEqualTo(first.getId());
    }

    @Test
    void termLookupsStayWithinYearAndReturnSequenceOrder() {
        School school = schools.save(school());
        AcademicYear first = years.save(year(school, "2026-2027", false));
        AcademicYear second = years.save(year(school, "2027-2028", false));
        Term later = terms.save(term(first, "Spring", 2));
        Term earlier = terms.save(term(first, "Semester 1", 1));
        Term other = terms.saveAndFlush(term(second, "Semester 1", 1));
        entityManager.clear();

        assertThat(terms.findByAcademicYearIdOrderBySequenceNumberAsc(first.getId())).containsExactly(earlier, later);
        assertThat(terms.findByAcademicYearIdOrderBySequenceNumberAsc(second.getId())).containsExactly(other);
        assertThat(terms.findById(earlier.getId()).orElseThrow().getAcademicYear().getId()).isEqualTo(first.getId());
    }

    @Test
    void auditingSetsCreationAndUpdateTimesAndPreservesCreationOnUpdate() {
        School school = schools.save(school());
        AcademicYear year = years.save(year(school, "2026-2027", false));
        Term term = terms.saveAndFlush(term(year, "Semester 1", 1));
        entityManager.clear();
        school = schools.findById(school.getId()).orElseThrow();
        year = years.findById(year.getId()).orElseThrow();
        term = terms.findById(term.getId()).orElseThrow();
        var schoolCreated = school.getCreatedAt();
        var yearCreated = year.getCreatedAt();
        var termCreated = term.getCreatedAt();
        var schoolUpdated = school.getUpdatedAt();
        var yearUpdated = year.getUpdatedAt();
        var termUpdated = term.getUpdatedAt();
        assertThat(schoolCreated).isNotNull();
        assertThat(yearCreated).isNotNull();
        assertThat(termCreated).isNotNull();

        school.setName("Renamed Test School");
        year.setName("Renamed Year");
        term.setName("Autumn");
        entityManager.flush();
        entityManager.clear();

        School reloadedSchool = schools.findById(school.getId()).orElseThrow();
        AcademicYear reloadedYear = years.findById(year.getId()).orElseThrow();
        Term reloadedTerm = terms.findById(term.getId()).orElseThrow();
        assertThat(reloadedSchool.getCreatedAt()).isEqualTo(schoolCreated);
        assertThat(reloadedYear.getCreatedAt()).isEqualTo(yearCreated);
        assertThat(reloadedTerm.getCreatedAt()).isEqualTo(termCreated);
        assertThat(reloadedSchool.getUpdatedAt()).isAfter(schoolUpdated);
        assertThat(reloadedYear.getUpdatedAt()).isAfter(yearUpdated);
        assertThat(reloadedTerm.getUpdatedAt()).isAfter(termUpdated);
    }

    @ParameterizedTest
    @MethodSource("entityTypes")
    void persistentIdentityIsStableAndWorksWithDetachedUninitializedProxies(Class<?> type) {
        Object entity;
        Object anotherTransient;
        Long id = null;
        if (type == School.class) {
            School school = school();
            entity = school;
            anotherTransient = school();
        } else if (type == AcademicYear.class) {
            School school = schools.save(school());
            entity = year(school, "2026-2027", false);
            anotherTransient = year(school, "2026-2027", false);
        } else {
            AcademicYear year = years.save(year(schools.save(school()), "2026-2027", false));
            entity = term(year, "Semester 1", 1);
            anotherTransient = term(year, "Semester 1", 1);
        }
        assertThat(entity).isEqualTo(entity).isNotEqualTo(anotherTransient).isNotEqualTo(new ClassEntity());
        Set<Object> set = new HashSet<>();
        set.add(entity);
        int hash = entity.hashCode();
        entityManager.persist(entity);
        entityManager.flush();
        if (entity instanceof School school) {
            id = school.getId();
            school.setName("Renamed Test School");
        } else if (entity instanceof AcademicYear year) {
            id = year.getId();
            year.setName("Renamed Year");
            year.setActive(true);
            year.setStartDate(LocalDate.of(2026, 8, 1));
        } else if (entity instanceof Term term) {
            id = term.getId();
            term.setName("Autumn");
            term.setSequenceNumber(2);
            term.setEndDate(LocalDate.of(2027, 2, 1));
        }
        entityManager.flush();
        entityManager.clear();
        Object proxy = entityManager.getReference(type, id);
        entityManager.clear();

        assertThat(Hibernate.isInitialized(proxy)).isFalse();
        assertThat(entity).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(entity);
        assertThat(entity.hashCode()).isEqualTo(hash).isEqualTo(proxy.hashCode());
        assertThat(set).contains(entity, proxy);
        assertThat(Hibernate.isInitialized(proxy)).isFalse();
    }

    static Stream<Class<?>> entityTypes() {
        return Stream.of(School.class, AcademicYear.class, Term.class);
    }

    @ParameterizedTest
    @MethodSource("invalidEntities")
    void beanValidationRejectsInvalidRequiredData(Object entity, String property) {
        assertThat(validator.validate(entity)).anyMatch(violation -> violation.getPropertyPath().toString().equals(property));
    }

    static Stream<Arguments> invalidEntities() {
        School missingName = school();
        missingName.setName(null);
        School blankName = school();
        blankName.setName(" ");
        AcademicYear missingSchool = year(null, "2026-2027", false);
        AcademicYear missingYearName = year(school(), null, false);
        AcademicYear missingYearStart = year(school(), "2026-2027", false);
        missingYearStart.setStartDate(null);
        AcademicYear missingYearEnd = year(school(), "2026-2027", false);
        missingYearEnd.setEndDate(null);
        AcademicYear invalidYearDates = year(school(), "2026-2027", false);
        invalidYearDates.setEndDate(invalidYearDates.getStartDate());
        AcademicYear validYear = year(school(), "2026-2027", false);
        Term missingTermName = term(validYear, null, 1);
        Term missingSequence = term(validYear, "Semester 1", 1);
        missingSequence.setSequenceNumber(null);
        Term invalidSequence = term(validYear, "Semester 1", 0);
        Term missingTermStart = term(validYear, "Semester 1", 1);
        missingTermStart.setStartDate(null);
        Term missingTermEnd = term(validYear, "Semester 1", 1);
        missingTermEnd.setEndDate(null);
        Term invalidTermDates = term(validYear, "Semester 1", 1);
        invalidTermDates.setEndDate(invalidTermDates.getStartDate());
        return Stream.of(
                Arguments.of(missingName, "name"), Arguments.of(blankName, "name"),
                Arguments.of(missingSchool, "school"), Arguments.of(missingYearName, "name"),
                Arguments.of(missingYearStart, "startDate"), Arguments.of(missingYearEnd, "endDate"),
                Arguments.of(invalidYearDates, "dateRangeValid"),
                Arguments.of(term(null, "Semester 1", 1), "academicYear"), Arguments.of(missingTermName, "name"),
                Arguments.of(missingSequence, "sequenceNumber"), Arguments.of(invalidSequence, "sequenceNumber"),
                Arguments.of(missingTermStart, "startDate"), Arguments.of(missingTermEnd, "endDate"),
                Arguments.of(invalidTermDates, "dateRangeValid"));
    }

    private static School school() {
        School school = new School();
        school.setName("Test School");
        return school;
    }

    private static AcademicYear year(School school, String name, boolean active) {
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        year.setName(name);
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 6, 30));
        year.setActive(active);
        return year;
    }

    private static Term term(AcademicYear year, String name, int sequence) {
        Term term = new Term();
        term.setAcademicYear(year);
        term.setName(name);
        term.setSequenceNumber(sequence);
        term.setStartDate(LocalDate.of(2026, 9, 1));
        term.setEndDate(LocalDate.of(2027, 1, 31));
        return term;
    }
}
